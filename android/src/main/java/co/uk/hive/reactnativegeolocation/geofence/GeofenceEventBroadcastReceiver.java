package co.uk.hive.reactnativegeolocation.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import co.uk.hive.reactnativegeolocation.ForegroundChecker;
import co.uk.hive.reactnativegeolocation.RNMapper;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceEventBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = GeofenceEventBroadcastReceiver.class.getSimpleName();
    private static final String GEOFENCE_EVENT_NAME = "geofence";

    private GeofenceController mGeofenceController;
    private ForegroundChecker mForegroundChecker;
    private final GeofenceMapper mGeofenceMapper = new GeofenceMapper();
    private final RNMapper mRnMapper = new RNMapper();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mGeofenceController == null) {
            mGeofenceController =
                    GeofenceServiceLocator.getGeofenceController(context.getApplicationContext());
        }

        if (mForegroundChecker == null) {
            mForegroundChecker = new ForegroundChecker(context);
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent.fromIntent returned null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage =
                    GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        Stream.of(geofencingEvent.getTriggeringGeofences())
                .map(gmsGeofence -> mGeofenceController.getGeofenceById(gmsGeofence.getRequestId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(geofence -> sendEvent(context, geofence, geofencingEvent));
    }

    private void sendEvent(Context context, Geofence geofence, GeofencingEvent event) {
        long timestamp = System.currentTimeMillis() / 1000;
        PersistableBundle bundle = mGeofenceMapper.toBundle(event, geofence, timestamp);

        ReactContext reactContext = getReactContext(context);

        boolean canEmitToForegroundRN =
                mForegroundChecker.isAppInForeground()
                        && hasActiveInstance(reactContext);

        if (canEmitToForegroundRN) {
            try {
                emitRNEvent(reactContext, bundle);
                return;
            } catch (Throwable t) {
                // If RN is "active" but JS isn't ready or emit throws, fall back to Headless JS.
                Log.w(TAG, "Emit to RN failed; falling back to Headless JS", t);
            }
        }

        // Background OR RN not ready OR emit failed then Headless JS
        runHeadlessJsTask(context, bundle);
    }

    /**
     * React Native deprecates hasActiveCatalystInstance() in favor of hasActiveReactInstance(). [1](https://github.com/facebook/react-native/pull/35718/checks?check_run_id=10303976842)
     */
    private boolean hasActiveInstance(ReactContext reactContext) {
        if (reactContext == null) return false;
        try {
            return reactContext.hasActiveReactInstance();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void emitRNEvent(ReactContext reactContext, PersistableBundle bundle) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(GEOFENCE_EVENT_NAME, mRnMapper.fromBundle(new Bundle(bundle)));
    }

    private void runHeadlessJsTask(Context context, PersistableBundle bundle) {
        GeofenceHeadlessJsTaskService.start(context, bundle);
        // ensure CPU stays awake while JS task spins up
        // IMPORTANT: If starting HeadlessJsTaskService from BroadcastReceiver,
        // acquireWakeLockNow MUST be called before onReceive() returns,
        // otherwise the CPU may sleep before the service starts.
        HeadlessJsTaskService.acquireWakeLockNow(context);
    }

    // helper for obtaining the current ReactContext (may be null)
    private ReactContext getReactContext(Context context) {
        try {
            Context appContext = context.getApplicationContext();
            if (!(appContext instanceof ReactApplication)) {
                Log.w(TAG, "Cannot obtain ReactNativeHost");
                return null;
            }

            ReactNativeHost host = ((ReactApplication) appContext).getReactNativeHost();
            ReactInstanceManager rim = host.getReactInstanceManager();
            return rim.getCurrentReactContext();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get ReactContext", e);
            return null;
        }
    }
}
