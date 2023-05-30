package co.uk.hive.reactnativegeolocation.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.core.app.ActivityCompat;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;

public class LocationController {
    private final Context mContext;
    private final FusedLocationProviderClient mLocationClient;
    private final Handler mHandler;
    private static final long CURRENT_LOCATION_REQUEST_DURATION_MILLIS = 3000;
    private static final boolean IS_ANDROID_8_OR_BELOW = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);

    public LocationController(Context context) {
        mContext = context;
        mLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private boolean isLocationEnabled() {
        final LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (!IS_ANDROID_8_OR_BELOW) {
                return locationManager.isLocationEnabled();
            } else {
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
        }
        return false;
    }

    @MainThread
    @SuppressLint("MissingPermission")
    public void getCurrentPosition(CurrentPositionRequest currentPositionRequest,
                                   Function<LatLng, Object> successCallback,
                                   Function<Object, Object> failureCallback) {

        if (mLocationClient == null) {
            failureCallback.apply(LocationError.LOCATION_CLIENT_IS_NULL);
            return;
        }

        if (!isLocationEnabled()) {
            failureCallback.apply(LocationError.LOCATION_DISABLED);
            return;
        }

        if (!hasPermissions()) {
            failureCallback.apply(LocationError.PERMISSION_DENIED);
            return;
        }

        // Android 8 and below
        if (IS_ANDROID_8_OR_BELOW) {
            try {
                if (getLocationMode(mContext) == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                    requestLocation(currentPositionRequest, successCallback, failureCallback);
                } else {
                    failureCallback.apply(LocationError.LOCATION_SETTINGS_FAILED);
                }
            } catch (Settings.SettingNotFoundException settingNotFoundException) {
                // Settings not found, attempt to get location anyways
                requestLocation(currentPositionRequest, successCallback, failureCallback);
            }
            return;
        }

        // Android 9+
        final LocationRequest locationRequest = getLocationRequest(currentPositionRequest);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(mContext);

        client.checkLocationSettings(builder.build())
                .addOnFailureListener(TaskExecutors.MAIN_THREAD, failureCallback::apply)
                .addOnSuccessListener(TaskExecutors.MAIN_THREAD, ignored -> requestLocation(currentPositionRequest, successCallback, failureCallback));
    }

    private int getLocationMode(Context context) throws Settings.SettingNotFoundException {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
    }

    private boolean hasPermissions() {
        return Stream.of(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .allMatch(permission -> ActivityCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED);
    }

    private LocationRequest getLocationRequest(CurrentPositionRequest currentPositionRequest) {
        return new LocationRequest.Builder(5000)
                .setMaxUpdates(1)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setDurationMillis(currentPositionRequest.getTimeout())
                .build();
    }

    @MainThread
    private void requestLocation(CurrentPositionRequest currentPositionRequest,
                                 Function<LatLng, Object> successCallback,
                                 Function<Object, Object> failureCallback) {

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final String errorMessage = "Missing the required location permissions!";
            Log.e(LocationController.class.getName(), errorMessage);
            failureCallback.apply(LocationError.PERMISSION_DENIED);
            return;
        }

        final CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setDurationMillis(currentPositionRequest.getTimeout()).build();

        final Task<Location> currentLocationTask = mLocationClient.getCurrentLocation(currentLocationRequest, null);
        currentLocationTask.addOnSuccessListener(location -> {
            if (location == null || Double.isNaN(location.getLatitude()) || Double.isNaN(location.getLongitude())) {
                failureCallback.apply(LocationError.LOCATION_IS_NULL);
            } else {
                successCallback.apply(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        });
        currentLocationTask.addOnFailureListener(e -> {
            Log.e(LocationController.class.getName(), e.getMessage() != null ? e.getMessage() : "Unable to access current position!");
            failureCallback.apply(LocationError.CURRENT_LOCATION_FAILED);
        });
    }
}
