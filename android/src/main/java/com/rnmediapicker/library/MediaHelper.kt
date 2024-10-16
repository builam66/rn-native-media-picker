package com.rnmediapicker.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.rnmediapicker.constants.DefaultConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaHelper(private val contentResolver: ContentResolver) {
  /**
   * Loads a list of folders from the device's media store.
   *
   * This function queries the media store for folders and media items. It first retrieves and creates special folders
   * for "AllMedia" and "AllVideos" with corresponding media counts and first media thumbnails. Then, it queries the media
   * store to get individual folders based on bucket IDs and names, and collects their media counts.
   *
   * @return A list of MediaFolderItem objects representing the folders and their media counts.
   */
  suspend fun loadFolders(): List<MediaFolderItem> = withContext(Dispatchers.IO) {
    val folderList = mutableListOf<MediaFolderItem>()
    val allMediaFolder = getFirstMediaUri()?.let {
      MediaFolderItem(
        folderName = "AllMedia",
        folderId = "all_media",
        folderPath = "",
        folderUri = it,
        mediaCount = getAllMediaCount()
      )
    }?.also { folderList.add(it) } // Add to folderList if not null

    val allVideosFolder = getFirstVideoUri()?.let {
      MediaFolderItem(
        folderName = "AllVideos",
        folderId = "all_videos",
        folderPath = "",
        folderUri = it,
        mediaCount = getAllVideosCount()
      )
    }?.also { folderList.add(it) } // Add to folderList if not null

    // Query for individual folders
    val projection = arrayOf(
      MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
      MediaStore.Files.FileColumns.BUCKET_ID,
      MediaStore.Files.FileColumns._ID,
      MediaStore.Files.FileColumns.MEDIA_TYPE
    )
    val selection =
      "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val selectionArgs = arrayOf(
      MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
      MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )
    val sortOrder =
      "${MediaStore.Files.FileColumns.BUCKET_ID} ASC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    val queryUri = MediaStore.Files.getContentUri("external")

    val cursor =
      contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
      val bucketIdColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
      val bucketNameColumn =
        it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
      val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
      val mediaTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

      val folderMap = mutableMapOf<String, MediaFolderItem>()

      while (it.moveToNext()) {
        val bucketId = it.getString(bucketIdColumn)
        val bucketName = it.getString(bucketNameColumn)
        val id = it.getLong(idColumn)
        val mediaType = it.getInt(mediaTypeColumn)

        if (!folderMap.containsKey(bucketId)) {
          val contentUri = when (mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
              Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
              )

            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
              Uri.withAppendedPath(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id.toString()
              )

            else -> Uri.parse("")
          }
          folderMap[bucketId] = MediaFolderItem(bucketName, bucketId, bucketId, contentUri)
        } else {
          folderMap[bucketId]?.mediaCount = folderMap[bucketId]?.mediaCount?.plus(1) ?: 1
        }
      }

      folderList.addAll(folderMap.values)

      // for (mediaFolderItem in folderList) {
      //   if (mediaFolderItem.folderId != "all_media" && mediaFolderItem.folderId != "all_videos") {
      //     mediaFolderItem.mediaCount = getMediaCount(mediaFolderItem.folderId)
      //   }
      // }
    }

    return@withContext folderList
  }

  suspend fun loadMedia(folderId: String): List<MediaItem> = withContext(Dispatchers.IO) {
    val mediaList = mutableListOf<MediaItem>()
    // Define the projection to retrieve the necessary columns
    val projection = arrayOf(
      MediaStore.Files.FileColumns._ID,
      MediaStore.Files.FileColumns.MEDIA_TYPE,
      MediaStore.Files.FileColumns.MIME_TYPE
    )

    // Define the selection criteria to filter by media types (images or videos)
    var selection: String? = null
    var selectionArgs: Array<String>? = null
    // Define the sort order to sort by the date added in descending order
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    // Define the URI for querying the external media content
    val queryUri = MediaStore.Files.getContentUri("external")

    when (folderId) {
      "all_media" -> { // Load all media (images and videos)
        selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        selectionArgs = arrayOf(
          MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
          MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
      }
      "all_videos" -> { // Load videos
        selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
      }
      else -> { // Load media files specific to the given folder ID
        selection =
          "${MediaStore.Files.FileColumns.BUCKET_ID}=? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        selectionArgs = arrayOf(
          folderId,
          MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
          MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
      }
    }

    // Perform the query to get the media files
    val cursor =
      contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
      // Get the column indices for ID and MIME type
      val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
      val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

      // Iterate through the cursor to retrieve media items
      while (it.moveToNext()) {
        val id = it.getLong(idColumn)
        val mimeType = it.getString(mimeTypeColumn)

        // Construct the content URI based on MIME type
        val contentUri = when {
          mimeType.startsWith(DefaultConstants.IMAGE) -> Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id.toString()
          )

          mimeType.startsWith(DefaultConstants.VIDEO) -> Uri.withAppendedPath(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id.toString()
          )

          else -> continue
        }

        // Determine the media type
        val mediaType =
          if (mimeType.startsWith(DefaultConstants.IMAGE)) MediaType.IMAGE else MediaType.VIDEO

        // Add the media item to the list
        mediaList.add(MediaItem(contentUri, mediaType))
      }
    }

    return@withContext mediaList
  }

  private fun getMediaCount(folderId: String): Int {
    // Define the projection to only retrieve the count of media items
    val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")

    // Define the selection criteria to filter by the specified folder ID
    val selection = "${MediaStore.Files.FileColumns.BUCKET_ID}=?"
    val selectionArgs = arrayOf(folderId)

    // Define the query URI for the external content
    val queryUri = MediaStore.Files.getContentUri("external")

    // Perform the query to get the count of media files in the specified folder
    val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

    // Extract the count from the cursor
    val count = cursor?.use {
      if (it.moveToFirst()) it.getInt(0) else 0
    }

    // Return the count, defaulting to 0 if no count is found
    return count ?: 0
  }

  private fun getAllMediaCount(): Int {
    // Define the projection to only retrieve the count of media items
    val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")

    // Define the selection criteria to filter for image and video files
    val selection =
      "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val selectionArgs = arrayOf(
      MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
      MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )

    // Define the query URI for the external content
    val queryUri = MediaStore.Files.getContentUri("external")

    // Perform the query to get the count of media files (images and videos)
    val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

    // Extract the count from the cursor
    val count = cursor?.use {
      if (it.moveToFirst()) it.getInt(0) else 0
    }

    // Return the count, defaulting to 0 if no count is found
    return count ?: 0
  }

  private fun getAllVideosCount(): Int {
    // Define the selection criteria to filter for video files
    val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")


    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    // Define the query URI for the external content
    val queryUri = MediaStore.Files.getContentUri("external")

    // Perform the query to get the count of video files
    val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

    // Extract the count from the cursor
    val count = cursor?.use {
      if (it.moveToFirst()) it.getInt(0) else 0
    }
    return count ?: 0
  }

  private suspend fun getFirstMediaUri(): Uri? {
    val selection =
      "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val selectionArgs = arrayOf(
      MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
      MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )
    return queryFirstMediaUri(selection, selectionArgs)
  }

//  private suspend fun getFirstMediaUriForAllMedia(): Uri? = withContext(Dispatchers.IO) {
//    val projection = arrayOf(
//      MediaStore.Files.FileColumns._ID,
//      MediaStore.Files.FileColumns.MEDIA_TYPE,
//      MediaStore.Files.FileColumns.MIME_TYPE
//    )
//    val selection =
//      "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
//    val selectionArgs = arrayOf(
//      MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
//      MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
//    )
//    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
//    val queryUri = MediaStore.Files.getContentUri("external")
//
//    val cursor =
//      contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)
//    cursor?.use {
//      if (it.moveToFirst()) {
//        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
//        val mimeTypeColumn =
//          it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
//
//        val id = it.getLong(idColumn)
//        val mimeType = it.getString(mimeTypeColumn)
//
//        return@withContext when {
//          mimeType.startsWith(DefaultConstants.IMAGE) -> Uri.withAppendedPath(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            id.toString()
//          )
//
//          mimeType.startsWith(DefaultConstants.VIDEO) -> Uri.withAppendedPath(
//            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//            id.toString()
//          )
//
//          else -> null
//        }
//      }
//    }
//    return@withContext null
//  }

  private suspend fun getFirstVideoUri(): Uri? {
    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    return queryFirstMediaUri(selection, selectionArgs, includeMimeType = false)
  }

//  private suspend fun getFirstMediaUriForAllVideos(): Uri? = withContext(Dispatchers.IO) {
//    val projection = arrayOf(
//      MediaStore.Files.FileColumns._ID,
//      MediaStore.Files.FileColumns.MIME_TYPE
//    )
//    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
//    val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
//    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
//    val queryUri = MediaStore.Files.getContentUri("external")
//
//    val cursor =
//      contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)
//    cursor?.use {
//      if (it.moveToFirst()) {
//        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
//        val id = it.getLong(idColumn)
//
//        return@withContext Uri.withAppendedPath(
//          MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//          id.toString()
//        )
//      }
//    }
//    return@withContext null
//  }

  private suspend fun queryFirstMediaUri(selection: String, selectionArgs: Array<String>, includeMimeType: Boolean = true): Uri? = withContext(Dispatchers.IO) {
    val projection = arrayOf(
      MediaStore.Files.FileColumns._ID,
      if (includeMimeType) MediaStore.Files.FileColumns.MIME_TYPE else null
    ).filterNotNull().toTypedArray() // Filter out null if includeMimeType is false

    val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    val queryUri = MediaStore.Files.getContentUri("external")

    contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use {
      if (it.moveToFirst()) {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val id = it.getLong(idColumn)

        return@withContext if (includeMimeType) {
          val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
          val mimeType = it.getString(mimeTypeColumn)

          when {
            mimeType.startsWith(DefaultConstants.IMAGE) -> Uri.withAppendedPath(
              MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
              id.toString()
            )
            mimeType.startsWith(DefaultConstants.VIDEO) -> Uri.withAppendedPath(
              MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
              id.toString()
            )
            else -> null
          }
        } else {
          Uri.withAppendedPath(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id.toString()
          )
        }
      }
    }
    return@withContext null
  }
}
