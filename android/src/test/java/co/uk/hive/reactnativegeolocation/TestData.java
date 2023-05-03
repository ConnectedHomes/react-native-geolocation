package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.geofence.legacy.Geofence;

class TestData {
    static Geofence createGeofence(String id) {
        return new Geofence(id, 0, 0, 0, false, false, false, 0);
    }
}
