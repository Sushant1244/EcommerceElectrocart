package com.example.e_commerceelectrocart

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * The main Application class, annotated for Hilt dependency injection.
 * Adds a global uncaught exception handler that writes a crash log file
 * into the app's files directory and logs the stacktrace to Logcat.
 */
@HiltAndroidApp
class ElectrocartApplication : Application() {
	companion object {
		private const val TAG = "ElectrocartApplication"
		private const val CRASH_DIR = "crash_logs"
	}

	override fun onCreate() {
		super.onCreate()

		// Install a global uncaught exception handler to capture crashes
		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			try {
				val sw = StringWriter()
				val pw = PrintWriter(sw)
				throwable.printStackTrace(pw)
				val stackTrace = sw.toString()

				Log.e(TAG, "Uncaught exception in thread ${thread.name}: $stackTrace")

				// Ensure crash directory exists
				val dir = File(filesDir, CRASH_DIR)
				if (!dir.exists()) dir.mkdirs()

				val crashFile = File(dir, "crash_${System.currentTimeMillis()}.log")
				crashFile.writeText("Thread: ${thread.name}\n\n$stackTrace")
			} catch (e: Exception) {
				// If logging fails, at least log to Logcat
				Log.e(TAG, "Failed to write crash log: ${e.message}")
			}

			// Re-throw to let the system handle the crash as well
			val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
			defaultHandler?.uncaughtException(thread, throwable)
		}
	}
}
