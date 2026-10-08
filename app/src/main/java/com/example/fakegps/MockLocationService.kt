package com.example.fakegps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MockLocationService : Service() {

    private lateinit var handlerThread: HandlerThread
    private var handler: Handler? = null
    private var lat = 0.0
    private var lng = 0.0

    private val pushRunnable = object : Runnable {
        override fun run() {
            pushLocation()
            handler?.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread("MockLocationThread").also { it.start() }
        handler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        lat = intent?.getDoubleExtra(EXTRA_LAT, 0.0) ?: 0.0
        lng = intent?.getDoubleExtra(EXTRA_LNG, 0.0) ?: 0.0

        startAsForeground()

        if (tryStartMocking()) {
            isRunning = true
            handler?.post(pushRunnable)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_text, lat, lng))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun tryStartMocking(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            addProvider(lm, LocationManager.GPS_PROVIDER, highPower = true)
            addProvider(lm, LocationManager.NETWORK_PROVIDER, highPower = false)
            true
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.mock_not_selected, Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.mock_error, e.message), Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun addProvider(lm: LocationManager, name: String, highPower: Boolean) {
        try { lm.removeTestProvider(name) } catch (_: Exception) { }
        lm.addTestProvider(
            name,
            false, false, false, false, true, true, true,
            if (highPower) LocationManager.POWER_USAGE_HIGH else LocationManager.POWER_USAGE_LOW,
            if (highPower) LocationManager.ACCURACY_FINE else LocationManager.ACCURACY_COARSE
        )
    }

    private fun pushLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtimeNanos()
        try {
            lm.setTestProviderLocation(
                LocationManager.GPS_PROVIDER,
                makeLocation(LocationManager.GPS_PROVIDER, lat, lng, 1.0f, now, elapsed)
            )
        } catch (_: Exception) { }
        try {
            lm.setTestProviderLocation(
                LocationManager.NETWORK_PROVIDER,
                makeLocation(LocationManager.NETWORK_PROVIDER, lat, lng, 30.0f, now, elapsed)
            )
        } catch (_: Exception) { }
    }

    private fun makeLocation(
        provider: String, lat: Double, lng: Double, accuracy: Float, time: Long, elapsedNanos: Long
    ): Location = Location(provider).apply {
        latitude = lat
        longitude = lng
        altitude = 0.0
        this.accuracy = accuracy
        bearing = 0f
        speed = 0f
        this.time = time
        elapsedRealtimeNanos = elapsedNanos
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        if (::handlerThread.isInitialized) handlerThread.quitSafely()
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
            lm.removeTestProvider(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) { }
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.fakegps.action.STOP"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fake_gps_channel"
        private const val UPDATE_INTERVAL_MS = 1000L

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, lat: Double, lng: Double) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}