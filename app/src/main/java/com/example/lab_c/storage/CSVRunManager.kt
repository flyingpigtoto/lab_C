package com.example.lab_c.storage

import android.content.Context
import com.example.lab_c.models.HRDataPoint
import com.example.lab_c.models.MyRun
import com.example.lab_c.models.SpeedDataPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object CSVRunManager {

    private const val RUNS_FOLDER = "runs"   // Subfolder for run CSV files

    /**
     * Saves a single run to internal storage as a CSV file:
     *   - File name: "run_<id>.csv"
     *   - Contains:
     *       line 1: dateTime
     *       line 2: distance
     *       line 3: duration
     *       line 4+: "HR,<timestamp>,<bpm>" for heart rate
     *       then     "SPD,<timestamp>,<speed>" for speed data
     */
    fun saveRun(context: Context, myRun: MyRun) {
        val runsDir = File(context.filesDir, RUNS_FOLDER)
        if (!runsDir.exists()) runsDir.mkdir()

        val fileName = "run_${myRun.id}.csv"
        val runFile = File(runsDir, fileName)

        runFile.bufferedWriter().use { out ->
            // Write basic metadata
            out.write(myRun.dateTime); out.newLine()
            out.write(myRun.distance.toString()); out.newLine()
            out.write(myRun.duration.toString()); out.newLine()

            // Write HR rows
            myRun.hrData.forEach { hr ->
                out.write("HR,${hr.timestamp},${hr.bpm}")
                out.newLine()
            }

            // Write speed rows
            myRun.speedData.forEach { spd ->
                out.write("SPD,${spd.timestamp},${spd.speed}")
                out.newLine()
            }
        }
    }

    /**
     * Returns a list of "lightweight" MyRun objects for display in SavedRunsActivity,
     * but WITHOUT the HR / speed arrays (we won't parse them here).
     * We'll parse only lines 1-3: dateTime, distance, duration.
     * ID is derived from the file name => "run_<id>.csv"
     */
    fun listRuns(context: Context): List<MyRun> {
        val runsDir = File(context.filesDir, RUNS_FOLDER)
        if (!runsDir.exists()) return emptyList()

        val runFiles = runsDir.listFiles()?.filter { it.name.startsWith("run_") && it.name.endsWith(".csv") }
            ?: emptyList()

        val list = mutableListOf<MyRun>()
        for (f in runFiles) {
            val lines = f.readLines()
            if (lines.size < 3) {
                // not enough info
                continue
            }
            val dateTime = lines[0]
            val distance = lines[1].toFloatOrNull() ?: 0f
            val duration = lines[2].toLongOrNull() ?: 0L
            val id = f.name.removePrefix("run_").removeSuffix(".csv")

            // We won't parse HR/speed in this method => pass empty lists
            val fakeHr = emptyList<HRDataPoint>()
            val fakeSpd = emptyList<SpeedDataPoint>()
            list.add(MyRun(id, dateTime, distance, duration, fakeHr, fakeSpd))
        }

        // Sort descending by dateTime (if you like):
        // (We can parse dateTime to compare or just do a custom approach)
        // For simplicity, let's do a naive sort by dateTime string:
        return list.sortedByDescending { it.dateTime }
    }

    /**
     * Reads the entire CSV file for a single run, returning the full MyRun object (with HR & speed).
     */
    fun getRunById(context: Context, runId: String): MyRun? {
        val runsDir = File(context.filesDir, RUNS_FOLDER)
        if (!runsDir.exists()) return null

        val runFile = File(runsDir, "run_$runId.csv")
        if (!runFile.exists()) return null

        val lines = runFile.readLines()
        if (lines.size < 3) return null

        val dateTime = lines[0]
        val distance = lines[1].toFloatOrNull() ?: 0f
        val duration = lines[2].toLongOrNull() ?: 0L

        val hrList = mutableListOf<HRDataPoint>()
        val speedList = mutableListOf<SpeedDataPoint>()
        // lines from index 3 onwards are "HR,timestamp,bpm" or "SPD,timestamp,speed"
        for (i in 3 until lines.size) {
            val row = lines[i]
            val parts = row.split(",")
            if (parts.size != 3) continue
            val type = parts[0]
            val ts = parts[1].toLongOrNull() ?: continue
            val val3 = parts[2]

            when (type) {
                "HR" -> {
                    val bpm = val3.toIntOrNull() ?: 0
                    hrList.add(HRDataPoint(ts, bpm))
                }
                "SPD" -> {
                    val spd = val3.toFloatOrNull() ?: 0f
                    speedList.add(SpeedDataPoint(ts, spd))
                }
            }
        }

        return MyRun(runId, dateTime, distance, duration, hrList, speedList)
    }

    /**
     * Returns a new unique ID based on date/time or simple counter
     */
    fun generateRunId(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(System.currentTimeMillis())  // e.g. "20250106_153045"
    }

    /**
     * Optional: we could also delete runs if you want that feature
     */
    fun deleteRun(context: Context, runId: String) {
        val runsDir = File(context.filesDir, RUNS_FOLDER)
        val runFile = File(runsDir, "run_$runId.csv")
        if (runFile.exists()) runFile.delete()
    }
}
