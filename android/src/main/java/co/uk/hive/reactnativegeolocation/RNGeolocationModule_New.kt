package co.uk.hive.reactnativegeolocation

import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Transformations.map
import co.uk.hive.reactnativegeolocation.data.RNMapper
import co.uk.hive.reactnativegeolocation.geofence.GeoFenceController
import co.uk.hive.reactnativegeolocation.geofence.legacy.Geofence
import co.uk.hive.reactnativegeolocation.location.LocationController
import com.annimon.stream.function.Function
import com.facebook.react.bridge.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStatusCodes

class RNGeolocationModule_New(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {
    //    private final GeofenceController mGeofenceController;
    //    private final LocationController mLocationController;
    //    private final RNMapper mRnMapper;
    private val geoFenceController: GeoFenceController
    private val locationController: LocationController
    private val rnMapper: RNMapper

    init {
        reactContext.addActivityEventListener(this)
        geoFenceController = GeoFenceController(reactContext)
        locationController = LocationController(reactContext)
        rnMapper = RNMapper()
        //        mGeofenceController = GeofenceServiceLocator.getGeofenceController(reactContext.getApplicationContext());
//        mLocationController = GeofenceServiceLocator.getLocationController(reactContext.getApplicationContext());
//        mRnMapper = new RNMapper();
    }

    override fun getName() = "RNGeolocationNew"

    @ReactMethod
    fun ready() {
        // TODO: Not sure if manual re-registration required.. as long as they are re-added if previously added
        //mGeofenceController.setupReregistration();
    }

    @ReactMethod
    fun startGeofences(successCallback: Callback?, failureCallback: Callback?) {
        Log.d("Geofence", "RNGeolocationModule::startGeofences!")
        // mGeofenceController.start(convertCallback(successCallback), convertCallback(failureCallback));
//        ReactNativeHost reactNativeHost = ((ReactApplication) this.reactContext.getApplicationContext()).getReactNativeHost();
//        ReactInstanceManager reactInstanceManager = reactNativeHost.getReactInstanceManager();
//        //noinspection ConstantConditions
//        reactInstanceManager.getCurrentReactContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
//                .emit("geofence", mRnMapper.fromBundle(new Bundle()));
    }

    @ReactMethod
    fun stopGeofences(successCallback: Callback?, failureCallback: Callback?) {
        Log.d("Geofence", "RNGeolocationModule::stopGeofences!")
        //mGeofenceController.stop(convertCallback(successCallback), convertCallback(failureCallback));
    }

    @ReactMethod
    fun addGeofences(geofencesArray: ReadableArray) {
        // TODO: Add safety checks / error handling
        val geoFences = mutableListOf<Geofence>()
        for (i in 0 until geofencesArray.size()) {
            geoFences.add(rnMapper.readGeofence(geofencesArray.getMap(i)))
        }
        geoFenceController.addGeofences(geoFences);
    }

    @ReactMethod
    fun removeGeofences() {
        Log.d("Geofence", "RNGeolocationModule::removeGeofences")
        //mGeofenceController.removeAllGeofences();
    }

    @ReactMethod
    fun getCurrentPosition(currentPositionRequest: ReadableMap?,
                           successCallback: Callback?, failureCallback: Callback?) {
//        Function<LatLng, Object> positionCallback = location -> {
//            successCallback.invoke(mRnMapper.writeLocation(location));
//            return null;
//        };
//        Log.d("Geofence", "RNGeolocationModule::getCurrentPosition: " + currentPositionRequest.toString());
//        mLocationController.getCurrentPosition(
//                mRnMapper.readPositionRequest(currentPositionRequest), positionCallback, convertCallback(failureCallback));

        // TODO: Handle RESOLUTION_REQUIRED error status here and try startActivityForResult to see wagwaaan!!
        // TODO: Handle no resolution avialable as well!!! WHHYYYYY GOOOOOGLE!!!!!
    }

    @ReactMethod
    fun isLocationEnabled(promise: Promise?) {
//        final boolean locationEnabled = new LocationServicesChecker(getReactApplicationContext())
//                .isLocationEnabled();
//        Log.d("Geofence", "RNGeolocationModule::locationEnabled: " + locationEnabled);
//        promise.resolve(locationEnabled);
    }

    private fun <T> convertCallback(callback: Callback): Function<T, Any?> {
        return Function { t: T ->
            if (t is ApiException) {
                val apiException = t as ApiException
                when (apiException.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {

                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        // TODO: Callback to module somehow... broadcast? Start activity for result from there and use activitylistener!
                        Log.e("Geofence", "RNGeolocationModule::RESOLUTION_REQUIRED")
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = apiException as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                    currentActivity!!,
                                    REQUEST_CHECK_SETTINGS)
                        } catch (e: SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {

                        // TODO: Test cancelling or check which devices/os don't have the accuracy thingy
                        Log.e("Geofence", "RNGeolocationModule::SETTINGS_CHANGE_UNAVAILABLE")
                        Toast.makeText(reactApplicationContext, "SETTINGS_CHANGE_UNAVAILABLE!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {

                        // TODO: The result of the above cases needed passing back to app
                        val errorMap = Arguments.createMap()
                        errorMap.putString("exceptionType", t.javaClass.toString())
                        errorMap.putString("message", apiException.message)
                        errorMap.putString("code", apiException.statusCode.toString())
                        errorMap.putString("JIRA", "MA1-1200")
                        callback.invoke(errorMap)
                    }
                }
            } else if (t is Exception) {
                val exception = t as Exception
                val errorMap = Arguments.createMap()
                errorMap.putString("exceptionType", t.javaClass.toString())
                errorMap.putString("message", exception.message)
                errorMap.putString("JIRA", "MA1-1200")
                callback.invoke(errorMap)
            } else {
                callback.invoke(t)
            }
            null
        }
    }

    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        // TODO: Consider a permission check here first!!
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK ->                     // All required changes were successfully made.. TODO: How to proceed?
                    Toast.makeText(reactApplicationContext, "User enabled additional setting!", Toast.LENGTH_SHORT).show()
                Activity.RESULT_CANCELED ->                     // The user was asked to change settings, but chose not to TODO: How to handle cancellation?
                    Toast.makeText(reactApplicationContext, "User cancelled location enabled!", Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        // NOOP
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 10008
    }

}