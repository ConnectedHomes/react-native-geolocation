package co.uk.hive.reactnativegeolocation.geofence;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import androidx.annotation.Nullable;
import android.util.Log;
import co.uk.hive.reactnativegeolocation.RNMapper;
import com.facebook.react.JobHeadlessJsTaskService;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class GeofenceHeadlessJsTaskService extends JobHeadlessJsTaskService {

    private static final int JOB_ID = 434;

    private static final String HEADLESS_TASK_NAME = "GeofenceEventTask";
    private static final String HEADLESS_TASK_ARGUMENT_NAME = "geofence";

    private RNMapper mRnMapper;

    @Override
    public void onCreate() {
        super.onCreate();
        mRnMapper = new RNMapper();
    }

    @Override
    protected @Nullable
    HeadlessJsTaskConfig getTaskConfig(Bundle extras) {
        Log.d(getClass().getSimpleName(), "Running GeofenceHeadlessJsTaskService");
        if (extras != null) {
            return new HeadlessJsTaskConfig(
                    HEADLESS_TASK_NAME,
                    mRnMapper.writeGeofenceTaskParams(HEADLESS_TASK_ARGUMENT_NAME, extras),
                    5000,
                    true
            );
        }
        return null;
    }

    public static void start(Context context, PersistableBundle params) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(buildJobInfo(context, params));
        }
    }

    private static JobInfo buildJobInfo(Context context, PersistableBundle params) {
        return new JobInfo.Builder(JOB_ID, new ComponentName(context, GeofenceHeadlessJsTaskService.class))
                .setExtras(params)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build();
    }
}
