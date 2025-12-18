package com.example.e_commerceelectrocart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_commerceelectrocart.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private val adminEmail = "admin@electrocart.com"
    private val TAG = "AdminLoginCheck"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- DEVELOPER BACKDOOR FOR ADMIN PANEL ---
            if (email == adminEmail) {
                Toast.makeText(this, "Opening Admin Panel in Dev Mode...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
                return@setOnClickListener
            }
            // --- END BACKDOOR ---

            // Regular User Login
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    val exception = it.exception
                    Log.e(TAG, "Login failed in onComplete listener", exception)
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(this, "Login failed: No account found with this email.", Toast.LENGTH_LONG).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Login failed: Incorrect password. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Login failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        // If a regular user is already logged in, redirect them.
        // We don't redirect admins, so they have a choice to log in as a regular user.
        if (currentUser != null && currentUser.email != adminEmail) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }
}
