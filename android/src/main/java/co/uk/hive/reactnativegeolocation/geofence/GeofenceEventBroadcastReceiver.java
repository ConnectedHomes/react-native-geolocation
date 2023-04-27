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
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceEventBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = GeofenceEventBroadcastReceiver.class.getSimpleName();
    private static final String GEOFENCE_EVENT_NAME = "geofence";

    private GeofenceController mGeofenceController;
    private ForegroundChecker mForegroundChecker;
    private GeofenceMapper mGeofenceMapper = new GeofenceMapper();
    private RNMapper mRnMapper = new RNMapper();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Geofence", "GeofenceEventBroadcastReceiver::onReceive: " + intent.toString());
        if (mGeofenceController == null) {
            mGeofenceController = GeofenceServiceLocator.getGeofenceController(context.getApplicationContext());
        }

        if (mForegroundChecker == null) {
            mForegroundChecker = new ForegroundChecker(context);
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(
                    geofencingEvent.getErrorCode());
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
        Log.d("Geofence", "GeofenceEventBroadcastReceiver::sendEvent::isAppInForeground: " + mForegroundChecker.isAppInForeground());
        if (mForegroundChecker.isAppInForeground()) {
            emitRNEvent(context, bundle);
        } else {
            runHeadlessJsTask(context, bundle);
        }
    }

    private void emitRNEvent(Context context, PersistableBundle bundle) {
        ReactNativeHost reactNativeHost = ((ReactApplication) context.getApplicationContext()).getReactNativeHost();
        ReactInstanceManager reactInstanceManager = reactNativeHost.getReactInstanceManager();
        //noinspection ConstantConditions
        Log.d("Geofence", "GeofenceEventBroadcastReceiver::emitRNEvent: " + bundle.toString());
        reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(GEOFENCE_EVENT_NAME, mRnMapper.fromBundle(new Bundle(bundle)));
    }

    private void runHeadlessJsTask(Context context, PersistableBundle bundle) {
        //GeofenceHeadlessJsTaskService.start(context, bundle);
        //GeofenceEventWorker.enqueueGeoEvent(context, bundle);
        Log.d("Geofence", "GeofenceEventBroadcastReceiver::runHeadlessJsTask: " + bundle.toString());
        GeofenceEventWorker.enqueueGeoEvent(context, bundle);
    }
}
