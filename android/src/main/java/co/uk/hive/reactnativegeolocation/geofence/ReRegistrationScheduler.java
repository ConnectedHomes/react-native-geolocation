package co.uk.hive.reactnativegeolocation.geofence;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import android.util.Log;


public class ReRegistrationScheduler {

    private static final int JOB_ID = 529;

    private final Context mContext;

    public ReRegistrationScheduler(Context context) {
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void scheduleReRegistration() {
        JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, getComponentName())
                    .addTriggerContentUri(getUri())
                    .build();
            int result = jobScheduler.schedule(jobInfo);
            if (result != JobScheduler.RESULT_SUCCESS) {
                Log.e(getClass().getSimpleName(), "Job scheduling failed");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private JobInfo.TriggerContentUri getUri() {
        Uri uri = Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return new JobInfo.TriggerContentUri(uri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS);
    }

    private ComponentName getComponentName() {
        return new ComponentName(mContext, ReRegisterGeofencesOreoJobService.class);
    }
}
