package co.uk.hive.reactnativegeolocation.geofence.legacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;

import co.uk.hive.reactnativegeolocation.util.LocationChecker;
import co.uk.hive.reactnativegeolocation.util.PermissionChecker;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import java.util.Objects;

import static co.uk.hive.reactnativegeolocation.geofence.legacy.ReRegisterGeofencesReceiver.LocationChangeResult.CHANGED_TO_DISABLED;
import static co.uk.hive.reactnativegeolocation.geofence.legacy.ReRegisterGeofencesReceiver.LocationChangeResult.CHANGED_TO_ENABLED;

/**
 * Re-registers the geofences in the following conditions:
 * Pre-Oreo:
 *   - location services enabled (for O+, {@link ReRegisterGeofencesOreoJobService} is used instead)
 * All SDK versions:
 *   - boot completed
 *   - this app package replaced
 *   - Google Play Services data cleared
 *
 * Details:
 *   - https://developer.android.com/training/location/geofencing#re-register-geofences-only-when-required
 *   - https://stackoverflow.com/a/50869301/1688728
 */
public class ReRegisterGeofencesReceiver extends BroadcastReceiver {

    private Function<? super Object, ? super Object> mEmptyCallback = o -> null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!actionMatches(Objects.requireNonNull(intent.getAction()))) {
            return;
        }

        if (packageOtherThanGmsCleared(intent)) {
            return;
        }

        if (CHANGED_TO_DISABLED.equals(isLocationServicesChanged(context, intent))) {
            return;
        }

        if (!isFullLocationPermissionGranted(context)) {
            GeofenceLog.d("All-the-time location access not granted. Cannot restart geofencing");
            return;
        }

        GeofenceLog.d("Attempting to restart geofences due to: " + intent.getAction());
        GeofenceController geofenceController = GeofenceServiceLocator.getGeofenceController(context);
        geofenceController.restart(mEmptyCallback, mEmptyCallback);
    }

    private boolean actionMatches(String action) {
        return Stream.of(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_DATA_CLEARED,
                LocationManager.MODE_CHANGED_ACTION)
                .anyMatch(action::equals);
    }

    private boolean packageOtherThanGmsCleared(Intent intent) {
        if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
            Uri uri = intent.getData();
            return uri != null && !uri.toString().equals("package:com.google.android.gms");
        }
        return false;
    }

    private LocationChangeResult isLocationServicesChanged(Context context, Intent intent) {
        if (LocationManager.MODE_CHANGED_ACTION.equals(intent.getAction())) {
            LocationChecker locationChecker = new LocationChecker(context);
            return locationChecker.isLocationEnabled() ? CHANGED_TO_ENABLED : CHANGED_TO_DISABLED;
        }
        return LocationChangeResult.NOT_CHANGED;
    }

    enum LocationChangeResult {
        NOT_CHANGED,
        CHANGED_TO_ENABLED,
        CHANGED_TO_DISABLED,
    }

    private boolean isFullLocationPermissionGranted(Context context) {
        return new PermissionChecker(context).isFullLocationPermissionGranted();
    }
}
