package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.database.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseReference: DatabaseReference
    private var isChangingTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        // Get current user ID from shared preferences
        val currentUserId = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("current_user_id", null)

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Existing switches setup
        setupExistingSwitches()

        // Set up Share Reports button
        val btnShareReports: Button = findViewById(R.id.btnShareReports)
        btnShareReports.setOnClickListener {
            showShareDialog()
        }

        // Set up View Shared Reports button
        val btnViewShared: Button = findViewById(R.id.btnViewShared)
        btnViewShared.setOnClickListener {
            showSharedReportsDialog()
        }
    }
    private fun shareReportsWithUser(targetUsername: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // First verify the target user exists
        databaseReference.child("users")
            .orderByChild("username")
            .equalTo(targetUsername)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val targetUserData = snapshot.children.first()
                        val targetUserId = targetUserData.key!!

                        if (targetUserId == currentUserId) {
                            Toast.makeText(this@SettingsActivity,
                                "Cannot share with yourself", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // Get current user's drive sessions
                        databaseReference.child("users")
                            .child(currentUserId)
                            .child("driveSessions")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(driveSnapshot: DataSnapshot) {
                                    if (driveSnapshot.exists()) {
                                        // Create shared reports path
                                        val targetSharedReportsRef = databaseReference
                                            .child("users")
                                            .child(targetUserId)
                                            .child("sharedReports")
                                            .child(currentUserId)

                                        // Copy drive sessions
                                        driveSnapshot.children.forEach { sessionSnapshot ->
                                            val sessionId = sessionSnapshot.key!!
                                            val sessionData = sessionSnapshot.getValue(DriveData::class.java)
                                            sessionData?.let {
                                                targetSharedReportsRef.child(sessionId).setValue(it)
                                            }
                                        }

                                        Toast.makeText(this@SettingsActivity,
                                            "Reports shared with $targetUsername", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@SettingsActivity,
                                            "No reports to share", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@SettingsActivity,
                                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    } else {
                        Toast.makeText(this@SettingsActivity,
                            "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showSharedReportsDialog() {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check user's shared reports
        databaseReference.child("users")
            .child(currentUserId)
            .child("sharedReports")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        val userIds = snapshot.children.mapNotNull { it.key }
                        if (userIds.isNotEmpty()) {
                            // Fetch usernames for these IDs
                            fetchUsernames(userIds)
                        } else {
                            Toast.makeText(this@SettingsActivity,
                                "No shared reports available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SettingsActivity,
                            "No shared reports available", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchUsernames(userIds: List<String>) {
        val usersList = mutableListOf<Pair<String, String>>()
        var completedQueries = 0

        userIds.forEach { userId ->
            databaseReference.child("users").child(userId)
                .child("username")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        completedQueries++

                        val username = snapshot.getValue(String::class.java)
                        if (username != null) {
                            usersList.add(Pair(userId, username))
                        }

                        // Show dialog when all queries are complete
                        if (completedQueries == userIds.size) {
                            showUsersListDialog(usersList)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        completedQueries++
                        Toast.makeText(this@SettingsActivity,
                            "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun showUsersListDialog(users: List<Pair<String, String>>) {
        if (users.isEmpty()) {
            Toast.makeText(this@SettingsActivity,
                "No shared reports available", Toast.LENGTH_SHORT).show()
            return
        }

        val usernames = users.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Shared Reports")
            .setItems(usernames) { _, position ->
                val intent = Intent(this, SharedReportsList::class.java)
                intent.putExtra("sharedUserId", users[position].first)
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun getCurrentUserId(): String? {
        return getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("current_user_id", null)
    }

    private fun setupExistingSwitches() {
        // Notification switch
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
        switchNotifications.isChecked = sharedPreferences.getBoolean("notifications", true)
        switchNotifications.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply()
        }
    }
    private fun showShareDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_share_reports, null)
        val usernameInput = dialogView.findViewById<EditText>(R.id.editTextUsername)

        AlertDialog.Builder(this)
            .setTitle("Share Driving Reports")
            .setView(dialogView)
            .setPositiveButton("Share") { _, _ ->
                val targetUsername = usernameInput.text.toString()
                if (targetUsername.isNotEmpty()) {
                    shareReportsWithUser(targetUsername)
                } else {
                    Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}