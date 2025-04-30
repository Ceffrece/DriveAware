package com.google.mediapipe.examples.facelandmarker
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import java.io.File

class TestingPreprocess : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_preprocess)

        // Find the ImageView from the layout
        val imageView: ImageView = findViewById(R.id.imageView)

        // Assuming you already have a file path to your text file
        val filePath = "/data/user/0/com.google.mediapipe.examples.facelandmarker/files/faceimg.txt" // Modify this with the actual file path

        // Create the Bitmap from the text file
        val bitmap = createBitmapFromTextFile(filePath)

        if (bitmap != null) {
            // Set the Bitmap to the ImageView
            Log.d("Bitmap", "Bitmap loaded successfully")
            imageView.setImageBitmap(bitmap)
        } else {
            // Handle error (e.g., show a Toast)
            Toast.makeText(this, "Failed to load the image", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to create Bitmap from text file (same as provided earlier)
    fun createBitmapFromTextFile(filePath: String): Bitmap? {
        try {
            // Read the file into a list of lines
            val lines = File(filePath).readLines()

            // Define image dimensions (example: 3x3, modify according to actual data)
            val width = 3  // Adjust this based on your image's actual width
            val height = 3 // Adjust this based on your image's actual height
            val pixels = IntArray(width * height)

            var pixelIndex = 0
            for (line in lines) {
                val values = line.split(" ")

                // Parse RGB values and convert them back to integers (from normalized floats)
                if (values.size == 3) {
                    val r = (values[0].toFloat() * 255).toInt()
                    val g = (values[1].toFloat() * 255).toInt()
                    val b = (values[2].toFloat() * 255).toInt()

                    // Combine RGB into a single integer
                    val pixel = (255 shl 24) or (r shl 16) or (g shl 8) or b
                    pixels[pixelIndex++] = pixel
                }
            }

            // Create and return the Bitmap
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
