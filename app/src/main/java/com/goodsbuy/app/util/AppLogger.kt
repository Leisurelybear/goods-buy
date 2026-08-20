package com.goodsbuy.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-wide file logger. When enabled, writes timestamped entries to
 * `filesDir/logs/app.log`. Also mirrors to logcat. Rotates when the
 * file exceeds [MAX_FILE_SIZE] bytes.
 */
object AppLogger {

    private const val TAG = "GoodsBuy"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "app.log"
    private const val CRASH_FILE = "crash.log"
    private const val MAX_FILE_SIZE = 512 * 1024L  // 512 KB

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var context: Context? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context, enabled: Boolean) {
        this.context = context.applicationContext
        this.enabled = enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun isEnabled(): Boolean = enabled

    fun d(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
        write("D", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(TAG, "[$tag] $message")
        write("I", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[$tag] $message", throwable)
        write("W", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$tag] $message", throwable)
        write("E", tag, message, throwable)
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!enabled) return
        val ctx = context ?: return
        try {
            val logDir = File(ctx.filesDir, LOG_DIR)
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, LOG_FILE)

            // Rotate if too large
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                val backup = File(logDir, "$LOG_FILE.bak")
                if (backup.exists()) backup.delete()
                logFile.renameTo(backup)
            }

            val ts = dateFormat.format(Date())
            val sb = StringBuilder()
            sb.append("$ts $level/$tag: $message\n")
            if (throwable != null) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                sb.append(sw.toString())
            }
            logFile.appendText(sb.toString())
        } catch (_: Exception) {
            // Logging must never crash the app
        }
    }

    /** Returns the current log file, or null if logging is disabled. */
    fun getLogFile(): File? {
        val ctx = context ?: return null
        return File(ctx.filesDir, "$LOG_DIR/$LOG_FILE").takeIf { it.exists() }
    }

    /** Returns the crash log file (unconditional), or null if never initialized. */
    fun getCrashLogFile(): File? {
        val ctx = context ?: return null
        return File(ctx.filesDir, "$LOG_DIR/$CRASH_FILE").takeIf { it.exists() }
    }

    /** Writes a crash stack trace to crash.log regardless of the logging toggle. */
    fun logCrash(throwable: Throwable) {
        val ctx = context ?: return
        try {
            val logDir = File(ctx.filesDir, LOG_DIR)
            if (!logDir.exists()) logDir.mkdirs()
            val crashFile = File(logDir, CRASH_FILE)
            val ts = dateFormat.format(Date())
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            crashFile.appendText("$ts CRASH on ${Thread.currentThread().name}:\n${sw}\n")
        } catch (_: Exception) {
            // Logging must never crash the app
        }
    }

    /** Deletes app.log, its backup and crash.log to free space. Logging continues seamlessly. */
    fun deleteLogs() {
        val ctx = context ?: return
        try {
            val logDir = File(ctx.filesDir, LOG_DIR)
            File(logDir, LOG_FILE).delete()
            File(logDir, "$LOG_FILE.bak").delete()
            File(logDir, CRASH_FILE).delete()
            if (logDir.exists() && logDir.listFiles().isNullOrEmpty()) {
                logDir.delete()
            }
        } catch (_: Exception) {
            // Logging must never crash the app
        }
    }
}
