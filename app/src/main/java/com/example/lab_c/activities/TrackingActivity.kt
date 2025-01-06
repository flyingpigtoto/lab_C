package com.example.lab_c.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lab_c.R
import com.example.lab_c.services.TrackingService
import com.google.android.material.bottomsheet.BottomSheetBehavior

class TrackingActivity : AppCompatActivity() {

    private lateinit var ivPolarStatus: ImageView
    private lateinit var tvDistance: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvTimer: TextView

    // Bottom sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var btnStartPause: Button
    private lateinit var btnReset: Button
    private lateinit var btnStop: Button
    private lateinit var btnFinish: Button
    private lateinit var btnBackMenu: Button

    private var polarAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        ivPolarStatus = findViewById(R.id.ivPolarStatus)
        tvDistance    = findViewById(R.id.tvDistance)
        tvHeartRate   = findViewById(R.id.tvHeartRate)
        tvTimer       = findViewById(R.id.tvTimer)

        val bottomSheet = findViewById<LinearLayout>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        // Start collapsed so only the "header" is visible
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Bottom sheet buttons
        btnStartPause = findViewById(R.id.btnStartPause)
        btnReset      = findViewById(R.id.btnReset)
        btnStop       = findViewById(R.id.btnStop)
        btnFinish     = findViewById(R.id.btnFinish)
        btnBackMenu   = findViewById(R.id.btnBackMenu)

        polarAddress = intent.getStringExtra("POLAR_ADDRESS")

        // Set up button logic
        btnStartPause.setOnClickListener {
            if (TrackingService.running) {
                if (TrackingService.paused) {
                    sendActionToService(TrackingService.ACTION_RESUME)
                    btnStartPause.text = "Pause"
                } else {
                    sendActionToService(TrackingService.ACTION_PAUSE)
                    btnStartPause.text = "Resume"
                }
            } else {
                // Start fresh
                val startIntent = Intent(this, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_START
                    putExtra("POLAR_ADDRESS", polarAddress)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent)
                } else {
                    startService(startIntent)
                }
                btnStartPause.text = "Pause"
            }
            refreshUi()
        }

        btnReset.setOnClickListener {
            if (TrackingService.running) {
                sendActionToService(TrackingService.ACTION_STOP)
                sendActionToService(TrackingService.ACTION_START)
            }
        }

        // Pause (if running) then open the map
        btnStop.setOnClickListener {
            if (TrackingService.running && !TrackingService.paused) {
                sendActionToService(TrackingService.ACTION_PAUSE)
            }
            goToMap()
        }

        // Finish => trigger saving in the service
        btnFinish.setOnClickListener {
            if (TrackingService.running) {
                sendActionToService(TrackingService.ACTION_FINISH)
            }
        }

        btnBackMenu.setOnClickListener {
            finish()
        }

        updateUiLoop()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        btnStartPause.text = when {
            !TrackingService.running -> "Start Service"
            TrackingService.paused   -> "Resume"
            else                     -> "Pause"
        }

        // Update polar icon
        ivPolarStatus.setImageResource(
            if (TrackingService.isPolarConnected) R.drawable.ic_polar_on
            else R.drawable.ic_polar_off
        )
    }

    private fun updateUiLoop() {
        val updater = object : Runnable {
            override fun run() {
                if (TrackingService.running) {
                    tvDistance.text  = "Distance: %.2f m".format(TrackingService.totalDistance)
                    tvHeartRate.text = "HR: ${TrackingService.currentHrBpm} bpm"
                } else {
                    tvDistance.text  = "Distance: (not running)"
                    tvHeartRate.text = "HR: (not running)"
                }
                // Timer from service
                tvTimer.text = formatTime(TrackingService.runSeconds)

                // Polar icon
                ivPolarStatus.setImageResource(
                    if (TrackingService.isPolarConnected) R.drawable.ic_polar_on
                    else R.drawable.ic_polar_off
                )

                refreshUi()
                tvDistance.postDelayed(this, 1000)
            }
        }
        tvDistance.post(updater)
    }

    private fun formatTime(totalSecs: Int): String {
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun goToMap() {
        val latLngList = TrackingService.routePoints.map {
            doubleArrayOf(it.latitude, it.longitude, it.altitude)
        }
        val intent = Intent(this, MapsActivity::class.java).apply {
            putExtra("ROUTE_POINTS", ArrayList(latLngList))
        }
        startActivity(intent)
    }

    private fun sendActionToService(action: String) {
        val intent = Intent(this, TrackingService::class.java).apply {
            this.action = action
            putExtra("POLAR_ADDRESS", polarAddress)
        }
        startService(intent)
    }
}
