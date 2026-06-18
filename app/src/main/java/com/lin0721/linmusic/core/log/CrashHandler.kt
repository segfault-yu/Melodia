package com.lin0721.linmusic.core.log

import android.content.Context
import android.os.Build
import com.lin0721.linmusic.BuildConfig

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 自动拉取设备环境信息
            val deviceInfo = """
                ================ FATAL CRASH ================
                App Version : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                OS Version  : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Device      : ${Build.BRAND} ${Build.MODEL}
                Manufacturer: ${Build.MANUFACTURER}
                Hardware    : ${Build.HARDWARE}
                Thread Name : ${thread.name}
                =============================================
            """.trimIndent()

            AppLogger.e(TAG, "$deviceInfo\n异常崩溃堆栈信息:", throwable)

            // 稍微挂起一下确保异步 IO 的日志 Channel 落盘完成
            Thread.sleep(800)
        } catch (e: Exception) {
            // 防御性异常处理，避免死循环
        } finally {
            // 回归系统原默认处理方式触发退出，不造成卡死
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
