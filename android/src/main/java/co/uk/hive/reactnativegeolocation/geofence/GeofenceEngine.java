package co.uk.hive.reactnativegeolocation.geofence;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import static com.google.android.gms.location.Geofence.*;

import co.uk.hive.reactnativegeolocation.PermissionChecker;

public class GeofenceEngine {

    private final GeofencingClient mGeofencingClient;
    private final PermissionChecker mPermissionChecker;
    private final PendingIntent mPendingIntent;

    @SuppressLint("UnspecifiedImmutableFlag")
    GeofenceEngine(Context context) {
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        mPermissionChecker = new PermissionChecker(context);

        Intent intent = new Intent(context, GeofenceEventBroadcastReceiver.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mPendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            mPendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @SuppressLint("MissingPermission")
    public void addGeofences(List<Geofence> geofenceRequests, Function<? super Object, ? super Object> successCallback,
                             Function<? super Object, ? super Object> failureCallback) {
        if (!mPermissionChecker.isFullLocationPermissionGranted()) {
            throw new IllegalStateException("All-the-time location access needs to be granted before calling addGeofences");
        }

        List<com.google.android.gms.location.Geofence> geofences = Stream.of(geofenceRequests)
                .map(geofence -> new com.google.android.gms.location.Geofence.Builder()
                        .setRequestId(geofence.getId())
                        .setCircularRegion(
                                geofence.getLatitude(),
                                geofence.getLongitude(),
                                geofence.getRadius())
                        .setLoiteringDelay(geofence.getLoiteringDelay())
                        .setTransitionTypes(defineTransitionTypes(geofence))
                        .setExpirationDuration(NEVER_EXPIRE)
                        .build())
                .toList();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofences(geofences)
                .setInitialTrigger(defineInitialTrigger())
                .build();

        mGeofencingClient.addGeofences(
                        geofencingRequest, mPendingIntent)
                .addOnSuccessListener(successCallback::apply)
                .addOnFailureListener(failureCallback::apply);
    }

    public void removeGeofences(List<String> geofenceIds, Function<? super Object, ? super Object> successCallback,
                                Function<? super Object, ? super Object> failureCallback) {
        mGeofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(successCallback::apply)
                .addOnFailureListener(failureCallback::apply);
    }

    private int defineTransitionTypes(Geofence geofence) {
        return (geofence.isNotifyOnEnter() ? GEOFENCE_TRANSITION_ENTER : 0)
                | (geofence.isNotifyOnExit() ? GEOFENCE_TRANSITION_EXIT : 0)
                | (geofence.isNotifyOnDwell() ? GEOFENCE_TRANSITION_DWELL : 0);
    }

    private int defineInitialTrigger() {
        return 0; // do not notify at the moment of setting the geofence
    }
}
