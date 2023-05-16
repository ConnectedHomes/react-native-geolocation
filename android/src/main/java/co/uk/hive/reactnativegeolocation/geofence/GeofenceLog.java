package co.uk.hive.reactnativegeolocation.geofence;

import android.util.Log;

import uk.co.centrica.hive.reactnativegeolocation.BuildConfig;

public class GeofenceLog {

    private static final String TAG = "ReactNativeGeolocation";

    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message);
        }
    }
}
