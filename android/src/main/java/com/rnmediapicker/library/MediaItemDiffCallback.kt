package com.rnmediapicker.library

import androidx.recyclerview.widget.DiffUtil

class MediaItemDiffCallback(
  private val oldList: List<MediaItem>,
  private val newList: List<MediaItem>
) : DiffUtil.Callback() {

  override fun getOldListSize(): Int {
    return oldList.size
  }

  override fun getNewListSize(): Int {
    return newList.size
  }

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition].uri == newList[newItemPosition].uri
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition] == newList[newItemPosition]
  }
}
