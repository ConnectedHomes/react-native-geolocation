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
  LOCATION_TIMEOUT: 408,
};

export default class Geolocation extends PlatformGeolocation {
  static isLocationUnknown(error) {
      return error === LocationError.LOCATION_UNKNOWN;
  }
  
  static isPermissionDenied(error) {
      return error === LocationError.PERMISSION_DENIED;
  }

  static isNetworkError(error) {
    return error === LocationError.NETWORK_ERROR;
  }

  static isLocationTimeout(error) {
    return error === LocationError.LOCATION_TIMEOUT;
  }
}
