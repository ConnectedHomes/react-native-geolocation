package co.uk.hive.reactnativegeolocation.util;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

public class LocationServicesChecker {

    private final Context mContext;

    public LocationServicesChecker(Context context) {
        mContext = context;
    }

    public boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(
                    mContext.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
            );

            return mode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }

}
