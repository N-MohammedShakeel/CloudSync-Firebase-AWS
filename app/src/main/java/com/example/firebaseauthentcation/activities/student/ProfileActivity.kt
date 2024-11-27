package com.example.firebaseauthentcation.activities.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.bumptech.glide.Glide
import com.example.firebaseauthentcation.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.example.firebaseauthentcation.AwsCredentials
import com.example.firebaseauthentcation.R

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val s3Client = AmazonS3Client(
        BasicAWSCredentials(AwsCredentials.AWS_ACCESS_KEY, AwsCredentials.AWS_SECRET_KEY)
    )

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.uploadImageButton.setOnClickListener {
            uploadImageToS3(selectedImageUri)
        }

        binding.deleteImageButton.setOnClickListener {
            deleteImage()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun uploadImageToS3(imageUri: Uri?) {
        if (imageUri == null) {
            Toast.makeText(this, "Image selection failed!", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "profile_${UUID.randomUUID()}.jpg"
        val s3Path = "profiles/$fileName"
        val file = File(getRealPathFromURI(imageUri))

        val s3Client = AmazonS3Client(
            BasicAWSCredentials(AwsCredentials.AWS_ACCESS_KEY, AwsCredentials.AWS_SECRET_KEY)
        )
        val transferUtility = TransferUtility.builder().s3Client(s3Client).context(applicationContext).build()

        transferUtility.upload(AwsCredentials.BUCKET_NAME, s3Path, file)
            .setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        val imageUrl = s3Client.getUrl(AwsCredentials.BUCKET_NAME, s3Path).toString()
                        storeImageUrlInFirestore(imageUrl)
                    }
                }

                override fun onError(id: Int, ex: Exception?) {
                    Toast.makeText(this@ProfileActivity, "Error uploading image", Toast.LENGTH_SHORT).show()
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                }
            })
    }

    private fun storeImageUrlInFirestore(imageUrl: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = database.collection("StudentsBio").document(userId)

            userDocRef.set(mapOf("imageUrl" to imageUrl), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    loadProfileImage(imageUrl)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload profile image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteImage() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = database.collection("StudentsBio").document(userId)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        val s3Path = imageUrl.substringAfter("profiles/")
                        Log.d("ProfileActivity", "S3 path: $s3Path")

                        GlobalScope.launch(Dispatchers.Main) {
                            try {
                                withContext(Dispatchers.IO) {
                                    deleteImageFromS3(s3Path)
                                }
                                deleteImageFromFirestore()
                            } catch (e: Exception) {
                                Toast.makeText(this@ProfileActivity, "Error deleting image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "No image to delete", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error retrieving image URL", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun deleteImageFromS3(s3Path: String) {
        try {
            withContext(Dispatchers.IO) {
                s3Client.deleteObject(AwsCredentials.BUCKET_NAME, s3Path)
            }
            Toast.makeText(this, "Image deleted from S3", Toast.LENGTH_SHORT).show()
        } catch (e: AmazonS3Exception) {
            Log.e("ProfileActivity", "Error deleting image from S3", e)
            Toast.makeText(this, "Error deleting image from S3", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteImageFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = database.collection("StudentsBio").document(userId)
            userDocRef.update("imageUrl", FieldValue.delete())
                .addOnSuccessListener {
                    Toast.makeText(this, "Image URL deleted from Firestore", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Error deleting image URL from Firestore",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.profileImageView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileImageFromFirestore()
    }

    private fun loadProfileImageFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = database.collection("StudentsBio").document(userId)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    val imageUrl = document.getString("imageUrl")
                    imageUrl?.let {
                        loadProfileImage(it)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex != -1) {
                        path = it.getString(columnIndex)
                    }
                }
                it.close()
            }
        } else if (uri.scheme == "file") {
            path = uri.path ?: ""
        }

        return path
    }
}

