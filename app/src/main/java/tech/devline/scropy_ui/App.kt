package tech.devline.scropy_ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class App : Application() {

    override fun onCreate() {
        app = this
        installCrashHandler()
        installStreamActivityDefaults()
        writeDiag("App.onCreate: started, SDK=${android.os.Build.VERSION.SDK_INT}")
        super.onCreate()
        writeDiag("App.onCreate: finished")
    }

    /**
     * Apply receiver-friendly defaults to every StreamActivity without touching
     * the streaming engine itself: keep the tablet/phone awake and restore
     * immersive fullscreen whenever Android resumes the stream screen.
     */
    private fun installStreamActivityDefaults() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is StreamActivity) applyStreamWindowDefaults(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity is StreamActivity) applyStreamWindowDefaults(activity)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun applyStreamWindowDefaults(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            writeDiag("CRASH on thread ${thread.name}:\n$sw")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private var app: App? = null

        fun writeDiag(msg: String) {
            android.util.Log.i("ScropyApp", msg)
            try {
                val dir = app?.filesDir ?: return
                File(dir, "diag.log").appendText("[${System.currentTimeMillis()}] $msg\n")
            } catch (_: Exception) {}
        }
    }
}
