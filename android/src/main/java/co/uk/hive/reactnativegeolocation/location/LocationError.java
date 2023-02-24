package co.uk.hive.reactnativegeolocation.location;

/**
 * Mimics react-native-background-location's LocationError values.
 * https://transistorsoft.github.io/react-native-background-geolocation/modules/_react_native_background_geolocation_.html#locationerror
 */
class LocationError {
    static final int LOCATION_UNKNOWN = 0;
    static final int PERMISSION_DENIED = 1;
    static final int LOCATION_CLIENT_IS_NULL = 3;
    static final int LOCATION_DISABLED = 4;
    static final int LOCATION_IS_NULL = 5;
    static final int CURRENT_LOCATION_FAILED = 6;
    static final int LOCATION_SETTINGS_FAILED = 7;
}
