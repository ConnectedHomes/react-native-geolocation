package co.uk.hive.reactnativegeolocation.sample_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.location.LatLng;

interface IGeoFenceController {
    void addGeofences(List<Geofence> geofences);

    void start(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback);

    void stop(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback);

    void removeAllGeofences();

    List<String> getGeofenceIds();

    Optional<Geofence> getGeofenceById(String id);
}

public class GeoFenceController implements IGeoFenceController {

    private final Context mContext;
    private final GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private final ArrayList<com.google.android.gms.location.Geofence> mGeofenceList;

    public GeoFenceController(Context context) {
        this.mContext = context;
        this.mGeofencingClient = LocationServices.getGeofencingClient(context);
        mGeofenceList = new ArrayList<>();
    }

    @Override
    public void addGeofences(List<Geofence> geofences) {
        // TODO: Make this dynamic!!! Will add to repo
        for (Map.Entry<String, LatLng> entry : Constants.GEO_FENCE_LANDMARKS.entrySet()) {

            int transition = 0;
            switch (entry.getKey()){
                case "ARRIVING": {
                    transition = com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
                    break;
                }
                case "LEAVING": {
                    transition = com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
                    break;
                }
            }

            mGeofenceList.add(new com.google.android.gms.location.Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().getLatitude(),
                            entry.getValue().getLongitude(),
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    // TODO: Set to never Geofence.NEVER_EXPIRE
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(transition)

                    // Create the geofence.
                    .build());
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void start(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        // TODO: Pull geofences from repo and pass to addGeofences
        if (!checkPermissions()) {
            Toast.makeText(mContext, mContext.getString(R.string.insufficient_permissions), Toast.LENGTH_SHORT).show();
            return;
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(mContext))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    @Override
    public void stop(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        // TODO: Do we ever need to stop? Wouldn't it always just be remove?
    }

    @Override
    public void removeAllGeofences() {
        if (!checkPermissions()) {
            Toast.makeText(mContext, mContext.getString(R.string.insufficient_permissions), Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: Add success/complete/failure listeners
        mGeofencingClient.removeGeofences(getGeofencePendingIntent(mContext)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @Override
    public List<String> getGeofenceIds() {
        // TODO: Implement with proper in use preferences storage
        return null;
    }

    @Override
    public Optional<Geofence> getGeofenceById(String id) {
        return null;
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(0);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        final Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        // TODO: Add pre S compatibility!
        mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // TODO: Investigate any re-registration required (Android O quirks/requirements?)
        return mGeofencePendingIntent;
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() { // TODO: Pass in context
        // TODO: Include background/always allow
        int permissionState = ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}
