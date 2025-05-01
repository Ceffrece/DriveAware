package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.distracted_driver_detection.databinding.ActivityDrivingReportsListBinding

class DrivingReportsList : AppCompatActivity() {
    private lateinit var binding: ActivityDrivingReportsListBinding
    private lateinit var databaseReference: DatabaseReference
    private val driveReportsList = mutableListOf<DriveData>()
    private lateinit var adapter: DriveReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrivingReportsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // Set up RecyclerView
        binding.rvDriveReports.layoutManager = LinearLayoutManager(this)
        adapter = DriveReportsAdapter(driveReportsList) { driveData ->
            // Handle click on report item
            val intent = Intent(this, DriverReports::class.java)
            intent.putExtra("sessionId", driveData.sessionId)
            startActivity(intent)
        }
        binding.rvDriveReports.adapter = adapter

        // Set up Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Load drive reports for current user
        loadUserDriveReports()
    }

    private fun loadUserDriveReports() {
        val currentUserId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("current_user_id", null)

        if (currentUserId == null) {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userDriveSessionsRef = databaseReference
            .child("users")
            .child(currentUserId)
            .child("driveSessions")

        userDriveSessionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driveReportsList.clear()
                for (sessionSnapshot in snapshot.children) {
                    val driveData = sessionSnapshot.getValue(DriveData::class.java)
                    driveData?.let { driveReportsList.add(it) }
                }

                // Sort by date and time using proper date parsing
                driveReportsList.sortWith { a, b ->
                    val dateComparison =
                        compareStringDates(b.date, a.date) // Reverse order for newest first
                    if (dateComparison == 0) {
                        // If same date, compare times
                        compareStringTimes(
                            b.startTime,
                            a.startTime
                        ) // Reverse order for latest first
                    } else {
                        dateComparison
                    }
                }

                adapter.notifyDataSetChanged()

                // Show empty state if no reports
                if (driveReportsList.isEmpty()) {
                    binding.tvNoReports.visibility = View.VISIBLE
                    binding.rvDriveReports.visibility = View.GONE
                } else {
                    binding.tvNoReports.visibility = View.GONE
                    binding.rvDriveReports.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DrivingReportsList,
                    "Error loading reports: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Helper function to compare dates in "YYYY-MM-DD" format
    private fun compareStringDates(date1: String?, date2: String?): Int {
        if (date1 == null && date2 == null) return 0
        if (date1 == null) return -1
        if (date2 == null) return 1

        try {
            val parts1 = date1.split("-").map { it.toInt() }
            val parts2 = date2.split("-").map { it.toInt() }

            // Compare year
            val yearComparison = parts1[0].compareTo(parts2[0])
            if (yearComparison != 0) return yearComparison

            // Compare month
            val monthComparison = parts1[1].compareTo(parts2[1])
            if (monthComparison != 0) return monthComparison

            // Compare day
            return parts1[2].compareTo(parts2[2])
        } catch (e: Exception) {
            return 0
        }
    }

    // Helper function to compare times in "HH:mm" format
    private fun compareStringTimes(time1: String?, time2: String?): Int {
        if (time1 == null && time2 == null) return 0
        if (time1 == null) return -1
        if (time2 == null) return 1

        try {
            val parts1 = time1.split(":").map { it.toInt() }
            val parts2 = time2.split(":").map { it.toInt() }

            // Compare hours
            val hourComparison = parts1[0].compareTo(parts2[0])
            if (hourComparison != 0) return hourComparison

            // Compare minutes
            return parts1[1].compareTo(parts2[1])
        } catch (e: Exception) {
            return 0
        }
    }
}

class DriveReportsAdapter(
    private val driveReports: List<DriveData>,
    private val onItemClick: (DriveData) -> Unit
) : RecyclerView.Adapter<DriveReportsAdapter.DriveReportViewHolder>() {

    class DriveReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.textViewDate)
        val percentageText: TextView = view.findViewById(R.id.textViewPercentage)
        val timeText: TextView = view.findViewById(R.id.textViewTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriveReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drive_report, parent, false)
        return DriveReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriveReportViewHolder, position: Int) {
        val report = driveReports[position]

        // Format date and convert time to 12-hour format
        val startTime = convertTo12HourFormat(report.startTime ?: "")
        holder.dateText.text = "Drive on ${report.date} at $startTime"

        // Format percentage
        holder.percentageText.text = "Distracted: ${report.distractedDrivingPercentage?.toInt()}%"

        // Calculate and format duration
        val duration = report.totalDistractedTime ?: 0
        val minutes = duration / 60
        val seconds = duration % 60
        holder.timeText.text = "Distracted Time: ${minutes}m ${seconds}s"

        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
    }

    override fun getItemCount() = driveReports.size

    private fun convertTo12HourFormat(time24: String): String {
        try {
            val parts = time24.split(":")
            if (parts.size != 2) return time24

            val hour = parts[0].toInt()
            val minute = parts[1]

            return when {
                hour == 0 -> "12:$minute AM"
                hour < 12 -> "$hour:$minute AM"
                hour == 12 -> "12:$minute PM"
                else -> "${hour - 12}:$minute PM"
            }
        } catch (e: Exception) {
            return time24
        }
    }
}