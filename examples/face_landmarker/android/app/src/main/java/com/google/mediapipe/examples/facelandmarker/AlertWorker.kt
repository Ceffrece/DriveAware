package com.google.mediapipe.examples.facelandmarker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class AlertWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val alertLevel = inputData.getInt("alert_level", 1)
        val message = inputData.getString("alert_message") ?: "Default Alert"
        val soundResId = inputData.getInt("alert_sound", R.raw.alert_level1)

        // Play the sound
        playAlertSound(soundResId)

        // Show the notification
        showNotification(alertLevel, message)

        return Result.success()
    }

    private fun playAlertSound(soundResId: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.start()
    }

    private fun showNotification(alertLevel: Int, message: String) {
        val channelId = "alert_channel_id"
        val notificationId = alertLevel // Unique ID for each level of notification

        // Create a Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Alert Notifications"
            val channelDescription = "Displays alert notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.alert_img) // Add your app's notification icon here
            .setContentTitle("Alert Level: $alertLevel")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context, // Fixed: Use context here
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Request the POST_NOTIFICATIONS permission if needed
                return
            }
            notify(notificationId, builder.build())
        }
    }
}