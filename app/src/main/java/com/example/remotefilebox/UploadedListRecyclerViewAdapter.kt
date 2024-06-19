package com.example.remotefilebox;

import android.view.LayoutInflater
import android.view.View;
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UploadedListRecyclerViewAdapter(private val files: MutableList<String>) :
    RecyclerView.Adapter<UploadedListRecyclerViewAdapter.ListViewItemHolder>() {


    class ListViewItemHolder(itemView:View) : RecyclerView.ViewHolder(itemView){
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val textView: TextView = itemView.findViewById(R.id.textView)
        private val baseURL = "https://artandway.com/remote-filebox/"

        fun bind(fileUrl: String) {
            var orgURL = baseURL + fileUrl
            textView.text = fileUrl
            Glide.with(itemView.context)
                .load(orgURL)
                .into(imageView)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_itemview, parent, false)
        return ListViewItemHolder(view)
    }

    override fun onBindViewHolder(
        holder: ListViewItemHolder,
        position: Int
    ) {
        val fileUrl = files[position]
        holder.bind(fileUrl)
    }

    override fun getItemCount(): Int {
        return files.size
    }
}
