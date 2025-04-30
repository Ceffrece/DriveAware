package com.google.mediapipe.examples.facelandmarker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.mediapipe.examples.facelandmarker.ml.LiteModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MLTest : AppCompatActivity(){

    lateinit var selectButton: Button
    lateinit var predictButton: Button
    lateinit var backBtn: Button
    lateinit var resView : TextView
    lateinit var imgView : ImageView
    lateinit var bitmap : Bitmap

    private lateinit var mediaPlayerLevel1: MediaPlayer
    private lateinit var mediaPlayerLevel2: MediaPlayer
    private lateinit var mediaPlayerLevel3: MediaPlayer

    //image processor
    var imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(64, 64, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ml_test)

        selectButton = findViewById(R.id.selectButton)
        predictButton = findViewById(R.id.predictButton)
        backBtn = findViewById(R.id.backBtn)
        resView = findViewById(R.id.resView)
        imgView = findViewById(R.id.image_view)

        var distractedCount = 0
        var alertCount = 0
        var focusedCount = 0

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

        selectButton.setOnClickListener{
            val file = File("/storage/emulated/0/Android/data/com.google.mediapipe.examples.facelandmarker/files/photo_2025-04-08-14-16-33.jpg")
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgView.setImageBitmap(bitmap)
//            var intent = Intent()
//            intent.setAction(Intent.ACTION_GET_CONTENT)
//            intent.setType("image/*")
//            startActivityForResult(intent,100)
        }

        backBtn.setOnClickListener{
            var intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
        }

        predictButton.setOnClickListener{

            //Make a bitmap from a string for the path
            val lines = File("/data/user/0/com.google.mediapipe.examples.facelandmarker/files/faceimg.txt").readLines()
            Log.d("DriveActivity", "Lines: $lines")

            // Get the dimensions of the image (e.g., 3x3)
            val width = 64  // Adjust as necessary
            val height = 64 // Adjust as necessary
            val pixels = IntArray(12288)

            Log.d("MLTest", "In processImage")

            var pixelIndex = 0
            for (line in lines) {
                val values = line.split(" ")
                Log.d("MLTest", "Line: $line")

                // Parse RGB values and convert them back to integers (from normalized floats)
                if (values.size == 3) {
                    val r = (values[0].toFloat() * 255).toInt()
                    val g = (values[1].toFloat() * 255).toInt()
                    val b = (values[2].toFloat() * 255).toInt()
                    Log.d("MLTest", "RGB: $r, $g, $b")

                    // Combine RGB into a single integer
                    val pixel = Color.rgb(r,g,b)
                    pixels[pixelIndex++] = pixel
                    Log.d("MLTest", "Pixel: $pixel")
                }
            }

            Log.d("MLTest", "Creating Bitmap")
            // Create a Bitmap from the pixel array
            bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            imgView.setImageBitmap(bitmap)

            /*tensorImage = imageProcessor.process(tensorImage)

            val model = LiteModel.newInstance(this)

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 64, 64, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(tensorImage.buffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            var maxIdx = 0
            outputFeature0.forEachIndexed { index, fl ->
                if(outputFeature0[maxIdx] < fl) {
                    maxIdx = index
                }
            }
            var maxVal = outputFeature0[maxIdx]
            if(maxVal < 0.5){
                resView.setText("Focused")
                distractedCount = 0
                focusedCount += 1
                if(focusedCount >= 6){
                    alertCount = 0
                    focusedCount = 0
                }
            } else {
                resView.setText("Distracted")
                distractedCount += 1
                if(distractedCount >= 3){
                    alertCount += 1
                    playAlert(alertCount)
                    distractedCount = 0
                }
            }

            // Releases model resources if no longer used.
            model.close()*/
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 100){
            var uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            imgView.setImageBitmap(bitmap)
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

    fun runPythonExecutable(imagePath: String): String? {
        try {
            val process = ProcessBuilder("./preprocess_eye", imagePath)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()

            return output.trim() // This should be the output image path
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}