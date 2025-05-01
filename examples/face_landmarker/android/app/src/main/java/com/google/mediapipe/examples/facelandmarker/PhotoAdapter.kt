package com.google.mediapipe.examples.facelandmarker

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage

class PhotoAdapter(
    private val photos: List<PhotoData>,
    private val onPhotoClick: (PhotoData) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private val storage = FirebaseStorage.getInstance()

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewDistracted)
        val timeText: TextView = view.findViewById(R.id.textViewPhotoTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // Set time text
        holder.timeText.text = "${photo.date} ${photo.time}"

        // Load image from Firebase Storage
        photo.photoUrl?.let { gsUrl ->
            if (gsUrl.startsWith("gs://")) {
                val storageRef = storage.getReferenceFromUrl(gsUrl)

                // Show loading placeholder
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)

                // Get download URL and load image with Glide
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(holder.imageView.context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(holder.imageView)
                }.addOnFailureListener { e ->
                    Log.e("PhotoAdapter", "Error getting download URL: ${e.message}")
                    holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } ?: run {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            onPhotoClick(photo)
        }
    }

    override fun getItemCount() = photos.size
}