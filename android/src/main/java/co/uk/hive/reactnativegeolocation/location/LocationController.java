package co.uk.hive.reactnativegeolocation.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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

    public LocationController(Context context) {
        mContext = context;
        mLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @MainThread
    @SuppressLint("MissingPermission")
    public void getCurrentPosition(CurrentPositionRequest currentPositionRequest,
                                   Function<LatLng, Object> successCallback,
                                   Function<Object, Object> failureCallback) {

        // Android 8 and below (Fused location provider not working when device only location mode set in OS)
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            final CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setGranularity(Granularity.GRANULARITY_FINE)
                    .setDurationMillis(CURRENT_LOCATION_REQUEST_DURATION_MILLIS).build();

            final Task<Location> currentLocationTask = mLocationClient.getCurrentLocation(currentLocationRequest, null);
            currentLocationTask.addOnSuccessListener(location -> successCallback.apply(new LatLng(location.getLatitude(), location.getLongitude())));
            currentLocationTask.addOnFailureListener(e -> {
                Log.e(LocationController.class.getName(), e.getMessage() != null ? e.getMessage() : "Unable to access current position!");
                failureCallback.apply(LocationError.LOCATION_UNKNOWN);
            });
            return;
        }

        // Android 9+ (Fused location provider will use a number of sources to calculate accuracy - cell, wifi, bluetooth, gps etc..)
        if (mLocationClient == null) {
            failureCallback.apply(LocationError.LOCATION_UNKNOWN);
            return;
        }

        if (!hasPermissions()) {
            failureCallback.apply(LocationError.PERMISSION_DENIED);
            return;
        }

        final LocationRequest locationRequest = getLocationRequest(currentPositionRequest);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(mContext);

        client.checkLocationSettings(builder.build())
                .addOnFailureListener(TaskExecutors.MAIN_THREAD, ignored -> failureCallback.apply(LocationError.LOCATION_UNKNOWN))
                .addOnSuccessListener(TaskExecutors.MAIN_THREAD, ignored -> requestLocation(currentPositionRequest, successCallback, failureCallback));
    }

    private boolean hasPermissions() {
        return Stream.of(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .allMatch(permission -> ActivityCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED);
    }

    private LocationRequest getLocationRequest(CurrentPositionRequest currentPositionRequest) {
        return LocationRequest.create()
                .setNumUpdates(1)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setExpirationDuration(currentPositionRequest.getTimeout());
    }

    @MainThread
    private void requestLocation(CurrentPositionRequest currentPositionRequest,
                                 Function<LatLng, Object> successCallback,
                                 Function<Object, Object> failureCallback) {
        final SingleLocationCallback singleLocationCallback = new SingleLocationCallback(successCallback, failureCallback);

        final LocationCallback locationCallback = new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                mHandler.removeCallbacksAndMessages(null);
                if (locationResult.getLastLocation() != null) {
                    singleLocationCallback.locationReceived(new LatLng(
                            locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()));
                } else {
                    singleLocationCallback.locationUnknown();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final String errorMessage = "Missing the required location permissions!";
            Log.e(LocationController.class.getName(), errorMessage);
            failureCallback.apply(errorMessage);
            return;
        }
        mLocationClient.requestLocationUpdates(getLocationRequest(currentPositionRequest), locationCallback, Looper.getMainLooper());

        final long timeout = currentPositionRequest.getTimeout();
        mHandler.postDelayed(() -> {
            mLocationClient.removeLocationUpdates(locationCallback);
            singleLocationCallback.locationUnknown();
        }, timeout);
    }

    private static class SingleLocationCallback {
        private final Function<LatLng, Object> mSuccessCallback;
        private final Function<Object, Object> mFailureCallback;
        private boolean mCalledBack;

        private SingleLocationCallback(Function<LatLng, Object> successCallback, Function<Object, Object> failureCallback) {
            mSuccessCallback = successCallback;
            mFailureCallback = failureCallback;
        }

        private void locationReceived(LatLng latLng) {
            if (mCalledBack) {
                return;
            }

            mCalledBack = true;
            mSuccessCallback.apply(latLng);
        }

        private void locationUnknown() {
            if (mCalledBack) {
                return;
            }

            mCalledBack = true;
            mFailureCallback.apply(LocationError.LOCATION_UNKNOWN);
        }
    }
}
