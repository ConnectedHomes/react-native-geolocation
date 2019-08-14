package co.uk.hive.reactnativegeolocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

public class PermissionChecker {

    private final Context mContext;

    public PermissionChecker(Context context) {
        mContext = context;
    }

    public boolean isFullLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return isFineLocationPermissionGranted() && isBackgroundLocationPermissionGranted();
        } else {
            return isFineLocationPermissionGranted();
        }
    }

    private boolean isBackgroundLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isFineLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
