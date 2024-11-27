package com.example.firebaseauthentcation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebaseauthentcation.R
import com.example.firebaseauthentcation.model.Student

class StudentAdapter(private val students: List<Student>) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_list, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.nameTextView.text = student.name
        holder.studentIdTextView.text = student.studentId
        Glide.with(holder.profileImageView.context)
            .load(student.imageUrl)
            .placeholder(R.drawable.ic_launcher_background) // Replace with your placeholder image
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int = students.size

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val profileImageView: ImageView = itemView.findViewById(R.id.studentProfileImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.studentNameTextView)
        val studentIdTextView: TextView = itemView.findViewById(R.id.studentIdTextView)
    }
}
