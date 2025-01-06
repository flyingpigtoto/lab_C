package com.example.lab_c.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_c.R
import com.example.lab_c.models.MyRun
import com.example.lab_c.storage.CSVRunManager
import java.text.SimpleDateFormat
import java.util.Locale

class SavedRunsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RunsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_runs)

        recyclerView = findViewById(R.id.recyclerViewRuns)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RunsAdapter { myRun ->
            // OnClick: Open ResultsActivity with run data
            val intent = Intent(this, ResultsActivity::class.java)
            // pass run ID
            intent.putExtra("RUN_ID", myRun.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadRuns()
    }

    private fun loadRuns() {
        val runsList = CSVRunManager.listRuns(this)
        adapter.submitList(runsList)
    }

    // Adapter Class
    class RunsAdapter(private val onItemClick: (MyRun) -> Unit) :
        RecyclerView.Adapter<RunsAdapter.RunViewHolder>() {

        private var runsList: List<MyRun> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false)
            return RunViewHolder(view)
        }

        override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
            val run = runsList[position]
            holder.bind(run, onItemClick)
        }

        override fun getItemCount(): Int = runsList.size

        fun submitList(runs: List<MyRun>) {
            runsList = runs
            notifyDataSetChanged()
        }

        class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDateTime: TextView = itemView.findViewById(R.id.tvRunDateTime)
            private val tvDistance: TextView = itemView.findViewById(R.id.tvRunDistance)
            private val tvDuration: TextView = itemView.findViewById(R.id.tvRunDuration)

            fun bind(run: MyRun, onItemClick: (MyRun) -> Unit) {
                tvDateTime.text = run.dateTime
                tvDistance.text = "Distance: %.2f m".format(run.distance)
                val durationStr = formatDuration(run.duration)
                tvDuration.text = "Duration: $durationStr"

                itemView.setOnClickListener {
                    onItemClick(run)
                }
            }

            private fun formatDuration(seconds: Long): String {
                val hrs = seconds / 3600
                val mins = (seconds % 3600) / 60
                val secs = seconds % 60
                return String.format("%02d:%02d:%02d", hrs, mins, secs)
            }
        }
    }
}
