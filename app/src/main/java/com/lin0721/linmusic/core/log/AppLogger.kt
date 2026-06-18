package com.lin0721.linmusic.core.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "AppLogger"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 超过 1MB 自动滚动

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logChannel = Channel<String>(capacity = 1000)

    private var logDir: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        logDir = File(context.cacheDir, "logs").apply {
            if (!exists()) mkdirs()
        }

        // 启动后台单协程，以非阻塞管道形式执行磁盘写入
        logScope.launch {
            for (logLine in logChannel) {
                writeLogToFile(logLine)
            }
        }
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        log("D", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        log("I", tag, msg)
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        Log.w(tag, msg, tr)
        log("W", tag, "$msg\n${tr?.stackTraceToString() ?: ""}")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
        log("E", tag, "$msg\n${tr?.stackTraceToString() ?: ""}")
    }

    private fun log(level: String, tag: String, msg: String) {
        val time = dateFormat.format(Date())
        val formattedMsg = "[$time][$level][$tag] $msg\n"
        logChannel.trySend(formattedMsg)
    }

    private fun writeLogToFile(text: String) {
        val dir = logDir ?: return
        val logFile = File(dir, "app_log_0.txt")

        // 循环重命名的滚动机制，限制最大日志大小
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            val backupFile = File(dir, "app_log_1.txt")
            if (backupFile.exists()) backupFile.delete()
            logFile.renameTo(backupFile)
        }

        try {
            FileWriter(logFile, true).use { writer ->
                writer.write(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log file", e)
        }
    }

    fun getLogFile(): File? {
        val dir = logDir ?: return null
        val logFile = File(dir, "app_log_0.txt")
        return if (logFile.exists()) logFile else null
    }
}
