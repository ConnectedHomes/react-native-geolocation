package com.facebook.react;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.jstasks.HeadlessJsTaskContext;
import com.facebook.react.jstasks.HeadlessJsTaskEventListener;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Base class for running JS without a UI. Generally, you only need to override
 * {@link #getTaskConfig}, which is called for every {@link #onStartCommand}. The
 * result, if not {@code null}, is used to run a JS task.
 *
 */
public abstract class JobHeadlessJsTaskService extends JobService implements HeadlessJsTaskEventListener {

    private final Set<Integer> mActiveTasks = new CopyOnWriteArraySet<>();
    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mJobParameters = jobParameters;
        Bundle jobParams = new Bundle();
        jobParams.putAll(jobParameters.getExtras());
        HeadlessJsTaskConfig taskConfig = getTaskConfig(jobParams);
        if (taskConfig != null) {
            startTask(taskConfig);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    /**
     * Called from {@link #onStartCommand} to create a {@link HeadlessJsTaskConfig} for this intent.
     * @return a {@link HeadlessJsTaskConfig} to be used with {@link #startTask}, or
     *         {@code null} to ignore this command.
     * @param extras
     */
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Bundle extras) {
        return null;
    }

    /**
     * Start a task. This method handles starting a new React instance if required.
     *
     * Has to be called on the UI thread.
     *
     * @param taskConfig describes what task to start and the parameters to pass to it
     */
    protected void startTask(final HeadlessJsTaskConfig taskConfig) {
        UiThreadUtil.assertOnUiThread();
        final ReactInstanceManager reactInstanceManager =
                getReactNativeHost().getReactInstanceManager();
        ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
        if (reactContext == null) {
            reactInstanceManager
                    .addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        @Override
                        public void onReactContextInitialized(ReactContext reactContext) {
                            invokeStartTask(reactContext, taskConfig);
                            reactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });
            if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
                reactInstanceManager.createReactContextInBackground();
            }
        } else {
            invokeStartTask(reactContext, taskConfig);
        }
    }

    private void invokeStartTask(ReactContext reactContext, final HeadlessJsTaskConfig taskConfig) {
        final HeadlessJsTaskContext headlessJsTaskContext = HeadlessJsTaskContext.getInstance(reactContext);
        headlessJsTaskContext.addTaskEventListener(this);

        UiThreadUtil.runOnUiThread(
                () -> {
                    int taskId = headlessJsTaskContext.startTask(taskConfig);
                    mActiveTasks.add(taskId);
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getReactNativeHost().hasInstance()) {
            ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
            ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
            if (reactContext != null) {
                HeadlessJsTaskContext headlessJsTaskContext =
                        HeadlessJsTaskContext.getInstance(reactContext);
                headlessJsTaskContext.removeTaskEventListener(this);
            }
        }
    }

    @Override
    public void onHeadlessJsTaskStart(int taskId) { }

    @Override
    public void onHeadlessJsTaskFinish(int taskId) {
        mActiveTasks.remove(taskId);
        if (mActiveTasks.size() == 0) {
            jobFinished(mJobParameters, false);
        }
    }

    /**
     * Get the {@link ReactNativeHost} used by this app. By default, assumes {@link #getApplication()}
     * is an instance of {@link ReactApplication} and calls
     * {@link ReactApplication#getReactNativeHost()}. Override this method if your application class
     * does not implement {@code ReactApplication} or you simply have a different mechanism for
     * storing a {@code ReactNativeHost}, e.g. as a static field somewhere.
     */
    protected ReactNativeHost getReactNativeHost() {
        return ((ReactApplication) getApplication()).getReactNativeHost();
    }
}
