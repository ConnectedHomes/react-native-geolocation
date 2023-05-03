package co.uk.hive.reactnativegeolocation.util;

import android.content.Context;
import android.location.LocationManager;
import com.annimon.stream.Stream;

public class LocationChecker {
    private final Context mContext;

    public LocationChecker(Context context) {
        mContext = context;
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return Stream.of(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER)
                .anyMatch(locationManager::isProviderEnabled);
        }
        return false;
    }
}
