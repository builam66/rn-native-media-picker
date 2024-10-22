package com.rnmediapicker.library

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rnmediapicker.databinding.MediaItemBinding
import com.rnmediapicker.enums.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaAdapter(
  private val context: Context,
  private var mediaList: List<MediaItem>,
  private val onItemSelected: (MediaItem) -> Unit,
  private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

  companion object {
    private const val THUMBNAIL_SIZE = 300
    private const val MAX_SELECTION_DEFAULT = 10
  }

  private val selectedItems = mutableSetOf<Uri>()
  private val thumbnailCache = mutableMapOf<Uri, Bitmap?>()
  private val videoDurationCache = mutableMapOf<Uri, String>()
  private var isMultiSelectMode = false
  private var maxSelection = 1

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
    val binding = MediaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return MediaViewHolder(binding)
  }

  override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
    val mediaItem = mediaList[position]
    holder.bind(mediaItem)

    val isItemSelectable = selectedItems.size < MAX_SELECTION_DEFAULT || selectedItems.contains(mediaItem.uri)

    holder.itemView.isClickable = isItemSelectable
    holder.itemView.isEnabled = isItemSelectable

    // Optionally, adjust the appearance of non-selectable items.
    holder.itemView.alpha = if (isItemSelectable) 1.0f else 0.5f
  }

  override fun getItemCount() = mediaList.size

  fun getMediaItemAt(position: Int): MediaItem? {
    return if (position in mediaList.indices) mediaList[position] else null
  }

  fun clearMediaItems() {
    mediaList = emptyList()
    notifyDataSetChanged()
  }

  fun updateMediaItems(newMediaList: List<MediaItem>) {
    mediaList = newMediaList
    notifyDataSetChanged()
  }

  inner class MediaViewHolder(private val binding: MediaItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(mediaItem: MediaItem) {
      if (mediaItem.type == MediaType.VIDEO) {
        binding.videoIcon.visibility = View.VISIBLE
        binding.videoDuration.visibility = View.VISIBLE
        mediaItem.uri?.let { loadVideoThumbnail(it) }
        mediaItem.uri?.let { loadVideoDuration(it) }
      } else {
        Glide.with(context)
          .load(mediaItem.uri)
          .override(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
          .into(binding.mediaImage)
        binding.videoIcon.visibility = View.GONE
        binding.videoDuration.visibility = View.GONE
      }

      // Determine if the item should be selectable
      val isItemSelectable = isMultiSelectMode || selectedItems.isEmpty() || selectedItems.contains(mediaItem.uri)

      // Update the checkbox state based on the current selection and max selection limit
      binding.checkIcon.isEnabled = selectedItems.size < maxSelection || selectedItems.contains(mediaItem.uri)
      binding.checkIcon.alpha = if (binding.checkIcon.isEnabled) 1.0f else 0.5f // Visually disable the checkbox if not enabled

      // Update the checkbox state based on the current selection
      updateSelectionState(mediaItem.uri)

      // Handle checkbox click
      binding.checkIcon.setOnClickListener {
        if (binding.checkIcon.isEnabled) {
          mediaItem.uri?.let { it1 -> toggleSelection(it1) }
        } else {
          // Toast.makeText(context, "You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
        }
      }

      binding.root.setOnClickListener {
        if (binding.checkIcon.isEnabled) {
          mediaItem.uri?.let { it1 -> toggleSelection(it1) }
          onItemSelected(mediaItem)
        } else {
          // Toast.makeText(context, "You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
        }
      }
    }

    private fun updateSelectionState(uri: Uri?) {
      if (uri == null) return

      val isSelected = selectedItems.contains(uri)
      binding.mediaOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
      binding.checkIcon.isChecked = isSelected
    }

    private fun loadVideoThumbnail(uri: Uri) {
      if (thumbnailCache.containsKey(uri)) {
        binding.mediaImage.setImageBitmap(thumbnailCache[uri])
      } else {
        CoroutineScope(Dispatchers.IO).launch {
          val bitmap = Utils.createVideoThumbnail(context, uri)
          thumbnailCache[uri] = bitmap

          withContext(Dispatchers.Main) {
            binding.mediaImage.setImageBitmap(bitmap)
          }
        }
      }
    }

    private fun loadVideoDuration(uri: Uri) {
      if (videoDurationCache.containsKey(uri)) {
        binding.videoDuration.text = videoDurationCache[uri]
      } else {
        getVideoDurationAsync(uri) { duration ->
          videoDurationCache[uri] = duration
          binding.videoDuration.text = duration
        }
      }
    }

    private fun getVideoDurationAsync(uri: Uri, callback: (String) -> Unit) {
      CoroutineScope(Dispatchers.IO).launch {
        val duration = Utils.getVideoDuration(context, uri)
        withContext(Dispatchers.Main) {
          callback(duration)
        }
      }
    }

    private fun toggleSelection(uri: Uri) {
      if (isMultiSelectMode) {
        // Multi-selection mode logic
        if (selectedItems.contains(uri)) {
          // Remove the item if it's already selected
          selectedItems.remove(uri)
        } else {
          // Add the item if the number of selected items is less than the max selection limit
          if (selectedItems.size < maxSelection) {
            selectedItems.add(uri)
          } else {
            // Toast.makeText(context,"You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
            return
          }
        }
      } else {
        // Single-selection mode logic
        if (selectedItems.contains(uri)) {
          // Deselect the item if it's already selected
          selectedItems.remove(uri)
        } else {
          // Check if there's already a selected item
          // if (selectedItems.isNotEmpty()) {
          //   Toast.makeText(context,"Only $maxSelection item can be selected at a time.", Toast.LENGTH_SHORT).show()
          //   return
          // }
          // Clear previous selections and select the new item
          selectedItems.clear()
          selectedItems.add(uri)
        }
      }
      onSelectionChanged(selectedItems.size)
      notifyDataSetChanged()
    }
  }

  fun getSelectedItems(): List<MediaItem> {
    return mediaList.filter { selectedItems.contains(it.uri) }
  }

  fun setSelectionMode(isMultiSelectMode: Boolean, maxSelection: Int) {
    if (this.isMultiSelectMode != isMultiSelectMode) {
      selectedItems.clear()
      onSelectionChanged(0)
    }
    this.isMultiSelectMode = isMultiSelectMode
    this.maxSelection = maxSelection
    notifyDataSetChanged()
  }

  fun setSelectedItems(items: List<MediaItem>) {
    selectedItems.clear() // Clear existing selections
    // Add the selected items URIs to the selected items set
    selectedItems.addAll(items.mapNotNull { it.uri })
    // Notify the adapter to refresh the views
    notifyDataSetChanged()
    // Notify about the selection count change
    onSelectionChanged(selectedItems.size)
  }
}
