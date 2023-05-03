
package co.uk.hive.reactnativegeolocation;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
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
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.List;

import co.uk.hive.reactnativegeolocation.data.RNMapper;
import co.uk.hive.reactnativegeolocation.geofence.legacy.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceServiceLocator;
import co.uk.hive.reactnativegeolocation.location.LatLng;
import co.uk.hive.reactnativegeolocation.location.LocationController;
import co.uk.hive.reactnativegeolocation.util.LocationServicesChecker;

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
        //mGeofenceController.setupReregistration();
    }

    @ReactMethod
    public void startGeofences(Callback successCallback, Callback failureCallback) {
        Log.d("Geofence", "RNGeolocationModule::startGeofences!");
        mGeofenceController.start(convertCallback(successCallback), convertCallback(failureCallback));
        ReactNativeHost reactNativeHost = ((ReactApplication) this.reactContext.getApplicationContext()).getReactNativeHost();
        ReactInstanceManager reactInstanceManager = reactNativeHost.getReactInstanceManager();
        //noinspection ConstantConditions
//        reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
//                .emit("geofence", mRnMapper.fromBundle(new Bundle()));
    }

    @ReactMethod
    public void stopGeofences(Callback successCallback, Callback failureCallback) {
        Log.d("Geofence", "RNGeolocationModule::stopGeofences!");
        mGeofenceController.stop(convertCallback(successCallback), convertCallback(failureCallback));
    }

    @ReactMethod
    public void addGeofences(ReadableArray geofencesArray) {
        Log.d("Geofence", "RNGeolocationModule::addGeofences: " + geofencesArray.toString());
        List<Geofence> geofences = Stream.range(0, geofencesArray.size())
                .map(geofencesArray::getMap)
                .map(mRnMapper::readGeofence)
                .toList();
        mGeofenceController.addGeofences(geofences);
    }

    @ReactMethod
    public void removeGeofences() {
        Log.d("Geofence", "RNGeolocationModule::removeGeofences");
        mGeofenceController.removeAllGeofences();
    }

    @ReactMethod
    public void getCurrentPosition(ReadableMap currentPositionRequest,
                                   Callback successCallback, Callback failureCallback) {
        Function<LatLng, Object> positionCallback = location -> {
            successCallback.invoke(mRnMapper.writeLocation(location));
            return null;
        };
        Log.d("Geofence", "RNGeolocationModule::getCurrentPosition: " + currentPositionRequest.toString());
        mLocationController.getCurrentPosition(
                mRnMapper.readPositionRequest(currentPositionRequest), positionCallback, convertCallback(failureCallback));

        // TODO: Handle RESOLUTION_REQUIRED error status here and try startActivityForResult to see wagwaaan!!
        // TODO: Handle no resolution avialable as well!!! WHHYYYYY GOOOOOGLE!!!!!
    }

    @ReactMethod
    public void isLocationEnabled(Promise promise) {
        final boolean locationEnabled = new LocationServicesChecker(getReactApplicationContext())
                .isLocationEnabled();
        Log.d("Geofence", "RNGeolocationModule::locationEnabled: " + locationEnabled);
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
                        Log.e("Geofence", "RNGeolocationModule::RESOLUTION_REQUIRED");
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
                        Log.e("Geofence", "RNGeolocationModule::SETTINGS_CHANGE_UNAVAILABLE");
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
