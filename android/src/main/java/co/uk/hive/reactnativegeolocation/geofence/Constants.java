package co.uk.hive.reactnativegeolocation.geofence;

import java.util.HashMap;

import co.uk.hive.reactnativegeolocation.location.LatLng;

/**
 * Constants used in this sample.
 */

public final class Constants {

    private Constants() {
    }

    private static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    //static final float GEOFENCE_RADIUS_IN_METERS = 1609; // 1 mile, 1.6 km
    public static final float GEOFENCE_RADIUS_IN_METERS = 183; // 1 mile, 1.6 km

    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    public static final HashMap<String, LatLng> GEO_FENCE_LANDMARKS = new HashMap<>();

    static {
//        // San Francisco International Airport.
//        GEO_FENCE_LANDMARKS.put("SFO", new LatLng(37.621313, -122.378955));
//        // Googleplex.
//        GEO_FENCE_LANDMARKS.put("GOOGLE", new LatLng(37.422611,-122.0840577));
        GEO_FENCE_LANDMARKS.put("ARRIVING", new LatLng(51.441846, -2.602087));
        GEO_FENCE_LANDMARKS.put("LEAVING", new LatLng(51.441846, -2.602087));
    }
}
