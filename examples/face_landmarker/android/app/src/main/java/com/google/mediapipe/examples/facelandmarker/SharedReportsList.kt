package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.example.distracted_driver_detection.databinding.ActivitySharedReportsListBinding



class SharedReportsList : AppCompatActivity() {
    private lateinit var binding: ActivitySharedReportsListBinding
    private lateinit var databaseReference: DatabaseReference
    private val driveReportsList = mutableListOf<DriveData>()
    private lateinit var adapter: DriveReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharedReportsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedUserId = intent.getStringExtra("sharedUserId")
        if (sharedUserId == null) {
            Toast.makeText(this, "Invalid shared user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user ID
        val currentUserId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("current_user_id", null)

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Reference to shared reports for this user
        databaseReference = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("sharedReports")
            .child(sharedUserId)

        // Set up RecyclerView with click listener
        binding.rvDriveReports.layoutManager = LinearLayoutManager(this)
        adapter = DriveReportsAdapter(driveReportsList) { driveData ->
            // Handle click on report item
            val intent = Intent(this, SharedReports::class.java)
            intent.putExtra("sessionId", driveData.sessionId)
            intent.putExtra("sharedUserId", sharedUserId)
            startActivity(intent)
        }
        binding.rvDriveReports.adapter = adapter

        // Set up Back button
        binding.btnBack.setOnClickListener { finish() }

        // Load shared reports
        loadSharedReports()
    }

    private fun loadSharedReports() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driveReportsList.clear()
                for (reportSnapshot in snapshot.children) {
                    try {
                        val report = reportSnapshot.getValue(DriveData::class.java)
                        report?.let {
                            // Make sure sessionId is set
                            if (it.sessionId == null) {
                                it.sessionId = reportSnapshot.key
                            }
                            driveReportsList.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e("SharedReportsList", "Error parsing report: ${e.message}")
                    }
                }

                // Sort reports by date and time
                driveReportsList.sortWith { a, b ->
                    val dateComparison = compareStringDates(b.date, a.date)
                    if (dateComparison == 0) {
                        // Convert times to 24-hour format for comparison
                        val time1 = convertTimeStringTo24Hour(b.startTime ?: "")
                        val time2 = convertTimeStringTo24Hour(a.startTime ?: "")
                        time1.compareTo(time2)
                    } else {
                        dateComparison
                    }
                }

                adapter.notifyDataSetChanged()

                // Update visibility
                if (driveReportsList.isEmpty()) {
                    binding.tvNoReports.visibility = View.VISIBLE
                    binding.rvDriveReports.visibility = View.GONE
                    Toast.makeText(this@SharedReportsList,
                        "No shared reports available", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvNoReports.visibility = View.GONE
                    binding.rvDriveReports.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SharedReportsList,
                    "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

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

    private fun convertTimeStringTo24Hour(timeStr: String): String {
        return try {
            if (timeStr.contains("AM") || timeStr.contains("PM")) {
                // Already in 12-hour format, convert to 24-hour
                val parts = timeStr.replace(" ", "").split(":")
                var hour = parts[0].toInt()
                val minute = parts[1].replace("AM", "").replace("PM", "")

                if (timeStr.contains("PM") && hour != 12) {
                    hour += 12
                } else if (timeStr.contains("AM") && hour == 12) {
                    hour = 0
                }

                String.format("%02d:%02d", hour, minute.toInt())
            } else {
                // Already in 24-hour format
                timeStr
            }
        } catch (e: Exception) {
            timeStr
        }
    }

    private fun convertTo12HourFormat(time24: String): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1]

            when {
                hour == 0 -> "12:$minute AM"
                hour < 12 -> "$hour:$minute AM"
                hour == 12 -> "12:$minute PM"
                else -> "${hour - 12}:$minute PM"
            }
        } catch (e: Exception) {
            time24
        }
    }
}