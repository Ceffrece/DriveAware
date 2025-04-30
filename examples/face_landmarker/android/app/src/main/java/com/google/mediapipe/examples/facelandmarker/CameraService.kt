package com.google.mediapipe.examples.facelandmarker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class CameraService : LifecycleService() {

    private lateinit var imageCapture: ImageCapture

    override fun onCreate() {
        super.onCreate()

        // Create a notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        //Start service as a foreground service
        startForegroundService()

        //Initialize CameraX and imageCapture
        setupCamera()

        //Start capturing images
        startCapture()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Camera provider is ready
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case for displaying camera feed
            val preview = Preview.Builder().build()

            // ImageCapture use case for taking photos
            imageCapture = ImageCapture.Builder().build()

            // Camera selector (use back camera here)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Unbind any previous use cases
                cameraProvider.unbindAll()

                // Bind the preview and imageCapture use cases to the camera lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                Log.e("CameraService", "Error binding camera use cases", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startForegroundService() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Capture Service")
            .setContentText("Capturing images in the background")
            .setSmallIcon(R.drawable.start_drive_img) // Use your own icon
            .build()

        startForeground(1, notification)
    }

    private fun startCapture() {
        // Use a coroutine to handle capturing images every 3 seconds
        runBlocking {
            launch(Dispatchers.IO) {
                while (true) {
                    captureImage()
                    delay(3000) // Capture an image every 3 seconds
                }
            }
        }
    }

    private fun captureImage() {
        val photoFile = createFile(applicationContext)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("CameraService", "Image saved to ${photoFile.absolutePath}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraService", "Error taking photo", exception)
                }
            }
        )
    }

    private fun createFile(context: Context): File {
        val directory = context.getExternalFilesDir(null) // Change to custom directory if needed
        val timestamp = System.currentTimeMillis().toString()
        return File(directory, "photo_$timestamp.jpg")
    }

    companion object {
        const val CHANNEL_ID = "camera_service_channel"
    }
}

