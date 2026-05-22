package com.eggzys.internetmonitor

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class MonitorService : Service() {

    companion object {
        const val TAG = "MonitorService"
        const val PREFS_NAME = "monitor_prefs"
        const val KEY_LAST_STATE = "last_state"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_LAST_CHECK_TIME = "last_check_time"
        const val KEY_GLOBAL_URLS = "global_urls"
        const val KEY_RUSSIA_URLS = "russia_urls"
        const val KEY_WHITELIST_URLS = "whitelist_urls"
        const val CHECK_INTERVAL_MS = 90_000L
        const val ACTION_STATE_CHANGED = "com.eggzys.internetmonitor.STATE_CHANGED"

        fun start(context: Context) {
            Log.d(TAG, "Starting monitor service")
            val intent = Intent(context, MonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping monitor service")
            val intent = Intent(context, MonitorService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var checker: InternetStateChecker
    private var lastState: InternetState = InternetState.UNKNOWN

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        notificationHelper = NotificationHelper(this)
        checker = InternetStateChecker()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedStateName = prefs.getString(KEY_LAST_STATE, InternetState.UNKNOWN.name)
        lastState = try {
            InternetState.valueOf(savedStateName!!)
        } catch (_: Exception) {
            InternetState.UNKNOWN
        }

        val notification = notificationHelper.buildServiceNotification(lastState)
        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
        Log.d(TAG, "Foreground service started with notification")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_RUNNING, true).apply()

        if (monitorJob?.isActive != true) {
            monitorJob = serviceScope.launch {
                Log.d(TAG, "Starting monitoring loop")
                // Run first check immediately
                runCheck()
                // Then continue on interval
                while (isActive) {
                    delay(CHECK_INTERVAL_MS)
                    runCheck()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        monitorJob?.cancel()
        serviceScope.cancel()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_RUNNING, false).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun runCheck() {
        try {
            Log.d(TAG, "Running network check...")
            val urlGroups = loadUrlGroups()
            val newState = checker.checkState(urlGroups)
            Log.d(TAG, "Check result: ${newState.displayName}")

            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                .apply()

            if (newState != lastState) {
                Log.d(TAG, "State changed: $lastState -> $newState")
                lastState = newState
                prefs.edit()
                    .putString(KEY_LAST_STATE, newState.name)
                    .apply()
                notificationHelper.sendStateAlert(newState)
            }

            // Always update persistent notification
            updatePersistentNotification()

            sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))

        } catch (e: Exception) {
            Log.e(TAG, "Check error", e)
            updatePersistentNotification()
        }
    }

    private fun updatePersistentNotification() {
        try {
            val notification = notificationHelper.buildServiceNotification(lastState)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
            Log.d(TAG, "Notification updated: ${lastState.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    internal fun loadUrlGroups(): UrlGroups {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val defaults = UrlGroups.defaults()

        val global = prefs.getString(KEY_GLOBAL_URLS, null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: defaults.globalUrls

        val russia = prefs.getString(KEY_RUSSIA_URLS, null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: defaults.russiaUrls

        val whitelist = prefs.getString(KEY_WHITELIST_URLS, null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: defaults.whitelistUrls

        return UrlGroups(global, russia, whitelist)
    }

    fun getCurrentState(): InternetState = lastState
}
