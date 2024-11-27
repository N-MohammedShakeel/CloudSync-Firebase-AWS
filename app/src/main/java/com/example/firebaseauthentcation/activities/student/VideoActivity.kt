package com.example.firebaseauthentcation.activities.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.example.firebaseauthentcation.AwsCredentials
import com.example.firebaseauthentcation.databinding.ActivityVideoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private var selectedVideoUri: Uri? = null

    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val VIDEO_PICK_CODE = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.selectVideoButton.setOnClickListener {
            openVideoPicker()
        }

        binding.deleteVideoButton.setOnClickListener {
            deleteVideoFromS3AndFirestore()
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "video/*"
        }
        startActivityForResult(intent, VIDEO_PICK_CODE)
    }

    private fun uploadVideoToS3(videoUri: Uri?) {
        if (videoUri == null) {
            Toast.makeText(this, "Video selection failed!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val fileName = "video_${UUID.randomUUID()}.mp4"
                val s3Path = "videos/$fileName"
                val file = File(getRealPathFromURI(videoUri))

                val s3Url = withContext(Dispatchers.IO) {
                    uploadToS3(s3Path, file)
                }

                storeVideoUrlInFirestore(s3Url)
            } catch (e: Exception) {
                Toast.makeText(this@VideoActivity, "Error uploading video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadToS3(s3Path: String, file: File): String {
        val s3Client = AmazonS3Client(
            BasicAWSCredentials(AwsCredentials.AWS_ACCESS_KEY, AwsCredentials.AWS_SECRET_KEY)
        )
        val transferUtility = TransferUtility.builder().s3Client(s3Client).context(applicationContext).build()

        val uploadObserver = transferUtility.upload(AwsCredentials.BUCKET_NAME, s3Path, file)

        return suspendCoroutine { continuation ->
            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        val videoUrl = s3Client.getUrl(AwsCredentials.BUCKET_NAME, s3Path).toString()
                        continuation.resume(videoUrl)
                    }
                }

                override fun onError(id: Int, ex: Exception?) {
                    continuation.resumeWithException(ex ?: Exception("Unknown error"))
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    // Optional: Show progress if desired
                }
            })
        }
    }

    private suspend fun storeVideoUrlInFirestore(videoUrl: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocRef = database.collection("Videos").document(userId)

            userDocRef.set(mapOf("videoUrl" to videoUrl), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Video uploaded successfully!", Toast.LENGTH_SHORT).show()
                    playVideo(videoUrl)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload video", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun playVideo(videoUrl: String) {
        val videoUri = Uri.parse(videoUrl)
        binding.videoPreviewView.setVideoURI(videoUri)
        binding.videoPreviewView.start()
    }

    private fun deleteVideoFromS3AndFirestore() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val userDocRef = database.collection("Videos").document(userId)

                    val document = userDocRef.get().await()
                    val videoUrl = document.getString("videoUrl")
                    if (videoUrl != null) {
                        val s3Path = getS3PathFromUrl(videoUrl)
                        deleteVideoFromS3(s3Path)
                        deleteVideoUrlFromFirestore(userDocRef)
                    } else {
                        Toast.makeText(this@VideoActivity, "No video URL found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@VideoActivity, "Error deleting video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun deleteVideoFromS3(s3Path: String) {
        val s3Client = AmazonS3Client(
            BasicAWSCredentials(AwsCredentials.AWS_ACCESS_KEY, AwsCredentials.AWS_SECRET_KEY)
        )
        withContext(Dispatchers.IO) {
            try {
                s3Client.deleteObject(AwsCredentials.BUCKET_NAME, s3Path)
                Toast.makeText(this@VideoActivity, "Video deleted from S3", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@VideoActivity, "Error deleting video from S3", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun deleteVideoUrlFromFirestore(userDocRef: DocumentReference) {
        try {
            userDocRef.update("videoUrl", FieldValue.delete()).await()
            Toast.makeText(this, "Video URL deleted from Firestore", Toast.LENGTH_SHORT).show()
            binding.videoPreviewView.stopPlayback() // Stop playback of the video
        } catch (e: Exception) {
            Toast.makeText(this, "Error deleting video URL from Firestore", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getS3PathFromUrl(videoUrl: String): String {
        // Extract the S3 path from the full URL
        val uri = Uri.parse(videoUrl)
        return uri.path?.substringAfter("videos/") ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VIDEO_PICK_CODE && resultCode == RESULT_OK) {
            selectedVideoUri = data?.data
            // Immediately upload the selected video to S3
            uploadVideoToS3(selectedVideoUri)
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
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

