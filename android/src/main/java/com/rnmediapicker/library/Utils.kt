package com.rnmediapicker.library

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

  suspend fun getMediaCreationDate(contentResolver: ContentResolver, uri: Uri): String = withContext(
    Dispatchers.IO) {
    val today = Date()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    var date = "Unknown"
    val projection = arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
    val cursor = contentResolver.query(uri, projection, null, null, null)

    cursor?.use {
      if (it.moveToFirst()) {
        val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED))
        val mediaDate = Date(dateAdded * 1000) // Convert from seconds to milliseconds

        val mediaDateStr = dateFormat.format(mediaDate)
        val todayStr = dateFormat.format(today)

        date = when (mediaDateStr) {
          todayStr -> "Today"
          dateFormat.format(Date(today.time - 86400000)) -> "Yesterday" // 86400000ms = 1 day
          else -> displayFormat.format(mediaDate)
        }
      }
    }
    date
  }

  fun createVideoThumbnail(context: Context, uri: Uri): Bitmap? {
    val filePath = getRealPathFromURI(context, uri)
    return filePath?.let {
      ThumbnailUtils.createVideoThumbnail(it, MediaStore.Video.Thumbnails.MINI_KIND)
    }
  }

  fun getVideoDuration(context: Context, uri: Uri): String {
    var duration = "00:00"
    val mediaPlayer = MediaPlayer()

    try {
      mediaPlayer.setDataSource(context, uri)
      mediaPlayer.prepare()
      val durationMs = mediaPlayer.duration
      val minutes = (durationMs / (1000 * 60)) % 60
      val seconds = (durationMs / 1000) % 60
      duration = String.format("%02d:%02d", minutes, seconds)
    } catch (e: IOException) {
      e.printStackTrace()
    } finally {
      mediaPlayer.release()
    }

    return duration
  }

  private fun getRealPathFromURI(context: Context, uri: Uri): String? {
    var result: String? = null
    val cursor = context.contentResolver.query(
      uri,
      arrayOf(MediaStore.Images.ImageColumns.DATA),
      null,
      null,
      null
    )
    cursor?.use {
      if (it.moveToFirst()) {
        val idx = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        result = it.getString(idx)
      }
    }
    return result
  }
}
