package co.uk.hive.reactnativegeolocation.geofence;

import android.os.Build;
import android.util.Log;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import java.util.List;

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
        start(successCallback, failureCallback, mGeofenceRepository.getGeofences());
    }

    public void start(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback, List<Geofence> geofences) {
        if (geofences.isEmpty()) {
            Log.w(getClass().getSimpleName(), "Starting geofences with none set, exiting");
            return;
        }

        mGeofenceEngine.addGeofences(
                geofences,
                successResult -> {
                    mGeofenceActivator.setGeofencesActivated(true);
                    return successCallback.apply(successResult);
                },
                failureCallback
        );
    }

    public void stop(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        if (mGeofenceRepository.getGeofences().isEmpty()) {
            Log.w(getClass().getSimpleName(), "Stopping geofences with none set, exiting");
            return;
        }

        List<String> geofenceIds = getGeofenceIdsToRemove();
        if (!geofenceIds.isEmpty()) {
            mGeofenceEngine.removeGeofences(
                    geofenceIds,
                    successResult -> {
                        mGeofenceActivator.setGeofencesActivated(false);
                        return successCallback.apply(successResult);
                    },
                    failureCallback
            );
        }
    }

    public void restart(Function<? super Object, ? super Object> successCallback, Function<? super Object, ? super Object> failureCallback) {
        if (mGeofenceActivator.areGeofencesActivated()) {
            final List<Geofence> geofences = Stream.of(mGeofenceRepository.getGeofences())
                    .filterNot(GeofenceController::isInvalidGeofence)
                    .toList();

            start(successCallback, failureCallback, geofences);
        }
    }

    private List<String> getGeofenceIdsToRemove() {
        return Stream.of(mGeofenceRepository.getGeofences())
                .filterNot(GeofenceController::isInvalidGeofence)
                .map(Geofence::getId)
                .toList();
    }

    private static boolean isInvalidGeofence(Geofence geofence) {
        // Removes geofences where `setGeofencesActivated` was set to `true` before crashing
        // They cannot be restarted (it'll crash) and they cannot be removed (they were never added)
        // Geofences added from 1.2.1 should never meet this condition
        return geofence.getRadius() == 0;
    }

    public void addGeofences(List<Geofence> geofences) {
        mGeofenceRepository.addGeofences(geofences);
    }

    public void removeAllGeofences() {
        mGeofenceRepository.removeAllGeofences();
    }

    public Optional<Geofence> getGeofenceById(String id) {
        return mGeofenceRepository.getGeofenceById(id);
    }

    public void setupReregistration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mReRegistrationScheduler.scheduleReRegistration();
        } // else: implicit broadcast will be triggered
    }
}
