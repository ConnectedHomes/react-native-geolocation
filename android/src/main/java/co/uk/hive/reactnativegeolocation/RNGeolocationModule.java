
package co.uk.hive.reactnativegeolocation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Toast;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceServiceLocator;
import co.uk.hive.reactnativegeolocation.location.LatLng;
import co.uk.hive.reactnativegeolocation.location.LocationController;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.List;

public class RNGeolocationModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext reactContext;
    private final GeofenceController mGeofenceController;
    private final LocationController mLocationController;
    private final RNMapper mRnMapper;

    private final static int REQUEST_CHECK_SETTINGS = 10008;

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
        ReactNativeHost reactNativeHost = ((ReactApplication) this.reactContext.getApplicationContext()).getReactNativeHost();
        ReactInstanceManager reactInstanceManager = reactNativeHost.getReactInstanceManager();
        //noinspection ConstantConditions
        reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("geofence", mRnMapper.fromBundle(new Bundle()));
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
        Function<LatLng, Object> positionCallback = location -> {
            successCallback.invoke(mRnMapper.writeLocation(location));
            return null;
        };
        mLocationController.getCurrentPosition(
                mRnMapper.readPositionRequest(currentPositionRequest), positionCallback, convertCallback(failureCallback));

        // TODO: Handle RESOLUTION_REQUIRED error status here and try startActivityForResult to see wagwaaan!!
        // TODO: Handle no resolution avialable as well!!! WHHYYYYY GOOOOOGLE!!!!!
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
                final ApiException apiException = (ApiException) t;

                switch (apiException.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        // TODO: Callback to module somehow... broadcast? Start activity for result from there and use activitylistener!
                        try {
                            // Cast to a resolvable exception.
                            final ResolvableApiException resolvable = (ResolvableApiException) apiException;
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                    getCurrentActivity(),
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        } catch (ClassCastException e) {
                            // Ignore, should be an impossible error.
                        }
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                        // TODO: Test cancelling or check which devices/os don't have the accuracy thingy
                        Toast.makeText(getReactApplicationContext(), "SETTINGS_CHANGE_UNAVAILABLE!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default: {
                        // TODO: The result of the above cases needed passing back to app
                        final WritableMap errorMap = Arguments.createMap();
                        errorMap.putString("exceptionType", t.getClass().toString());
                        errorMap.putString("message", apiException.getMessage());
                        errorMap.putString("code", String.valueOf(apiException.getStatusCode()));
                        errorMap.putString("JIRA", "MA1-1200");
                        callback.invoke(errorMap);
                    }
                }
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

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        // TODO: Consider a permission check here first!!
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // All required changes were successfully made.. TODO: How to proceed?
                    Toast.makeText(getReactApplicationContext(), "User enabled additional setting!", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    // The user was asked to change settings, but chose not to TODO: How to handle cancellation?
                    Toast.makeText(getReactApplicationContext(), "User cancelled location enabled!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // NOOP
    }
}
