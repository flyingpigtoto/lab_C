package com.example.lab_c.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lab_c.R
import com.example.lab_c.models.HRDataPoint
import com.example.lab_c.models.MyRun
import com.example.lab_c.models.SpeedDataPoint
import com.example.lab_c.storage.CSVRunManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ResultsActivity : AppCompatActivity() {

    private lateinit var tvSummary: TextView
    private lateinit var chartHr: LineChart
    private lateinit var chartSpeed: LineChart
    private lateinit var btnBackMenu: Button

    private var runId: String? = null
    private var runData: MyRun? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        tvSummary   = findViewById(R.id.tvSummary)
        chartHr     = findViewById(R.id.chartHr)
        chartSpeed  = findViewById(R.id.chartSpeed)
        btnBackMenu = findViewById(R.id.btnBackMenu)

        // Grab RUN_ID if passed from the Service or SavedRuns
        runId = intent.getStringExtra("RUN_ID")

        if (runId == null) {
            tvSummary.text = "No run ID provided."
            btnBackMenu.text = "Back to Menu"
        } else {
            loadRunData(runId!!)
        }

        btnBackMenu.setOnClickListener {
            // If you want to go back to a menu or a list of runs
            val intent = Intent(this, SavedRunsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadRunData(id: String) {
        runData = CSVRunManager.getRunById(this, id)
        runData?.let { displayRunData(it) }
            ?: run {
                tvSummary.text = "Run data not found."
            }
    }

    private fun displayRunData(run: MyRun) {
        val summaryText = "Workout Complete!\nDistance: %.2f m\nDuration: %s".format(
            run.distance,
            formatDuration(run.duration)
        )
        tvSummary.text = summaryText

        styleChart(chartHr, "Heart Rate (BPM)")
        styleChart(chartSpeed, "Speed (km/h)")

        populateHrChart(run.hrData)
        populateSpeedChart(run.speedData)
    }

    private fun styleChart(chart: LineChart, label: String) {
        chart.description.isEnabled = false

        // Enable gestures
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // X Axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = android.graphics.Color.DKGRAY
        xAxis.textSize = 12f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        // Y Axis
        val leftAxis = chart.axisLeft
        leftAxis.textColor = android.graphics.Color.DKGRAY
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = 0f  // start at zero

        chart.axisRight.isEnabled = false

        // Legend
        val legend = chart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.textColor = android.graphics.Color.BLACK
        legend.textSize = 12f
    }

    private fun populateHrChart(hrList: List<HRDataPoint>) {
        if (hrList.isEmpty()) {
            chartHr.clear()
            chartHr.invalidate()
            return
        }

        val entries = mutableListOf<Entry>()
        hrList.forEachIndexed { index, dp ->
            // X = index, Y = BPM
            entries.add(Entry(index.toFloat(), dp.bpm.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Heart Rate (BPM)")
        dataSet.color = android.graphics.Color.RED
        dataSet.setCircleColor(android.graphics.Color.RED)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = android.graphics.Color.BLACK

        // **Remove the data values** on each point
        dataSet.setDrawValues(false)

        // **Enable cubic lines** for a rounded look
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f  // tweak for more/less rounding

        // Optionally remove the circles entirely:
        // dataSet.setDrawCircles(false)

        // If you still want a fill under the line:
        dataSet.setDrawFilled(true)
        dataSet.fillColor = android.graphics.Color.parseColor("#FFFFCDD2") // Light red fill

        val lineData = LineData(dataSet)
        chartHr.data = lineData
        chartHr.invalidate()
    }

    private fun populateSpeedChart(speedList: List<SpeedDataPoint>) {
        if (speedList.isEmpty()) {
            chartSpeed.clear()
            chartSpeed.invalidate()
            return
        }

        val entries = mutableListOf<Entry>()
        speedList.forEachIndexed { index, dp ->
            val kmh = dp.speed * 3.6f
            entries.add(Entry(index.toFloat(), kmh))
        }

        val dataSet = LineDataSet(entries, "Speed (km/h)")
        dataSet.color = android.graphics.Color.BLUE
        dataSet.setCircleColor(android.graphics.Color.BLUE)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = android.graphics.Color.BLACK

        // **Remove the data values** on each point
        dataSet.setDrawValues(false)

        // **Rounded lines**
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f

        // Optionally remove circles:
        // dataSet.setDrawCircles(false)

        // Keep fill if you like
        dataSet.setDrawFilled(true)
        dataSet.fillColor = android.graphics.Color.parseColor("#FFBBDEFB") // Light blue fill

        val lineData = LineData(dataSet)
        chartSpeed.data = lineData
        chartSpeed.invalidate()
    }

    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }
}
