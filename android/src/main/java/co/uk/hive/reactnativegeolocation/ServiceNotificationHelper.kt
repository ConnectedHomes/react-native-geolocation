package co.uk.hive.reactnativegeolocation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import uk.co.centrica.hive.reactnativegeolocation.R

internal object ServiceNotificationHelper {
    private const val JOB_ID = 1001
    private const val HIVE_GEO_SERVICE_CHANNEL = "hive_geo_service_channel"

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun initChannel(context: Context) {
        val mNotifyMgr = context.getSystemService(NotificationManager::class.java)
        var mChannel = mNotifyMgr.getNotificationChannel(HIVE_GEO_SERVICE_CHANNEL)
        val notChannelName = "Geolocation"
        if (mChannel == null) {
            mChannel = NotificationChannel(
                HIVE_GEO_SERVICE_CHANNEL,
                notChannelName,
                NotificationManager.IMPORTANCE_LOW
            )
        }

        // Configure the notification channel.
        mChannel.name = notChannelName
        mChannel.setShowBadge(false)
        mChannel.enableLights(false)
        mChannel.enableVibration(false)
        mNotifyMgr.createNotificationChannel(mChannel)
    }

    private fun getForegroundNotification(context: Context): Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(context)
        }
        return NotificationCompat.Builder(context, HIVE_GEO_SERVICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Requesting geolocation data")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    internal fun getForeGroundInfo(context: Context): ForegroundInfo {
        return ForegroundInfo(
                JOB_ID,
                getForegroundNotification(context)
            )
    }
}
