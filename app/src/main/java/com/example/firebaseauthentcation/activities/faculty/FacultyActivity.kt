package com.example.firebaseauthentcation.activities.faculty

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseauthentcation.databinding.ActivityFacultyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FacultyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacultyBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseFirestore

    companion object {
        private const val REQUEST_CODE_UPDATE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacultyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Swipe-to-refresh listener
        binding.swipeRefresh.setOnRefreshListener {
            refreshContent()
        }

        // Navigate to FacultyDetailsActivity
        binding.facultyDetailsButton.setOnClickListener {
            val intent = Intent(this, FacultyDetailsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_UPDATE)
        }

        binding.viewStudentButton.setOnClickListener{
            val intent = Intent(this, AllStudentsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun refreshContent() {
        // Reload faculty bio
        loadFacultyBio()

        // Stop swipe-to-refresh animation
        binding.swipeRefresh.isRefreshing = false
    }

    private fun loadFacultyBio() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.collection("FacultyBio").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Populate TextViews with data
                        binding.facultyIdTextView.text = document.getString("facultyId")
                        binding.departmentTextView.text = document.getString("department")
                        binding.nameTextView.text = document.getString("name")
                        binding.phoneTextView.text = document.getString("phone")
                        binding.bloodGroupTextView.text = document.getString("bloodGroup")
                        binding.dobTextView.text = document.getString("dob")
                        binding.descriptionTextView.text = document.getString("description")
                        binding.professionTextView.text = document.getString("profession")
                    } else {
                        Toast.makeText(this, "No bio found for this faculty.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load bio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshContent()
    }
}
