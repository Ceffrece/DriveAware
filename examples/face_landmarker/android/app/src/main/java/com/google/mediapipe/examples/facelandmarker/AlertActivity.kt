package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.Manifest
import androidx.appcompat.widget.Toolbar

class AlertActivity : AppCompatActivity() {

    private lateinit var mediaPlayerLevel1: MediaPlayer
    private lateinit var mediaPlayerLevel2: MediaPlayer
    private lateinit var mediaPlayerLevel3: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alert_activity)

        // Initialize MediaPlayers with the sound resources
        mediaPlayerLevel1 = MediaPlayer.create(this, R.raw.alert_level1)
        mediaPlayerLevel2 = MediaPlayer.create(this, R.raw.alert_level2)
        mediaPlayerLevel3 = MediaPlayer.create(this, R.raw.alert_level3)

        // Toolbar stuff
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Find buttons by ID
        val btnAlertLevel1: Button = findViewById(R.id.btnAlertLevel1)
        val btnAlertLevel2: Button = findViewById(R.id.btnAlertLevel2)
        val btnAlertLevel3: Button = findViewById(R.id.btnAlertLevel3)

        // Set onClickListeners for each button
        btnAlertLevel1.setOnClickListener {
            scheduleSingleAlert(
                level = 1,
                message = "You are slightly distracted! Refocus!",
                soundResId = R.raw.alert_level1
            )
        }

        // Alert Level 2 Button
        btnAlertLevel2.setOnClickListener {
            scheduleSingleAlert(
                level = 2,
                message = "You are distracted! Refocus!",
                soundResId = R.raw.alert_level2
            )
        }

        // Alert Level 3 Button
        btnAlertLevel3.setOnClickListener {
            scheduleSingleAlert(
                level = 3,
                message = "You are seriously distracted! Refocus!",
                soundResId = R.raw.alert_level3
            )
        }

        // Request perms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101 // Request code
                )
            }
        }
    }

    private fun playSound(mediaPlayer: MediaPlayer) {
        mediaPlayer.setVolume(1.0f,1.0f);
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Function to schedule a single alert
    private fun scheduleSingleAlert(level: Int, message: String, soundResId: Int) {
        val workManager = WorkManager.getInstance(this)

        val alertData = Data.Builder()
            .putInt("alert_level", level)
            .putString("alert_message", message)
            .putInt("alert_sound", soundResId)
            .build()

        val alertWork = OneTimeWorkRequestBuilder<AlertWorker>()
            .setInputData(alertData)
            .build()

        workManager.enqueue(alertWork)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayers to free resources
        mediaPlayerLevel1.release()
        mediaPlayerLevel2.release()
        mediaPlayerLevel3.release()
    }
}