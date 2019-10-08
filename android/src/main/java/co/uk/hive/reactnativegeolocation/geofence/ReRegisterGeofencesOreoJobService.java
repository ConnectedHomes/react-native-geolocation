package co.uk.hive.reactnativegeolocation.geofence;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.annimon.stream.function.Function;

import co.uk.hive.reactnativegeolocation.LocationChecker;
import co.uk.hive.reactnativegeolocation.PermissionChecker;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ReRegisterGeofencesOreoJobService extends JobService {

    private static final boolean COMPLETE = false;

    private Function<? super Object, ? super Object> mEmptyCallback = o -> null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (!isFullLocationPermissionGranted()) {
            GeofenceLog.d("All-the-time location access not granted. Cannot restart geofencing");
            return COMPLETE;
        }
        
        LocationChecker locationChecker = new LocationChecker(this);
        if (locationChecker.isLocationEnabled()) {
            GeofenceController geofenceController = GeofenceServiceLocator.getGeofenceController(this);
            geofenceController.restart(mEmptyCallback, mEmptyCallback);
        }

        ReRegistrationScheduler scheduler = new ReRegistrationScheduler(this);
        scheduler.scheduleReRegistration();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private boolean isFullLocationPermissionGranted() {
        return new PermissionChecker(this).isFullLocationPermissionGranted();
    }
}
