import { Platform } from 'react-native';
import GeolocationIOS from './index.ios.js';
import GeolocationAndroid from './index.android.js';

const PlatformGeolocation = Platform.OS === 'ios'
  ? GeolocationIOS
  : GeolocationAndroid;
  
// https://transistorsoft.github.io/react-native-background-geolocation/modules/_react_native_background_geolocation_.html#locationerror
const LocationError = {
  LOCATION_UNKNOWN: 0,
  PERMISSION_DENIED: 1,
  NETWORK_ERROR: 2,
  LOCATION_CLIENT_IS_NULL: 3,
  LOCATION_DISABLED: 4,
  LOCATION_IS_NULL: 5,
  CURRENT_LOCATION_FAILED: 6,
  LOCATION_SETTINGS_FAILED: 7,
  LOCATION_TIMEOUT: 408,
};

export default class Geolocation extends PlatformGeolocation {
  static isLocationUnknown(error) {
      return [
        LocationError.PERMISSION_DENIED,
        LocationError.LOCATION_CLIENT_IS_NULL ,
        LocationError.LOCATION_DISABLED ,
        LocationError.LOCATION_IS_NULL ,
        LocationError.CURRENT_LOCATION_FAILED ,
        LocationError.LOCATION_SETTINGS_FAILED ,
      ].includes(error);
  }
  
  static isPermissionDenied(error) {
    return error === LocationError.PERMISSION_DENIED;
  }

  static isLocationDisabled(error) {
    return error === LocationError.LOCATION_DISABLED;
  }

  static isNetworkError(error) {
    return error === LocationError.NETWORK_ERROR;
  }

  static isLocationTimeout(error) {
    return error === LocationError.LOCATION_TIMEOUT;
  }
}
