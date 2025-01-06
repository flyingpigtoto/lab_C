package com.example.lab_c.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.lab_c.R

class MenuActivity : AppCompatActivity() {

    private lateinit var btnScanConnect: Button
    private lateinit var btnStartTracking: Button
    private lateinit var btnViewSavedRuns: Button
    private lateinit var btnQuit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        btnScanConnect = findViewById(R.id.btnScanConnect)
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnViewSavedRuns = findViewById(R.id.btnViewSavedRuns)
        btnQuit = findViewById(R.id.btnQuit)

        btnScanConnect.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }

        btnStartTracking.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            intent.putExtra("POLAR_ADDRESS", "") // Or pass the selected Polar address
            startActivity(intent)
        }

        btnViewSavedRuns.setOnClickListener {
            val intent = Intent(this, SavedRunsActivity::class.java)
            startActivity(intent)
        }

        btnQuit.setOnClickListener {
            finishAffinity()
        }
    }
}
