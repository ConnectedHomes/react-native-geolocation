package co.uk.hive.reactnativegeolocation.geofence.legacy;

import android.os.Build;
import android.util.Log;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import java.util.List;

import co.uk.hive.reactnativegeolocation.data.GeofenceRepository;

@SuppressWarnings("WeakerAccess")
public class GeofenceController {
    private final GeofenceEngine mGeofenceEngine;
    private final GeofenceRepository mGeofenceRepository;
    private final GeofenceActivator mGeofenceActivator;
    private final ReRegistrationScheduler mReRegistrationScheduler;

    GeofenceController(GeofenceEngine geofenceEngine,
            GeofenceRepository geofenceRepository,
                       GeofenceActivator geofenceActivator,
                       ReRegistrationScheduler reRegistrationScheduler) {
        mGeofenceEngine = geofenceEngine;
        mGeofenceRepository = geofenceRepository;
        mGeofenceActivator = geofenceActivator;
        mReRegistrationScheduler = reRegistrationScheduler;
    }

    public void start(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        if (mGeofenceRepository.getGeofences().isEmpty()) {
            Log.w(getClass().getSimpleName(), "Starting geofences with none set, exiting");
            return;
        }
        mGeofenceActivator.setGeofencesActivated(true);
        Log.d("Geofence", "GeofenceController::start: " + mGeofenceRepository.getGeofences().toString());
        mGeofenceEngine.addGeofences(mGeofenceRepository.getGeofences(), successCallback, failureCallback);
    }

    public void stop(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        if (mGeofenceRepository.getGeofences().isEmpty()) {
            Log.w(getClass().getSimpleName(), "Stopping geofences with none set, exiting");
            return;
        }
        mGeofenceActivator.setGeofencesActivated(false);
        List<String> geofenceIds = getGeofenceIds();
        if (!geofenceIds.isEmpty()) {
            Log.d("Geofence", "GeofenceController::removeGeofences: " + geofenceIds.toString());
            mGeofenceEngine.removeGeofences(geofenceIds, successCallback, failureCallback);
        }
    }

    public void restart(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        if (mGeofenceActivator.areGeofencesActivated()) {
            start(successCallback, failureCallback);
        }
    }

    // TODO: Add debug JS bridge function to return this to the app
    private List<String> getGeofenceIds() {
        return Stream.of(mGeofenceRepository.getGeofences())
                .map(Geofence::getId)
                .toList();
    }

    public void addGeofences(List<Geofence> geofences) {
        Log.d("Geofence", "GeofenceController::addGeofences: " + geofences.toString());
        mGeofenceRepository.addGeofences(geofences);
    }

    public void removeAllGeofences() {
        Log.d("Geofence", "GeofenceController::removeAllGeofences");
        mGeofenceRepository.removeAllGeofences();
    }

    public Optional<Geofence> getGeofenceById(String id) {
        return mGeofenceRepository.getGeofenceById(id);
    }

    // TODO: Test new implementation on O and/or port over re-registration
    public void setupReregistration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mReRegistrationScheduler.scheduleReRegistration();
        } // else: implicit broadcast will be triggered
    }
}
