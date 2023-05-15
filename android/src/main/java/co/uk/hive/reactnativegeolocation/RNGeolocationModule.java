
package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceServiceLocator;
import co.uk.hive.reactnativegeolocation.location.LatLng;
import co.uk.hive.reactnativegeolocation.location.LocationController;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.facebook.react.bridge.*;
import com.google.android.gms.common.api.ApiException;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

public class RNGeolocationModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private final GeofenceController mGeofenceController;
    private final LocationController mLocationController;
    private final RNMapper mRnMapper;

    public RNGeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
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
                final ApiException apiException = (ApiException) t;
                final WritableMap errorMap = Arguments.createMap();

                errorMap.putString("exceptionType", t.getClass().toString());
                errorMap.putString("message", apiException.getMessage());
                errorMap.putString("code", String.valueOf(apiException.getStatusCode()));
                errorMap.putString("JIRA", "MA1-1200");

                callback.invoke(errorMap);
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
}
