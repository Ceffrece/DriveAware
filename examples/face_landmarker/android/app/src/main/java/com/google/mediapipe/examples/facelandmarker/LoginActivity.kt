package com.google.mediapipe.examples.facelandmarker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mediapipe.examples.facelandmarker.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        binding.btnSubmit.setOnClickListener {
            val loginUsername = binding.etUsername.text.toString().trim()
            val loginPassword = binding.etPassword.text.toString().trim()

            if (loginUsername.isNotEmpty() && loginPassword.isNotEmpty()) {
                loginUser(loginUsername, loginPassword)
            } else {
                Toast.makeText(this, "All fields are mandatory", Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewLogin.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }

    private fun loginUser(username: String, password: String) {
        try {
            databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (userSnapshot in dataSnapshot.children) {
                                try {
                                    // Get basic user data separately
                                    val userName = userSnapshot.child("username").getValue(String::class.java)
                                    val userPassword = userSnapshot.child("password").getValue(String::class.java)

                                    if (userName == username && userPassword == password) {
                                        // Login successful
                                        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                                        with(sharedPref.edit()) {
                                            putString("current_user_id", userSnapshot.key)
                                            apply()
                                        }

                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Login Successful",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Navigate to MainActivity and clear the activity stack
                                        val intent = Intent(this@LoginActivity, HomePageActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                        return
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginActivity", "Error reading user data: ${e.message}")
                                }
                            }
                        }

                        // If we get here, login failed
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid username or password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login failed: ${databaseError.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } catch (e: Exception) {
            Log.e("LoginActivity", "Login error: ${e.message}")
            Toast.makeText(
                this@LoginActivity,
                "An error occurred: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}