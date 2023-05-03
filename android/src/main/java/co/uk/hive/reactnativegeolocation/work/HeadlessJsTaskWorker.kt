package co.uk.hive.reactnativegeolocation.work

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import co.uk.hive.reactnativegeolocation.util.ServiceNotificationHelper
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager.ReactInstanceEventListener
import com.facebook.react.ReactNativeHost
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.facebook.react.jstasks.HeadlessJsTaskContext
import com.facebook.react.jstasks.HeadlessJsTaskEventListener
import com.google.common.util.concurrent.ListenableFuture

/**
 * Listenable worker class used to communicate with HeadlessJS tasks declared in teh JS app.
 *
 * Required to workaround new restrictions in Android 12+ that prevent apps starting
 * foreground services while the app is in the background.
 *
 * See: https://developer.android.com/guide/components/foreground-services#background-start-restrictions
 *
 * Note: If the app is not running in the background, there may be some delay on enqueued work as
 * the app has to boot up in the background so the HeadlessJS task can be run.
 *
 * Referenced from: https://github.com/wjaykim/react-native-headless-task-worker
 */
// TODO: Consider moving to own module to share around project
abstract class HeadlessJsTaskWorker(
    context: Context,
    params: WorkerParameters) : ListenableWorker(context, params), HeadlessJsTaskEventListener {
    private var taskId = 0
    private var mCompleter: CallbackToFutureAdapter.Completer<Result>? = null
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result>? ->
            val taskConfig = getTaskConfig(this.inputData)
            mCompleter = completer
            if (taskConfig != null) {
                Log.d("Geofence", "HeadlessJsTaskWorker::startWork: $taskConfig")
                startTask(taskConfig)
            } else {
                Log.e("Geofence", "HeadlessJsTaskWorker::startWork: No task config defined!")
                mCompleter!!.set(Result.failure())
            }
        }
    }

    protected abstract fun getTaskConfig(data: Data?): HeadlessJsTaskConfig?

    private fun startTask(taskConfig: HeadlessJsTaskConfig) {
        val reactInstanceManager = reactNativeHost.reactInstanceManager
        val reactContext = reactInstanceManager.currentReactContext
        if (reactContext == null) {
            reactInstanceManager.addReactInstanceEventListener(object : ReactInstanceEventListener {
                override fun onReactContextInitialized(reactContext: ReactContext) {
                    invokeStartTask(reactContext, taskConfig)
                    reactInstanceManager.removeReactInstanceEventListener(this)
                }
            })
            reactInstanceManager.createReactContextInBackground()
        } else {
            invokeStartTask(reactContext, taskConfig)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        val future = SettableFuture.create<ForegroundInfo>()
        future.set(ServiceNotificationHelper.getForeGroundInfo(applicationContext))
        return future
    }

    private fun invokeStartTask(reactContext: ReactContext, taskConfig: HeadlessJsTaskConfig) {
        val headlessJsTaskContext = HeadlessJsTaskContext.getInstance(reactContext)
        headlessJsTaskContext.addTaskEventListener(this)
        Log.d("Geofence", "HeadlessJsTaskWorker::invokeStartTask: $taskConfig")
        UiThreadUtil.runOnUiThread { taskId = headlessJsTaskContext.startTask(taskConfig) }
    }

    private fun cleanUpTask() {
        if (reactNativeHost.hasInstance()) {
            val reactInstanceManager = reactNativeHost.reactInstanceManager
            val reactContext = reactInstanceManager.currentReactContext
            if (reactContext != null) {
                val headlessJsTaskContext = HeadlessJsTaskContext.getInstance(reactContext)
                headlessJsTaskContext.removeTaskEventListener(this)
            }
        }
    }

    override fun onStopped() {
        super.onStopped()
        cleanUpTask()
    }

    override fun onHeadlessJsTaskStart(taskId: Int) {}
    override fun onHeadlessJsTaskFinish(taskId: Int) {
        if (this.taskId == taskId) {
            if (mCompleter != null) {
                mCompleter!!.set(Result.success())
                cleanUpTask()
            }
        }
    }

    private val reactNativeHost: ReactNativeHost
        get() = (this.applicationContext as ReactApplication).reactNativeHost
}
