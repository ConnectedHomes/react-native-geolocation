package co.uk.hive.reactnativegeolocation.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import co.uk.hive.reactnativegeolocation.geofence.GeoFenceController;

public class GeofenceRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "GeoRestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (!GeoLocationUtil.isGpsEnabled(context) || !GeoLocationUtil.hasGpsPermissions(context))
//            return;
//
//        GeofenceSettings settings = new GeofenceSettings(context);
//
//        if (!settings.isGeofencingActive()) {
//            ToastLog.logLong(context, TAG, "No provider active.");
//            return;
//        }
//
//        ToastLog.logLong(context, TAG, "Re-registering provider...");
//
//        Location homeLocation = settings.getHomeLocation();
//        double enterRadius = settings.getEnterRadius();
//        double exitRadius = settings.getExitRadius();
//        String activeGeofenceProvider = settings.getGeofenceProvider();
//        boolean initTrigger = settings.isInitialTriggerEnabled();
//        boolean usePolling = settings.isGpsPollingEnabled();
//
//        if (homeLocation != null && activeGeofenceProvider != null) {
//            GeofenceProvider geofenceProvider = null;
//            if (activeGeofenceProvider.equals("Play")) {
//                geofenceProvider = new PlayGeofenceProvider(context);
//            } else if (activeGeofenceProvider.equals("PathSense")) {
//                geofenceProvider = new PathSenseGeofenceProvider(context);
//            }
//
//            if (geofenceProvider != null) {
//                geofenceProvider.start(homeLocation, enterRadius, exitRadius,
//                        initTrigger, usePolling);
//            }
//        }

        // TODO: Check permissions
        // TODO: Check gps enabled
        // TODO: Check repo (was previously active)
        // TODO: Re-add the geofences and start monitoring!!

        final GeoFenceController geoFenceController = new GeoFenceController(context);
        geoFenceController.addGeofences(new ArrayList<>());
        geoFenceController.start(success -> null, failure -> null);
    }
}
