
package co.uk.hive.reactnativegeolocation;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.List;
import java.util.Objects;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceLog;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceServiceLocator;
import co.uk.hive.reactnativegeolocation.location.LatLng;
import co.uk.hive.reactnativegeolocation.location.LocationController;
import co.uk.hive.reactnativegeolocation.location.LocationError;

public class RNGeolocationModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final static int CHECK_SETTINGS_REQUEST_CODE = 104;

    private final ReactApplicationContext reactContext;
    private final GeofenceController mGeofenceController;
    private final LocationController mLocationController;
    private final RNMapper mRnMapper;

    private ReadableMap currentPositionRequest = null;
    private Callback positionSuccessCallback = null;
    private Callback positionFailureCallback = null;

    public RNGeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
        mGeofenceController = GeofenceServiceLocator.getGeofenceController(reactContext.getApplicationContext());
        mLocationController = GeofenceServiceLocator.getLocationController(reactContext.getApplicationContext());
        mRnMapper = new RNMapper();
    }

    @Override
    public String getName() {
        return "RNGeolocation";
    }

    @ReactMethod
    public void ready() {
        mGeofenceController.setupReregistration();
    }

    @ReactMethod
    public void startGeofences(Callback successCallback, Callback failureCallback) {
        mGeofenceController.start(convertCallback(successCallback), convertCallback(failureCallback));
    }

    @ReactMethod
    public void stopGeofences(Callback successCallback, Callback failureCallback) {
        mGeofenceController.stop(convertCallback(successCallback), convertCallback(failureCallback));
    }

    @ReactMethod
    public void addGeofences(ReadableArray geofencesArray) {
        List<Geofence> geofences = Stream.range(0, geofencesArray.size())
                .map(geofencesArray::getMap)
                .map(mRnMapper::readGeofence)
                .toList();
        mGeofenceController.addGeofences(geofences);
    }

    @ReactMethod
    public void removeGeofences() {
        mGeofenceController.removeAllGeofences();
    }

    @ReactMethod
    public void getCurrentPosition(ReadableMap currentPositionRequest,
                                   Callback successCallback, Callback failureCallback) {

        // Keep references for a retry
        this.currentPositionRequest = currentPositionRequest;
        this.positionSuccessCallback = successCallback;
        this.positionFailureCallback = failureCallback;

        Function<LatLng, Object> positionCallback = location -> {
            successCallback.invoke(mRnMapper.writeLocation(location));
            return null;
        };
        mLocationController.getCurrentPosition(
                mRnMapper.readPositionRequest(currentPositionRequest), positionCallback, convertCallback(failureCallback));
    }

    @ReactMethod
    public void isLocationEnabled(Promise promise) {
        final boolean locationEnabled = new LocationServicesChecker(getReactApplicationContext())
                .isLocationEnabled();

        promise.resolve(locationEnabled);
    }

    private <T> Function<T, Object> convertCallback(Callback callback) {
        return t -> {
            if (t instanceof ApiException) {
                handleApiException((ApiException) t, callback);
            } else if (t instanceof Exception) {
                final Exception exception = (Exception) t;
                final WritableMap errorMap = Arguments.createMap();
                errorMap.putString("exceptionType", t.getClass().toString());
                errorMap.putString("message", exception.getMessage());
                errorMap.putString("JIRA", "MA1-1200");
                callback.invoke(errorMap);
            } else {
                callback.invoke(t);
            }
            return null;
        };
    }

    private void handleApiException(ApiException apiException, Callback failureCallback) {
        switch (apiException.getStatusCode()) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                try {
                    // Cast to a resolvable exception.
                    final ResolvableApiException resolvable = (ResolvableApiException) apiException;
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    resolvable.startResolutionForResult(Objects.requireNonNull(getCurrentActivity()), CHECK_SETTINGS_REQUEST_CODE);
                    GeofenceLog.d("Resolution required!");
                } catch (IntentSender.SendIntentException | ClassCastException e) {
                    GeofenceLog.e(e.getMessage());
                    failureCallback.invoke(LocationError.LOCATION_SETTINGS_FAILED);
                }
                return;
            }
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                GeofenceLog.e("Settings change unavailable");
                failureCallback.invoke(LocationError.LOCATION_SETTINGS_FAILED);
                return;
            }
        }

        // Default case if status code not handled
        final WritableMap errorMap = Arguments.createMap();
        errorMap.putString("exceptionType", apiException.getClass().toString());
        errorMap.putString("message", apiException.getMessage());
        errorMap.putString("code", String.valueOf(apiException.getStatusCode()));
        errorMap.putString("JIRA", "MA1-1200");
        failureCallback.invoke(errorMap);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == CHECK_SETTINGS_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    GeofenceLog.d("Improved location accuracy enabled!");
                    final boolean hasRequestParams = this.currentPositionRequest != null
                            && this.positionSuccessCallback != null
                            && this.positionFailureCallback != null;
                    if (hasRequestParams) {
                        // All required changes were successfully made.. retry current position request
                        this.getCurrentPosition(this.currentPositionRequest, this.positionSuccessCallback, this.positionFailureCallback);
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    // The user was asked to change settings, but chose not to
                    GeofenceLog.d("Improved location accuracy not enabled!");
                    if (this.positionFailureCallback != null) {
                        this.positionFailureCallback.invoke(LocationError.LOCATION_SETTINGS_FAILED);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}
