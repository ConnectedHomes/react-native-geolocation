
package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceServiceLocator;
import co.uk.hive.reactnativegeolocation.location.LatLng;
import co.uk.hive.reactnativegeolocation.location.LocationController;
import co.uk.hive.reactnativegeolocation.location.LocationError;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.facebook.react.bridge.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("LOCATION_UNKNOWN", LocationError.LOCATION_UNKNOWN);
        constants.put("PERMISSION_DENIED", LocationError.PERMISSION_DENIED);
        return constants;
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

    private <T> Function<T, Object> convertCallback(Callback callback) {
        return t -> {
            callback.invoke(t);
            return null;
        };
    }
}
