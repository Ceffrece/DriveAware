package com.google.mediapipe.examples.facelandmarker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.Calendar

class DriveActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var startStopButton: Button
    private lateinit var backButton: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var mediaPlayerLevel1: MediaPlayer
    private lateinit var mediaPlayerLevel2: MediaPlayer
    private lateinit var mediaPlayerLevel3: MediaPlayer
    lateinit var bitmap : Bitmap
    var distractedCount = 0
    var alertCount = 0
    var focusedCount = 0
    var distractedTotal = 0
    var focusedTotal = 0
    var startTime = Calendar.getInstance().time
    var endTime = Calendar.getInstance().time
    lateinit var imageProcessor : ImageProcessor

    private var isCapturing = false
    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drive_activity)

        previewView = findViewById(R.id.previewView)
        startStopButton = findViewById(R.id.startStopButton)
        backButton = findViewById(R.id.backButton)

        // Create notification channel for Android 8.0 (API level 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val descriptionText = "This is an alerts channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("Alerts", name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(64, 64, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        //Handle python stuff
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        startStopButton.setOnClickListener {
            if (isCapturing) {
                stopCapture()
            } else {
                startCapture()
            }
        }

        backButton.setOnClickListener {
            onBackPressed() // Handle back navigation
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Camera provider is ready
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .build()

        // Select the camera (rear camera here)
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        try {
            // Unbind use cases before rebinding them
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("DriveActivity", "Binding camera use cases failed", e)
        }
    }

    private fun startCapture() {
        isCapturing = true
        startStopButton.text = "Stop Drive"

        startTime = Calendar.getInstance().time

        // Start a coroutine to capture images every 3 seconds
        lifecycleScope.launch(Dispatchers.IO) {
            while (isCapturing) {
                //captureImage()
                delay(3000) // Delay for 3 seconds before taking another picture
            }
        }
    }

    private fun stopCapture() {
        isCapturing = false
        startStopButton.text = "Start Drive"

        endTime = Calendar.getInstance().time

        //Save to database as a new drive
        val driveData = DriveData(
            sessionId = "-" + System.currentTimeMillis().toString(),
            date = SimpleDateFormat("yyyy-MM-dd").format(startTime),
            distractedDrivingPercentage = (distractedTotal.toDouble() / (distractedTotal + focusedTotal).toDouble()) * 100,
            startTime = SimpleDateFormat("HH:mm").format(startTime),
            endTime = SimpleDateFormat("HH:mm").format(endTime),
            totalDistractedTime = distractedTotal * 3
            )
        saveDriveSession(driveData)
    }

    private fun captureImage() {
        val photoFile = createFile(applicationContext)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("DriveActivity", "Image saved to ${photoFile.absolutePath}")
                    //Get python instance
                    val python = Python.getInstance()
                    //Get python file
                    val pythonModule = python.getModule("server")
                    val result = pythonModule.callAttr("serverFunction", photoFile.absolutePath)
                    Log.d("DriveActivity", "Python result: $result")
                    if(result != null){
                        /*val path = "/data/user/0/com.google.mediapipe.examples.facelandmarker/files/faceimg.txt"
                        val file = File(path) //Use path
                        if (!file.exists()) {
                            throw FileNotFoundException("File not found: $result")
                        } else {
                            processImage(path, result.toInt()) //send to processor with the file path
                        }*/
                       var resultString = result.toString()
                       if(resultString.equals("Focused")){
                           // Display a Toast message indicating that the person was focused
                           runOnUiThread {
                               Toast.makeText(this@DriveActivity, "Focused", Toast.LENGTH_SHORT).show()
                           }
                           processImage(0)
                       } else {
                           // Display a Toast message indicating that the user was distracted
                           runOnUiThread {
                               Toast.makeText(this@DriveActivity, "Distracted", Toast.LENGTH_SHORT).show()
                           }
                           processImage(1)
                       }
                    }
                    else{
                        //No left eye found, distracted
                        distractedCount += 1
                        distractedTotal += 1
                        // Display a Toast message indicating that the user was distracted
                        runOnUiThread {
                            Toast.makeText(this@DriveActivity, "Distracted", Toast.LENGTH_SHORT).show()
                        }
                        if(distractedCount >= 3){
                            alertCount += 1
                            playAlert(alertCount)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("DriveActivity", "Error taking photo", exception)
                }
            }
        )
    }

    private fun createFile(context: android.content.Context): File {
        val directory = context.getExternalFilesDir(null) // Change this to a custom directory if needed
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())
        return File(directory, "photo_$timestamp.jpg")
    }

    /*fun sendImageAndReceiveLabel(serverIp: String, serverPort: Int, imagePath: String): String? {
        var socket: Socket? = null
        return try {
            socket = Socket(serverIp, serverPort)
            val outputStream = DataOutputStream(socket.getOutputStream())
            val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Load image and encode as JPEG bytes
            val imageFile = File(imagePath)
            val bufferedImage: BufferedImage = ImageIO.read(imageFile)
            val baos = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "jpg", baos)
            val imageBytes = baos.toByteArray()

            // Send image length (4 bytes, big-endian)
            outputStream.writeInt(imageBytes.size)

            // Send image bytes
            outputStream.write(imageBytes)
            outputStream.flush()

            // Read server response (Distracted or Focused)
            val response = inputStream.readLine()

            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            socket?.close()
        }
    }*/


    private fun processImage(distracted: Int) {
        if(distracted == 0){
            distractedCount = 0
            focusedCount += 1
            focusedTotal += 1
            if(focusedCount >= 6){
                alertCount = 0
                focusedCount = 0
            }
        } else {
            distractedCount += 1
            distractedTotal += 1
            if(distractedCount >= 3){
                alertCount += 1
                playAlert(alertCount)
            }
        }
    }

    fun playAlert(alertNum:Int){
        // Initialize MediaPlayers with the sound resources
        mediaPlayerLevel1 = MediaPlayer.create(this, R.raw.alert_level1)
        mediaPlayerLevel2 = MediaPlayer.create(this, R.raw.alert_level2)
        mediaPlayerLevel3 = MediaPlayer.create(this, R.raw.alert_level3)
        if(alertNum == 1){
            scheduleSingleAlert(
                level = 1,
                message = "You are slightly distracted! Refocus!",
                soundResId = R.raw.alert_level1
            )
        }
        else if(alertNum == 2){
            scheduleSingleAlert(
                level = 2,
                message = "You are distracted! Refocus!",
                soundResId = R.raw.alert_level2
            )
        }
        else{//AlertNum is 3 or higher
            scheduleSingleAlert(
                level = 3,
                message = "You are seriously distracted! Refocus!",
                soundResId = R.raw.alert_level3
            )
        }
    }
    // Function to schedule a single delayed alert
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
    fun saveDriveSession(driveSession: DriveData) {
        // Get the current user's UID from Firebase Authentication (assuming you're using Firebase Authentication)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("current_user_id", null)

        // Ensure the user is authenticated before proceeding
        if (userId == null) {
            // Handle the case where the user is not authenticated
            println("User not authenticated!")
            return
        }

        // Get the Firebase Realtime Database reference
        val database: DatabaseReference = FirebaseDatabase.getInstance().reference

        // Use the session ID or create a new unique ID (Firebase will auto-generate one if needed)
        val sessionRef = database.child("users").child(userId).child("driveSessions").child(driveSession.sessionId ?: "")

        // Save the drive session data to Firebase
        sessionRef.setValue(driveSession)
            .addOnSuccessListener {
                println("Drive session saved successfully!")
            }
            .addOnFailureListener { exception ->
                println("Error saving drive session: ${exception.message}")
            }
    }
}