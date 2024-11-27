package com.example.firebaseauthentcation.activities.faculty

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebaseauthentcation.adapter.StudentAdapter
import com.example.firebaseauthentcation.databinding.ActivityAllStudentsBinding
import com.example.firebaseauthentcation.model.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class AllStudentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllStudentsBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var studentAdapter: StudentAdapter
    private val studentList = mutableListOf<Student>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllStudentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadStudents()
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(studentList)
        binding.recyclerViewStudents.apply {
            layoutManager = LinearLayoutManager(this@AllStudentsActivity)
            adapter = studentAdapter
        }
    }

    private fun loadStudents() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Fetch the faculty's department
            database.collection("FacultyBio").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val department = document.getString("department")
                    if (!department.isNullOrEmpty()) {
                        fetchStudentsFromDepartment(department)
                    } else {
                        Toast.makeText(this, "No department found for the faculty.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching faculty details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudentsFromDepartment(department: String) {
        database.collection("StudentsBio")
            .whereEqualTo("department", department)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                studentList.clear()
                for (document in querySnapshot.documents) {
                    val student = document.toObject(Student::class.java)
                    if (student != null) {
                        studentList.add(student)
                    }
                }
                if (studentList.isEmpty()) {
                    Toast.makeText(this, "No students found in this department.", Toast.LENGTH_SHORT).show()
                }
                studentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching students: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
