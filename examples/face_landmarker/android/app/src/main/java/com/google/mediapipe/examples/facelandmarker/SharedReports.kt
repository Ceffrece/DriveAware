package com.google.mediapipe.examples.facelandmarker

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.google.mediapipe.examples.facelandmarker.databinding.ActivityDriverReportsBinding

class SharedReports : AppCompatActivity() {
    private lateinit var binding: ActivityDriverReportsBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionId = intent.getStringExtra("sessionId")
        val sharedUserId = intent.getStringExtra("sharedUserId")

        if (sessionId == null || sharedUserId == null) {
            Toast.makeText(this, "Invalid report details", Toast.LENGTH_SHORT).show()
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

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("sharedReports")
            .child(sharedUserId)
            .child(sessionId)

        // Set up Back button
        binding.btnBack.setOnClickListener { finish() }

        // Load report data
        loadSharedReport()
    }

    private fun loadSharedReport() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val report = snapshot.getValue(DriveData::class.java)
                if (report != null) {
                    updateUIWithDriveData(report)
                } else {
                    showNoDataMessage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SharedReports,
                    "Error loading report: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUIWithDriveData(driveData: DriveData) {
        with(binding) {
            // Display percentage
            val percentage = driveData.distractedDrivingPercentage?.toInt() ?: 0
            textViewDistractedDrivingPercentage.text =
                "Distracted Driving Percentage: $percentage%"

            // Display time
            val totalTime = driveData.totalDistractedTime ?: 0
            val minutes = totalTime / 60
            val seconds = totalTime % 60
            textViewTotalDistractedTime.text =
                "Total Distracted Time: ${minutes}m ${seconds}s"

            // Convert and display times
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
                    time
                }
            } ?: "--:--"

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
                    time
                }
            } ?: "--:--"

            textViewStartTime.text = "Start Time: $startTime"
            textViewEndTime.text = "End Time: $endTime"

            // Display date
            textViewDate.text = "Date: ${driveData.date ?: "--/--/----"}"
        }
    }

    private fun showNoDataMessage() {
        Toast.makeText(this, "No report data available", Toast.LENGTH_SHORT).show()
        with(binding) {
            textViewDistractedDrivingPercentage.text = "Distracted Driving Percentage: 0%"
            textViewTotalDistractedTime.text = "Total Distracted Time: 0m 0s"
            textViewStartTime.text = "Start Time: --:--"
            textViewEndTime.text = "End Time: --:--"
            textViewDate.text = "Date: --/--/----"
        }
    }
}