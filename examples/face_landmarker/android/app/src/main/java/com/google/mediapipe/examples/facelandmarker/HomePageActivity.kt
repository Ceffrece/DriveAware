package com.google.mediapipe.examples.facelandmarker


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.widget.TextView

class HomePageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        // Setting up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = null

        // Check login status and update button text
        updateLoginButton()

        val navigateButton = findViewById<TextView>(R.id.navigate_button)
        navigateButton.setOnClickListener {
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            if (sharedPref.getString("current_user_id", null) != null) {
                // User is logged in, so log them out
                with(sharedPref.edit()) {
                    remove("current_user_id")
                    apply()
                }
                updateLoginButton()
                // Show logout message
                android.widget.Toast.makeText(
                    this,
                    "Logged out successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                // User is not logged in, navigate to login screen
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        // Set up drive button
        val btnDrive: Button = findViewById(R.id.btnDrive)
        btnDrive.setOnClickListener {
            if (checkLoginRequired()) {
                val intent = Intent(this, DriveActivity::class.java)
                startActivity(intent)
            }
        }

        // Settings button
        val btnSettings = findViewById<Button>(R.id.settingsBtn)
        btnSettings.setOnClickListener {
            if (checkLoginRequired()) {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        // View Reports button
        val btnViewReports = findViewById<Button>(R.id.btnViewReports)
        btnViewReports.setOnClickListener {
            if (checkLoginRequired()) {
                val intent = Intent(this, DrivingReportsList::class.java)
                startActivity(intent)
            }
        }

        // Alerts button
        val btnAlerts = findViewById<Button>(R.id.alertButton)
        btnAlerts.setOnClickListener {
            if (checkLoginRequired()) {
                val intent = Intent(this, AlertActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun checkLoginRequired(): Boolean {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("current_user_id", null)

        return if (userId == null) {
            android.widget.Toast.makeText(
                this,
                "Please log in first",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            false
        } else {
            true
        }
    }

    private fun updateLoginButton() {
        val navigateButton = findViewById<TextView>(R.id.navigate_button)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getString("current_user_id", null) != null
        navigateButton.text = if (isLoggedIn) "LOGOUT" else "LOGIN"
    }

    override fun onResume() {
        super.onResume()
        updateLoginButton()
    }
}