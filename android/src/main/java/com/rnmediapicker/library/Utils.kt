package com.rnmediapicker.library

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.rnmediapicker.R
import com.rnmediapicker.constants.DefaultConstants
import com.rnmediapicker.constants.ResultConstants
import com.rnmediapicker.enums.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
  suspend fun getMediaCreationDate(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val contentResolver: ContentResolver = context.contentResolver
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
          todayStr -> context.getString(R.string.today)
          dateFormat.format(Date(today.time - 86400000)) -> context.getString(R.string.yesterday) // 86400000ms = 1 day
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

  fun getParcelableResponse(context: Context, mediaItems: List<MediaItem>): MediaPickerResponse {
    val assets = mediaItems.mapNotNull { item ->
      if (item.uri != null) {
        val mediaMetadata: Array<String> = if (item.type == MediaType.IMAGE) {
          getImageMetadata(context, item.uri)
        } else {
          getVideoMetadata(context, item.uri)
        }

        MediaAsset(
          uri = item.uri.toString(),
          type = item.type.toString(),
          mimeType = getMimeType(context, item.uri) ?: "",
          name = item.uri.lastPathSegment ?: "",
          size = getSize(context, item.uri),
          width = mediaMetadata[0].toDoubleOrNull() ?: 0.0,
          height = mediaMetadata[1].toDoubleOrNull() ?: 0.0,
          datetime = mediaMetadata[2],
          duration = if (item.type == MediaType.VIDEO) mediaMetadata[3].toDoubleOrNull() else null,
          bitrate = if (item.type == MediaType.VIDEO) mediaMetadata[4].toDoubleOrNull() else null
        )
      } else {
        null
      }
    }

    return MediaPickerResponse(
      resultCode = ResultConstants.SUCCESS,
      assets = assets,
    )
  }

  fun getReadableMapResponse(context: Context, mediaItems: List<MediaItem>): ReadableMap {
    val assets = Arguments.createArray().apply {
      mediaItems.forEach { item ->
        if (item.uri != null) {
          pushMap(Arguments.createMap().apply {
            putString("uri", item.uri.toString())
            putString("type", item.type.toString())
            putString("mimeType", getMimeType(context, item.uri))
            putString("name", item.uri.lastPathSegment)
            putDouble("size", getSize(context, item.uri))

            if (item.type == MediaType.IMAGE) {
              val imageMetadata = getImageMetadata(context, item.uri)
              putString("width", imageMetadata[0])
              putString("height", imageMetadata[1])
              putString("datetime", imageMetadata[2])
            }

            if (item.type == MediaType.VIDEO) {
              val videoMetadata = getVideoMetadata(context, item.uri)
              putString("width", videoMetadata[0])
              putString("height", videoMetadata[1])
              putString("datetime", videoMetadata[2])
              putString("duration", videoMetadata[3])
              putString("bitrate", videoMetadata[4])
            }
          })
        }
      }
    }

    return Arguments.createMap().apply {
      putArray(DefaultConstants.RES_ASSETS, assets)
    }
  }

  private fun getMimeType(context: Context?, uri: Uri): String? {
    if (context == null || uri.scheme == "file") {
      return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
    }

    return context.contentResolver.getType(uri)
  }

  private fun getSize(context: Context?, uri: Uri): Double {
    try {
      if (context == null) return 0.0
      context.contentResolver.openFileDescriptor(uri, "r").use { f ->
        if (f == null) return 0.0
        return f.statSize.toDouble()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return 0.0
    }
  }

  private fun getVideoMetadata(context: Context, uri: Uri): Array<String> {
    var width = "0"
    var height = "0"
    var datetime = ""
    var duration = "0"
    var bitrate = "0"

    try {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(context, uri)

      datetime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) ?: ""

      val widthData = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val heightData = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      if (widthData != null && heightData != null) {
        val rotationData =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val rotation = rotationData?.toInt() ?: 0
        if (rotation == 90 || rotation == 270) {
          width = heightData
          height = widthData
        } else {
          width = widthData
          height = heightData
        }
      }
      duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
      bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: "0"

      retriever.release()
    } catch (e: RuntimeException) {
      e.printStackTrace()
    }

    return arrayOf(width, height, datetime, duration, bitrate)
  }

  private fun getImageMetadata(context: Context, uri: Uri): Array<String> {
    var width = "0"
    var height = "0"
    var datetime = ""

    try {
      val options = BitmapFactory.Options()
      options.inJustDecodeBounds = true
      val inputStream = context.contentResolver.openInputStream(uri)
      if (inputStream != null) {
        val exif = ExifInterface(inputStream)
        datetime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: ""
        width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) ?: ""
        height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) ?: ""
      }
      inputStream?.close()
    } catch (e: RuntimeException) {
      e.printStackTrace()
    }

    return arrayOf(width, height, datetime)
  }
}
