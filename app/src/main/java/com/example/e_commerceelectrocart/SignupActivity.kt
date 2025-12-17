package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_commerceelectrocart.databinding.ActivitySignupBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || phoneNumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user == null) {
                            Toast.makeText(this, "User creation failed, please try again.", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        val userData = hashMapOf(
                            "name" to name,
                            "phoneNumber" to phoneNumber,
                            "email" to email
                        )

                        FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
                            .setValue(userData).addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    showThankYouDialog()
                                } else {
                                    Log.e("SignupActivity", "Failed to write user data to database", dbTask.exception)
                                    Toast.makeText(this, "Database Error: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        if (authTask.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email already in use. Please log in.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Log.e("SignupActivity", "User authentication failed", authTask.exception)
                            Toast.makeText(this, "Sign up failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun showThankYouDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Welcome to Electrocart!")
            .setMessage("Thank you for creating an account. You can now log in to start shopping.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .show()
    }
}
