package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
import com.google.mediapipe.examples.facelandmarker.databinding.ActivityDriverReportsBinding

class DriverReports : AppCompatActivity() {
    private lateinit var binding: ActivityDriverReportsBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance()

        // Get current user ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("current_user_id", null)

        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if this is a shared report
        val isSharedReport = intent.getBooleanExtra("isSharedReport", false)
        val sharedUserId = intent.getStringExtra("sharedUserId")

        // Update database reference based on whether it's a shared report
        databaseReference = if (isSharedReport && sharedUserId != null) {
            firebaseDatabase.reference
                .child("users")
                .child(userId)
                .child("sharedReports")
                .child(sharedUserId)
        } else {
            firebaseDatabase.reference
                .child("users")
                .child(userId)
                .child("driveSessions")
        }

        // Set up Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve drive data
        val sessionId = intent.getStringExtra("sessionId")
        if (sessionId != null) {
            loadDriveData(sessionId)
        } else {
            loadLatestDriveData()
        }
    }

    private fun loadDriveData(sessionId: String) {
        Log.d("DriverReports", "Loading drive data for session: $sessionId")

        databaseReference.child(sessionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("DriverReports", "Data snapshot: ${snapshot.value}")

                    if (snapshot.exists()) {
                        try {
                            val driveData = snapshot.getValue(DriveData::class.java)
                            if (driveData != null) {
                                Log.d("DriverReports", "Drive data loaded: $driveData")
                                updateUIWithDriveData(driveData)
                            } else {
                                Log.e("DriverReports", "Failed to parse drive data")
                                showNoDataMessage()
                            }
                        } catch (e: Exception) {
                            Log.e("DriverReports", "Error parsing data: ${e.message}")
                            showNoDataMessage()
                        }
                    } else {
                        Log.d("DriverReports", "No data found for session: $sessionId")
                        showNoDataMessage()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DriverReports", "Database error: ${error.message}")
                    Toast.makeText(
                        this@DriverReports,
                        "Failed to load drive data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadLatestDriveData() {
        databaseReference.orderByChild("startTime").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            val driveData = snapshot.children.first().getValue(DriveData::class.java)
                            if (driveData != null) {
                                Log.d("DriverReports", "Latest drive data loaded: $driveData")
                                updateUIWithDriveData(driveData)
                            } else {
                                Log.e("DriverReports", "Failed to parse latest drive data")
                                showNoDataMessage()
                            }
                        } catch (e: Exception) {
                            Log.e("DriverReports", "Error parsing latest data: ${e.message}")
                            showNoDataMessage()
                        }
                    } else {
                        Log.d("DriverReports", "No latest drive data found")
                        showNoDataMessage()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DriverReports", "Database error: ${error.message}")
                    Toast.makeText(
                        this@DriverReports,
                        "Failed to load drive data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateUIWithDriveData(driveData: DriveData) {
        with(binding) {
            val percentage = driveData.distractedDrivingPercentage?.toInt() ?: 0
            textViewDistractedDrivingPercentage.text =
                "Distracted Driving Percentage: $percentage%"

            // Format time in minutes and seconds
            val totalTime = driveData.totalDistractedTime ?: 0
            val minutes = totalTime / 60
            val seconds = totalTime % 60
            textViewTotalDistractedTime.text =
                "Total Distracted Time: ${minutes}m ${seconds}s"

            // Convert distance to kilometers if over 1000 meters
            val distance = driveData.totalDistractedDistance ?: 0.0
            val distanceText = if (distance >= 1000) {
                String.format("%.2f km", distance / 1000)
            } else {
                String.format("%.0f m", distance)
            }
            textViewTotalDistractedDistance.text = "Total Distracted Distance: $distanceText"

            // Convert start time from 24-hour to 12-hour format
            val startTime = driveData.startTime?.let { time ->
                try {
                    val parts = time.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1]
                    when {
                        hour == 0 -> "12:$minute AM"
                        hour < 12 -> "$hour:$minute AM"
                        hour == 12 -> "12:$minute PM"
                        else -> "${hour - 12}:$minute PM"
                    }
                } catch (e: Exception) {
                    time // Return original time if conversion fails
                }
            } ?: "--:--"

            // Convert end time from 24-hour to 12-hour format
            val endTime = driveData.endTime?.let { time ->
                try {
                    val parts = time.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1]
                    when {
                        hour == 0 -> "12:$minute AM"
                        hour < 12 -> "$hour:$minute AM"
                        hour == 12 -> "12:$minute PM"
                        else -> "${hour - 12}:$minute PM"
                    }
                } catch (e: Exception) {
                    time // Return original time if conversion fails
                }
            } ?: "--:--"

            textViewStartTime.text = "Start Time: $startTime"
            textViewEndTime.text = "End Time: $endTime"

            val date = driveData.date ?: "--/--/----"
            textViewDate.text = "Date: $date"
        }
    }

    private fun showNoDataMessage() {
        Toast.makeText(this, "No drive data available", Toast.LENGTH_SHORT).show()
        with(binding) {
            textViewDistractedDrivingPercentage.text = "Distracted Driving Percentage: 0%"
            textViewTotalDistractedTime.text = "Total Distracted Time: 0m 0s"
            textViewTotalDistractedDistance.text = "Total Distracted Distance: 0 m"
            textViewStartTime.text = "Start Time: --:--"
            textViewEndTime.text = "End Time: --:--"
            textViewDate.text = "Date: --/--/----"
        }
    }
}