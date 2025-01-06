@file:Suppress("MissingPermission")
package com.example.lab_c.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.lab_c.R
import com.example.lab_c.activities.ResultsActivity
import com.example.lab_c.activities.TrackingActivity
import com.example.lab_c.ble.PolarHrManager
import com.example.lab_c.models.HRDataPoint
import com.example.lab_c.models.MyRun
import com.example.lab_c.models.SpeedDataPoint
import com.example.lab_c.storage.CSVRunManager
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.Locale

class TrackingService : Service() {

    companion object {
        private const val TAG = "TrackingService"
        private const val CHANNEL_ID = "TrackingServiceChannel"
        private const val NOTIF_ID = 999

        // Actions
        const val ACTION_START = "com.example.lab_c.ACTION_START"
        const val ACTION_PAUSE = "com.example.lab_c.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.lab_c.ACTION_RESUME"
        const val ACTION_STOP = "com.example.lab_c.ACTION_STOP"
        const val ACTION_FINISH = "com.example.lab_c.ACTION_FINISH"

        // Data
        val routePoints = mutableListOf<Location>()
        var totalDistance = 0f
        var lastLocation: Location? = null

        // HR + speed data for charts
        val speedData = mutableListOf<SpeedDataPoint>()
        val hrData = mutableListOf<HRDataPoint>()
        var currentHrBpm = 0
        var isPolarConnected = false

        // Polar
        var polarAddress: String? = null
        var polarManager: PolarHrManager? = null

        // Run state
        var running = false
        var paused = false

        // CSV ID + date
        var runId: String? = null
        var startDateTime: String = ""

        // Timer stored in the service
        // This is the total elapsed seconds for the current run
        var runSeconds = 0
    }

    // Timer Handler/Thread
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (running && !paused) {
                runSeconds++
            }
            // Re-run after 1 second
            timerHandler.postDelayed(this, 1000)
        }
    }

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate => startForeground")

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Tracking started", "Collecting location+HR in background"))

        // Start the timer loop
        timerHandler.post(timerRunnable)

        setupLocation()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        polarAddress = intent?.getStringExtra("POLAR_ADDRESS") ?: polarAddress

        when (action) {
            ACTION_START -> {
                startNewRun()
                maybeConnectPolar()
            }
            ACTION_PAUSE -> {
                if (running && !paused) {
                    paused = true
                    stopLocationUpdates()
                    polarManager?.disconnect()
                    updateNotification()
                }
            }
            ACTION_RESUME -> {
                if (running && paused) {
                    paused = false
                    startLocationUpdates()
                    maybeConnectPolar()
                    updateNotification()
                }
            }
            ACTION_STOP -> {
                // Stop tracking but do NOT save
                stopTrackingCompletely()
            }
            ACTION_FINISH -> {
                // Save + Stop
                saveRun()
                // Jump directly to ResultsActivity with the run ID
                val resultsIntent = Intent(this, ResultsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("RUN_ID", runId)
                }
                startActivity(resultsIntent)

                stopTrackingCompletely()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed => stopping tracking if still running.")
        stopTrackingCompletely()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, text: String): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TrackingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_distance)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "Tracking Service", NotificationManager.IMPORTANCE_LOW)
            chan.description = "Keeps location & HR tracking in background"
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(chan)
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val n = buildNotification(
            "Tracking ongoing",
            "HR: $currentHrBpm bpm, Dist: ${"%.1f".format(totalDistance)} m"
        )
        nm.notify(NOTIF_ID, n)
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                if (paused) return
                for (loc in res.locations) {
                    lastLocation?.let {
                        totalDistance += it.distanceTo(loc)
                    }
                    speedData.add(SpeedDataPoint(System.currentTimeMillis(), loc.speed))
                    lastLocation = loc
                    routePoints.add(loc)
                    updateNotification()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permissions => can't start location updates.")
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun maybeConnectPolar() {
        if (!polarAddress.isNullOrEmpty()) {
            polarManager = PolarHrManager(
                context = this,
                onHrValue = { bpm ->
                    if (!paused) {
                        currentHrBpm = bpm
                        hrData.add(HRDataPoint(System.currentTimeMillis(), bpm))
                        updateNotification()
                    }
                },
                onConnectionChanged = { connected ->
                    isPolarConnected = connected
                    updateNotification()
                }
            )
            connectPolarIfPermitted()
        }
    }

    private fun connectPolarIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val c = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            if (c != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No BLUETOOTH_CONNECT permission => can't connect Polar.")
                return
            }
        }
        polarAddress?.let { polarManager?.connect(it) }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return !(fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED)
    }

    private fun startNewRun() {
        running = true
        paused = false

        // Reset data
        routePoints.clear()
        speedData.clear()
        hrData.clear()
        totalDistance = 0f
        lastLocation = null
        runSeconds = 0

        // Generate new ID
        runId = CSVRunManager.generateRunId()
        startDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
    }

    private fun stopTrackingCompletely() {
        if (!running) return
        running = false
        paused = false

        stopLocationUpdates()
        polarManager?.disconnect()
        polarManager = null

        stopForeground(true)
        stopSelf()

        // Stop the timer
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun saveRun() {
        // Always save, even if there's no route
        val myRun = MyRun(
            id = runId ?: CSVRunManager.generateRunId(),
            dateTime = startDateTime,
            distance = totalDistance,
            duration = runSeconds.toLong(),
            hrData = hrData.toList(),
            speedData = speedData.toList()
        )
        CSVRunManager.saveRun(this, myRun)
        Log.d(TAG, "Run saved => ID=${myRun.id}, distance=${myRun.distance}, hrCount=${myRun.hrData.size}")
    }
}
