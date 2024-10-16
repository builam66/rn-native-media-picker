package com.rnmediapicker.library

import android.net.Uri

data class MediaFolderItem(
  val folderName: String,
  val folderId: String,
  val folderPath: String,
  val folderUri: Uri,
  var mediaCount: Int = 0
)
