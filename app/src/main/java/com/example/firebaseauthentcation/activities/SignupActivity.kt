package com.example.firebaseauthentcation.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseauthentcation.activities.faculty.FacultyActivity
import com.example.firebaseauthentcation.activities.student.StudentActivity
import com.example.firebaseauthentcation.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        val userTypes = arrayOf("Student", "Faculty")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypes)
        binding.spinnerUserType.adapter = adapter

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val userType = binding.spinnerUserType.selectedItem.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                signUpUser(name, email, password, userType)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signUpUser(name: String, email: String, password: String, userType: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "userType" to userType
                    )

                    userId?.let {
                        firestore.collection("users").document(it).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                                if (userType == "Student") {
                                    startActivity(Intent(this, StudentActivity::class.java))
                                } else {
                                    startActivity(Intent(this, FacultyActivity::class.java))
                                }
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
