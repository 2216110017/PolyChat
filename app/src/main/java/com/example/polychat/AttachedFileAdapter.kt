package com.example.polychat

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AttachedFileAdapter(private val files: ArrayList<Uri>, private val onDelete: (Uri) -> Unit) : RecyclerView.Adapter<AttachedFileAdapter.FileViewHolder>() {
    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attached_file_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val uri = files[position]
        holder.imageView.setImageURI(uri)
        Log.e("AttachedFileAdapter","uri : $uri")
        holder.deleteButton.setOnClickListener {
            onDelete(uri)
            Log.e("AttachedFileAdapter","uri : $uri")
        }
    }

    fun addFile(fileUri: Uri) {
        files.add(fileUri)
        notifyItemInserted(files.size - 1)
    }

    override fun getItemCount(): Int = files.size
}
