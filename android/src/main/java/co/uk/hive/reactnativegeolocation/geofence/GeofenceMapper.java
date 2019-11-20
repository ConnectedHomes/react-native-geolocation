package co.uk.hive.reactnativegeolocation.geofence;

import android.location.Location;
import android.os.PersistableBundle;
import com.google.android.gms.location.GeofencingEvent;

import static com.google.android.gms.location.Geofence.*;

class GeofenceMapper {
    PersistableBundle toBundle(GeofencingEvent event, Geofence geofence, long timestamp) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("action", getGeofenceAction(event.getGeofenceTransition()));
        bundle.putString("identifier", geofence.getId());
        bundle.putLong("timestamp", timestamp);
        bundle.putPersistableBundle("location", getLocationBundle(event.getTriggeringLocation()));
        return bundle;
    }

    private PersistableBundle getLocationBundle(Location triggeringLocation) {
        PersistableBundle bundle = new PersistableBundle();
        PersistableBundle coordsBundle = new PersistableBundle();
        coordsBundle.putDouble("latitude", triggeringLocation.getLatitude());
        coordsBundle.putDouble("longitude", triggeringLocation.getLongitude());
        bundle.putPersistableBundle("coords", coordsBundle);
        return bundle;
    }

    private String getGeofenceAction(int geofenceTransition) {
        switch (geofenceTransition) {
            case GEOFENCE_TRANSITION_ENTER:
                return "ENTER";
            case GEOFENCE_TRANSITION_EXIT:
                return "EXIT";
            case GEOFENCE_TRANSITION_DWELL:
                return "DWELL";
            default:
                return null;
        }
    }
}
