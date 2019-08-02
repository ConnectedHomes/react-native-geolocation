package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;

class TestData {
    static Geofence createGeofence(String id) {
        return new Geofence(id, 1, 0, 0, false, false, false, 0);
    }

    static Geofence createInvalidGeofence(String id) {
        return new Geofence(id, 0, 0, 0, false, false, false, 0);
    }
}
