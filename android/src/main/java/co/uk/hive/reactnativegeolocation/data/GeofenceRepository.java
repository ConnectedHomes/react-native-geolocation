package co.uk.hive.reactnativegeolocation.data;

import com.annimon.stream.Optional;

import java.util.List;

import co.uk.hive.reactnativegeolocation.geofence.legacy.Geofence;

public interface GeofenceRepository {
    void addGeofences(List<Geofence> geofences);

    void removeAllGeofences();

    List<Geofence> getGeofences();

    Optional<Geofence> getGeofenceById(String id);
}
