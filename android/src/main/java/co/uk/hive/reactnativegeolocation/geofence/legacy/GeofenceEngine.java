package co.uk.hive.reactnativegeolocation.geofence.legacy;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import static com.google.android.gms.location.Geofence.*;

import co.uk.hive.reactnativegeolocation.geofence.GeofenceBroadcastReceiver;
import co.uk.hive.reactnativegeolocation.util.PermissionChecker;

public class GeofenceEngine {

    private final GeofencingClient mGeofencingClient;
    private final PermissionChecker mPermissionChecker;
    private final PendingIntent mPendingIntent;

    @SuppressLint("UnspecifiedImmutableFlag")
    GeofenceEngine(Context context) {
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        mPermissionChecker = new PermissionChecker(context);

        //final Intent intent = new Intent(context, GeofenceEventBroadcastReceiver.class);
        final Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mPendingIntent = PendingIntent.getActivity(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            mPendingIntent = PendingIntent.getActivity(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @SuppressLint("MissingPermission")
    public void addGeofences(List<Geofence> geofenceRequests, Function<? super Object, ? super Object> successCallback,
                             Function<? super Object, ? super Object> failureCallback) {
        if (!mPermissionChecker.isFullLocationPermissionGranted()) {
            // TODO: Return coded error to the app if all the time permissions are not granted
            final String errorMessage = "All-the-time location access needs to be granted before calling addGeofences";
            Log.e("Geofence", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        Log.d("Geofence", "addGeofences: " + geofenceRequests.toString());
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
                        .setNotificationResponsiveness(1000)
                        .build())
                .toList();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofences(geofences)
                .setInitialTrigger(defineInitialTrigger())
                .build();
        // TODO: Add proper error listeneres incase something useful comes back...
        // See: https://www.kodeco.com/7372-geofencing-api-tutorial-for-android
        mGeofencingClient.addGeofences(
                        geofencingRequest, mPendingIntent)
                .addOnSuccessListener(successCallback::apply)
                .addOnFailureListener(failureCallback::apply);
    }

    public void removeGeofences(List<String> geofenceIds, Function<? super Object, ? super Object> successCallback,
                                Function<? super Object, ? super Object> failureCallback) {
        Log.d("Geofence", "removeGeofences: " + geofenceIds.toString());
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
        //return GeofencingRequest.INITIAL_TRIGGER_DWELL;
    }
}
