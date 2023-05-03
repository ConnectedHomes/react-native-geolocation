package co.uk.hive.reactnativegeolocation.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ForegroundChecker {

    private final Context mContext;

    public ForegroundChecker(Context context) {
        mContext = context;
    }

    public boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = mContext.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

}
