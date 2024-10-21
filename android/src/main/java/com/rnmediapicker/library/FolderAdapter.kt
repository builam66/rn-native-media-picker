package com.rnmediapicker.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rnmediapicker.R
import com.rnmediapicker.databinding.FolderItemBinding

class FolderAdapter(
    private val folderList: List<MediaFolderItem>,
    private var selectedFolderId: String?,
    private val onFolderClick: (MediaFolderItem) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    /**
     * Creates and returns a ViewHolder for folder items.
     * @param parent The parent ViewGroup.
     * @param viewType The type of view.
     */


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = FolderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    /**
     * Binds the folder item to the ViewHolder.
     * @param holder The ViewHolder for the folder item.
     * @param position The position of the folder in the list.
     */

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folderItem = folderList[position]
        holder.bind(folderItem)
    }

    /**
     * Returns the total number of folder items in the list.
     * @return The size of the folder list.
     */

    override fun getItemCount(): Int = folderList.size

    /**
     * ViewHolder class for folder items.
     * @param binding The view binding for the folder item layout.
     */

    inner class FolderViewHolder(private val binding: FolderItemBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a folder item to the ViewHolder and sets up click listeners.
         * @param folderItem The folder item to bind.
         */

        fun bind(folderItem: MediaFolderItem) {

            // Set the folder name and media count in the corresponding TextViews
            binding.folderName.text = folderItem.folderName
            binding.mediaCount.text = "${folderItem.mediaCount} items"

            // Load the folder thumbnail image using Glide with a placeholder
            Glide.with(binding.folderThumbnail.context)
                .load(folderItem.folderUri)
                .placeholder(R.drawable.folder_placeholder) // Placeholder image
                .into(binding.folderThumbnail)


            // Show or hide the tick icon based on whether the folder is selected
            binding.tickIcon.visibility = if (folderItem.folderId == selectedFolderId) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Handle folder item click events
            binding.root.setOnClickListener {

                // Update the selected folder ID and notify the adapter to refresh the UI
                selectedFolderId = folderItem.folderId
                notifyDataSetChanged()

                // Trigger the callback for the folder click event
                onFolderClick(folderItem)
            }
        }
    }
}
