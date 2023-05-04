package co.uk.hive.reactnativegeolocation.data;

import android.content.Context;
import android.content.SharedPreferences;

public class DataStorage {
    private static final String SHARED_PREFERENCES_NAME =
            "connected-home_react-native-geolocation_geofence-repository";

    private final SharedPreferences mSharedPreferences;

    public DataStorage(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
    }

    public void store(String key, String data) {
        mSharedPreferences.edit().putString(key, data).apply();
    }

    public String load(String key) {
        return mSharedPreferences.getString(key, "");
    }
}
