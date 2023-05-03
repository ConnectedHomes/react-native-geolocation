package co.uk.hive.reactnativegeolocation.work

import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.*
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class GeofenceEventWorker(
    @NonNull context: Context,
    @NonNull params: WorkerParameters
) : HeadlessJsTaskWorker(context, params) {

    companion object {
        private const val HEADLESS_TASK_NAME = "GeofenceEventTask"
        private const val HEADLESS_TASK_ARGUMENT_NAME = "geofence"

        @JvmStatic fun enqueueGeoEvent(mContext: Context, bundle: PersistableBundle) {
            Log.d("Geofence", "GeofenceEventWorker::enqueueGeoEvent: $bundle")
            val dataBuilder = Data.Builder()
            bundle.keySet().forEach {
                dataBuilder.putString(it, bundle.getString(it))
            }
            Log.d("Geofence", "GeofenceEventWorker::enqueueGeoEvent: ${dataBuilder.build()}")
            WorkManager.getInstance(mContext).enqueue(
                OneTimeWorkRequest.Builder(GeofenceEventWorker::class.java)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(dataBuilder.build())
                    .build())
        }
    }
    // TODO: TEST/DEBUG!!!
    override fun getTaskConfig(data: Data?): HeadlessJsTaskConfig? {
        val geoMap = mapOf("name" to HEADLESS_TASK_ARGUMENT_NAME, "params" to data?.keyValueMap)
        Log.d("Geofence", "GeofenceEventWorker::getTaskConfig: $geoMap")
        if (data != null) {
            return HeadlessJsTaskConfig(
                HEADLESS_TASK_NAME,
                Arguments.makeNativeMap(geoMap),
                5000,
                true
            )
        }
        return null
    }
}

