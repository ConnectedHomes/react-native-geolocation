package co.uk.hive.reactnativegeolocation.location;

/**
 * Mimics react-native-background-location's LocationError values.
 * https://transistorsoft.github.io/react-native-background-geolocation/modules/_react_native_background_geolocation_.html#locationerror
 */
public class LocationError {
    public static final int LOCATION_UNKNOWN = 0;
    public static final int PERMISSION_DENIED = 1;
    public static final int LOCATION_CLIENT_IS_NULL = 3;
    public static final int LOCATION_DISABLED = 4;
    public static final int LOCATION_IS_NULL = 5;
    public static final int CURRENT_LOCATION_FAILED = 6;
    public static final int LOCATION_SETTINGS_FAILED = 7;
}
