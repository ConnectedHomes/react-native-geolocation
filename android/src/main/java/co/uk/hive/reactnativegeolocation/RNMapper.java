package co.uk.hive.reactnativegeolocation;

import android.os.Bundle;
import android.os.PersistableBundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.location.CurrentPositionRequest;
import co.uk.hive.reactnativegeolocation.location.LatLng;

public class RNMapper {
    Geofence readGeofence(ReadableMap map) {
        return new Geofence(
                readString(map,"identifier", null),
                readInt(map,"radius", 0),
                readDouble(map,"latitude", 0d),
                readDouble(map,"longitude", 0d),
                readBoolean(map,"notifyOnEntry", true),
                readBoolean(map,"notifyOnExit", true),
                readBoolean(map,"notifyOnDwell", false),
                readInt(map,"loiteringDelay", 0));
    }

    WritableMap writeLocation(LatLng location) {
        WritableMap coords = Arguments.createMap();
        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        WritableMap result = Arguments.createMap();
        result.putMap("coords", coords);
        return result;
    }

    CurrentPositionRequest readPositionRequest(ReadableMap readableMap) {
        final int timeout = readInt(readableMap, "timeout", CurrentPositionRequest.DEFAULT_TIMEOUT);
        return new CurrentPositionRequest(timeout);
    }

    private static String readString(ReadableMap map, String key, String defaultValue) {
        return map.hasKey(key) ? map.getString(key) : defaultValue;
    }

    private static int readInt(ReadableMap map, String key, int defaultValue) {
        return map.hasKey(key) ? map.getInt(key) : defaultValue;
    }

    private static double readDouble(ReadableMap map, String key, double defaultValue) {
        return map.hasKey(key) ? map.getDouble(key) : defaultValue;
    }

    private static boolean readBoolean(ReadableMap map, String key, boolean defaultValue) {
        return map.hasKey(key) ? map.getBoolean(key) : defaultValue;
    }

    public WritableMap writeGeofenceTaskParams(String name, Bundle params) {
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putBundle("params", params);
        return fromBundle(args);
    }

    public WritableMap fromBundle(Bundle bundle) {
        WritableMap map = Arguments.createMap();

        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value == null) {
                map.putNull(key);
            } else if (value.getClass().isArray()) {
                map.putArray(key, Arguments.fromArray(value));
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else if (value instanceof Number) {
                if (value instanceof Integer) {
                    map.putInt(key, (Integer) value);
                } else {
                    map.putDouble(key, ((Number) value).doubleValue());
                }
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Bundle) {
                map.putMap(key, fromBundle((Bundle) value));
            } else if (value instanceof PersistableBundle) {
                map.putMap(key, fromBundle(new Bundle((PersistableBundle) value)));
            } else {
                if (!(value instanceof List)) {
                    throw new IllegalArgumentException("Could not convert " + value.getClass());
                }

                map.putArray(key, Arguments.fromList((List) value));
            }
        }

        return map;
    }
}
