//import BackgroundGeolocation from 'react-native-background-geolocation';

//export default BackgroundGeolocation;

import { NativeModules, AppRegistry, NativeEventEmitter } from 'react-native';

const TAG = "GeofenceEventTask"

export default class RNGeolocation {

  static registerHeadlessTask(task) {
    AppRegistry.registerHeadlessTask(TAG, () => task);
  }

  static ready(config) {
      return new Promise((resolve, reject) => {
        NativeModules.RNGeolocation.ready();
        resolve();
      });
  }

  static onGeofence(geofenceListener) {
    const GeofenceEvents = new NativeEventEmitter(NativeModules.RNGeolocation)
    GeofenceEvents.addListener('geofence', geofenceListener);
  }

  static startGeofences(successCallback, failureCallback) {
    let promise = new Promise((resolve, reject) => {
      let success = ()      => { resolve() }
      let failure = (error) => { reject(error) }
      NativeModules.RNGeolocation.startGeofences(success, failure);
    });
    if (!arguments.length) {
      return promise;
    } else {
        return promise.then(successCallback).catch(failureCallback);
    }
  }
  
  static stop(successCallback, failureCallback) {
    let promise = new Promise((resolve, reject) => {
      let success = ()      => { resolve() }
      let failure = (error) => { reject(error) }
      NativeModules.RNGeolocation.stopGeofences(success, failure);
    });
    if (!arguments.length) {
      return promise;
    } else {
        return promise.then(successCallback).catch(failureCallback);
    }
  }

  static stopGeofences(successCallback, failureCallback) {
    return RNGeolocation.stop(successCallback, failureCallback);
  }

  static addGeofences(geofences) {
    return new Promise((resolve, reject) => {
        NativeModules.RNGeolocation.addGeofences(geofences);
        resolve();
      });
  }

  static removeGeofences() {
    return new Promise((resolve, reject) => {
      NativeModules.RNGeolocation.removeGeofences();
      resolve();
    });
  }

  static getCurrentPosition(currentPositionRequest, successCallback, failureCallback) {
    let promise = new Promise((resolve, reject) => {
            let success = (location)     => { resolve(location) }
            let failure = (error)        => { reject(error) }
            console.log("why am I here");
            
            NativeModules.RNGeolocation.getCurrentPosition(currentPositionRequest || {}, success, failure);
        });
    if (arguments.length == 1) {
      return promise;
    } else {
      return promise.then(successCallback).catch(failureCallback);
    }
  }

  static isLocationEnabled() {
    return NativeModules.RNGeolocation.isLocationEnabled();
  }
}