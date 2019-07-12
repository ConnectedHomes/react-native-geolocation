package co.uk.hive.reactnativegeolocation.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

public class LocationController {
    private final Context mContext;
    private final FusedLocationProviderClient mLocationClient;

    public LocationController(Context context) {
        mContext = context;
        mLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentPosition(CurrentPositionRequest currentPositionRequest,
                                   Function<LatLng, Object> successCallback,
                                   Function<Object, Object> failureCallback) {
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
                .addOnFailureListener(Runnable::run, ignored -> failureCallback.apply(LocationError.LOCATION_UNKNOWN))
                .addOnSuccessListener(Runnable::run, ignored -> mLocationClient.requestLocationUpdates(getLocationRequest(currentPositionRequest), new LocationCallback() {
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult.getLastLocation() != null) {
                            successCallback.apply(new LatLng(
                                    locationResult.getLastLocation().getLatitude(),
                                    locationResult.getLastLocation().getLongitude()));
                        } else {
                            failureCallback.apply(LocationError.LOCATION_UNKNOWN);
                        }
                    }
        }, Looper.getMainLooper()));
    }

    private boolean hasPermissions() {
        return Stream.of(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                .allMatch(permission -> ActivityCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED);
    }

    private LocationRequest getLocationRequest(CurrentPositionRequest currentPositionRequest) {
        return new LocationRequest()
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }
}
