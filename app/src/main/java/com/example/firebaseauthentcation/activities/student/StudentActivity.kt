package com.example.firebaseauthentcation.activities.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.firebaseauthentcation.R
import com.example.firebaseauthentcation.databinding.ActivityStudentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class StudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.swipeRefresh.setOnRefreshListener {
            refreshContent()
        }

        binding.studentDetailsButton.setOnClickListener {
            val intent = Intent(this, StudentDetailsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_UPDATE)
        }

        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.videoButton.setOnClickListener {
            val intent = Intent(this, VideoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun refreshContent() {
        loadStudentBio()
        loadSelfIntroVideo()

        binding.swipeRefresh.isRefreshing = false
    }

    private fun loadStudentBio() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.collection("StudentsBio").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.studentIdTextView.text = document.getString("studentId")
                        binding.departmentTextView.text = document.getString("department")
                        binding.nameTextView.text = document.getString("name")
                        binding.phoneTextView.text = document.getString("phone")
                        binding.bloodGroupTextView.text = document.getString("bloodGroup")
                        binding.dobTextView.text = document.getString("dob")
                        binding.descriptionTextView.text = document.getString("description")
                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            loadProfileImage(imageUrl)
                        } else {
                            Toast.makeText(this, "No profile image found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No bio found for this student.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load bio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.profileImageView)
    }

    private fun loadSelfIntroVideo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.collection("Videos").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val videoUrl = document.getString("videoUrl")
                    if (!videoUrl.isNullOrEmpty()) {
                        playVideo(videoUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_UPDATE && resultCode == RESULT_OK) {
            loadStudentBio()
        }
    }



    private fun playVideo(videoUrl: String) {
        binding.videoView.stopPlayback()
        binding.videoView.setVideoURI(Uri.parse(videoUrl))
        binding.videoView.setMediaController(MediaController(this))
        binding.videoView.start()
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            if (idx >= 0) {
                path = cursor.getString(idx)
            }
        }
        return path
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 100
    }

    override fun onResume() {
        super.onResume()
        refreshContent()
    }
}



    // Storing the image in firebase storage and the image url in firestore

//    private fun showImagePickerDialog() {
//        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//
//        val chooserIntent = Intent.createChooser(pickImageIntent, "Select or capture an image")
//        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureImageIntent))
//
//        imagePickerLauncher.launch(chooserIntent)
//    }
//
//    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//        if (result.resultCode == RESULT_OK) {
//            val data = result.data
//            val selectedImageUri = data?.data ?: data?.extras?.get("data") as Uri?
//            selectedImageUri?.let {
//                uploadImageToFirebase(it)
//            }
//        } else {
//            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun uploadImageToFirebase(imageUri: Uri) {
//        val userId = auth.currentUser?.uid ?: return
//        val profileImageRef = storage.reference.child("ProfileImages/$userId.jpg")
//
//        profileImageRef.putFile(imageUri)
//            .addOnSuccessListener {
//                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
//                    saveImageUrlToFirestore(downloadUri.toString())
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun saveImageUrlToFirestore(imageUrl: String) {
//        val userId = auth.currentUser?.uid ?: return
//        val profileData = mapOf("profileImageUrl" to imageUrl)
//
//        database.collection("Profile").document(userId)
//            .set(profileData)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Profile image saved", Toast.LENGTH_SHORT).show()
//                loadProfileImage() // Refresh the profile image
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Failed to save profile data", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun loadProfileImage() {
//        val userId = auth.currentUser?.uid ?: return
//
//        database.collection("Profile").document(userId)
//            .get()
//            .addOnSuccessListener { document ->
//                val imageUrl = document.getString("profileImageUrl")
//                if (!imageUrl.isNullOrEmpty()) {
//                    Glide.with(this)
//                        .load(imageUrl)
//                        .into(binding.profileImageView)
//                } else {
//                    Toast.makeText(this, "No profile image found", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
//            }
//    }

