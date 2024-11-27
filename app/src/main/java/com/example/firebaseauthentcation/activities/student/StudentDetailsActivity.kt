package com.example.firebaseauthentcation.activities.student

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.firebaseauthentcation.R
import com.example.firebaseauthentcation.databinding.ActivityStudentDetailsBinding


class StudentDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var binding: ActivityStudentDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.department_array,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.departmentSpinner.adapter = adapter



        binding.dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                binding.dobEditText.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
            }, year, month, day)
            datePicker.show()
        }

        binding.updateButton.setOnClickListener {
            loadStudentBio()
        }
        binding.saveButton.setOnClickListener {
            saveStudentBio()
        }
    }

    private fun loadStudentBio() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
             database.collection("StudentsBio").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.studentIdEditText.setText(document.getString("studentId"))
                        binding.nameEditText.setText(document.getString("name"))
                        binding.phoneEditText.setText(document.getString("phone"))
                        binding.bloodGroupEditText.setText(document.getString("bloodGroup"))
                        binding.dobEditText.setText(document.getString("dob"))
                        binding.descriptionEditText.setText(document.getString("description"))

                        val department = document.getString("department")
                        val departmentsArray = resources.getStringArray(R.array.department_array)
                        val position = departmentsArray.indexOf(department)
                        binding.departmentSpinner.setSelection(position)

                        setResult(RESULT_OK)

                        Toast.makeText(this, "Information loaded for editing.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No bio found. Please enter your details.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load bio: ${e.message}", Toast.LENGTH_SHORT).show()
                }


        }
    }

    private fun saveStudentBio() {
        val studentId = binding.studentIdEditText.text.toString().trim()
        val department = binding.departmentSpinner.selectedItem.toString()
        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val bloodGroup = binding.bloodGroupEditText.text.toString().trim()
        val dob = binding.dobEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (studentId.isEmpty() || department.isEmpty() || name.isEmpty() || phone.isEmpty() ||
            bloodGroup.isEmpty() || dob.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId != null) {
            val studentBio = hashMapOf(
                "studentId" to studentId,
                "department" to department,
                "name" to name,
                "phone" to phone,
                "bloodGroup" to bloodGroup,
                "dob" to dob,
                "description" to description
            )

            database.collection("StudentsBio").document(userId)
                .set(studentBio)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bio saved successfully!", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving bio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearFields() {
        binding.studentIdEditText.text.clear()
        binding.nameEditText.text.clear()
        binding.phoneEditText.text.clear()
        binding.bloodGroupEditText.text.clear()
        binding.dobEditText.text.clear()
        binding.descriptionEditText.text.clear()
        binding.departmentSpinner.setSelection(0)
    }
}
