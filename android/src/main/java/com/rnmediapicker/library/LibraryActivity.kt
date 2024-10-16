package com.rnmediapicker.library

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rnmediapicker.RnMediaPickerOptions
import com.rnmediapicker.constants.IntentConstants
import com.rnmediapicker.databinding.LibraryActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : AppCompatActivity() {
  private lateinit var viewBinding: LibraryActivityBinding
  private lateinit var mediaAdapter: MediaAdapter
  private lateinit var mediaHelper: MediaHelper
  private var folderList = mutableListOf<MediaFolderItem>()
  private var isMultipleSelection = false
  private var maxSelection: Int = 1
  private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val requestPermissionsLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      val allGranted = permissions.values.all { it }
      val isGranted = isGrantedPermissions()
      if (isGranted) {
        activityScope.launch(Dispatchers.Main) {
          folderList = mediaHelper.loadFolders().toMutableList()

          mediaAdapter.clearMediaItems()
          val mediaList = mediaHelper.loadMedia(folderList.firstOrNull()?.folderId ?: "")
          mediaAdapter.updateMediaItems(mediaList)
        }
      } else {
        Log.e("Permission", "Permission denied!")
        // showPermissionDeniedDialog()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    mediaHelper = MediaHelper(contentResolver)
    // load input options
    val options: RnMediaPickerOptions? = intent.getParcelableExtra(IntentConstants.PICKER_OPTIONS)
    if (options != null) {
      isMultipleSelection = options.isMultipleSelection
      maxSelection = options.maxSelection
    }

    // using viewBinding to inflate library
    viewBinding = LibraryActivityBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    // initialize the adapter and click
    viewBinding.mediaRecyclerView.layoutManager = GridLayoutManager(this, 3)
    mediaAdapter = MediaAdapter(this, emptyList(), { mediaItem ->
      Log.d("MediaAdapter", "Click" + mediaItem.uri)
    }, { selectedCount ->
      // Update the done button and selected count when the selection changes
      updateDoneButton(selectedCount)
    })
    viewBinding.mediaRecyclerView.adapter = mediaAdapter

    // initialize multi-select mode and change-mode click
    // isMultipleSelection = true
    updateSelectionMode()
    // viewBinding.mcvSelectMultipleSingle.setOnClickListener {
    //   isMultipleSelection = !isMultipleSelection
    //   updateSelectionMode()
    // }

    // initialize back button click
    viewBinding.tvBack.setOnClickListener {
      finish()
    }

    // get and set items from intent data
    val receivedItems: ArrayList<MediaItem> =
      intent.getParcelableArrayListExtra(IntentConstants.SELECTED_ITEMS) ?: arrayListOf()
    mediaAdapter.setSelectedItems(receivedItems)

    viewBinding.tvDone.setOnClickListener {
      // Get selected items from the adapter
      val selectedItems = mediaAdapter.getSelectedItems()
      val selectedItemsArrayList = ArrayList(selectedItems)

      // Log the selected items for debugging
      Log.e(TAG, "onCreate: $selectedItemsArrayList")

      // Create an intent and put the selected items
      val resultIntent = Intent().apply {
        putParcelableArrayListExtra(IntentConstants.SELECTED_ITEMS, selectedItemsArrayList)
      }

      // Set the result and finish the activity
      setResult(Activity.RESULT_OK, resultIntent)
      finish()
    }

    if (isGrantedPermissions()) {
      activityScope.launch(Dispatchers.Main) {
        folderList = mediaHelper.loadFolders().toMutableList()

        mediaAdapter.clearMediaItems()
        val mediaList = mediaHelper.loadMedia("all_media")
        mediaAdapter.updateMediaItems(mediaList)
      }
    } else {
      requestPermissions()
    }

//    viewBinding.selectedFolderName.setOnClickListener {
//      showFolderSelectionDialog()
//    }
//    viewBinding.selectedFolderName.text = selectedFolderName

    viewBinding.mediaRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        // Get the first visible item position
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()


        if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
          // Get the media item from the adapter based on the position
          val mediaItem = mediaAdapter.getMediaItemAt(firstVisibleItemPosition)

          // Update the date TextView based on the media item
          mediaItem?.let {
            it.uri?.let { uri ->
              // Pass the contentResolver along with the uri
              updateDateTextView(contentResolver, uri)
            }
          }
        }

      }
    })
  }

  private fun updateDoneButton(selectedCount: Int) {
    if (selectedCount == 0) {
      viewBinding.tvDone.isClickable = false
      // viewBinding.tvDone.text = "Post"
    } else {
      // Enable the button, change its color back to default, and show selected count
      viewBinding.tvDone.isClickable = true
      // viewBinding.tvDone.text = "Post ($selectedCount/$maxSelection)" // Text with selected count
    }
  }

   private fun updateSelectionMode() {
     // if (isMultipleSelection) {
     //   viewBinding.tvSelectSingleMulti.text = "Select Single"
     //   maxSelection = 10 // Maximum selection for multi-select mode
     // } else {
     //   viewBinding.tvSelectSingleMulti.text = "Select Multiple"
     //   maxSelection = 1 // Maximum selection for single-select mode
     // }

     // Get the current selected count and update the done button
     val selectedCount = mediaAdapter.getSelectedItems().size
     updateDoneButton(selectedCount)
     mediaAdapter.setSelectionMode(isMultipleSelection, maxSelection)
   }

  fun updateDateTextView(contentResolver: ContentResolver, uri: Uri) {
    activityScope.launch(Dispatchers.IO) {
      val date = Utils.getMediaCreationDate(contentResolver, uri)
      withContext(Dispatchers.Main) {
        viewBinding.tvDate.text = date
      }
    }
  }

  private fun isGrantedPermissions(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
      && (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED)
    ) {
      // full access android 13 and above
      return true
    } else if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    ) {
      // partial access on android 14 and above
      return true
    } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    ) {
      // full access android 12 and below
      return true
    } else {
      // access denied
      return false
    }
  }

  private fun requestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      // android 14 and above
      requestPermissionsLauncher.launch(
        arrayOf(
          Manifest.permission.READ_MEDIA_IMAGES,
          Manifest.permission.READ_MEDIA_VIDEO,
          // Manifest.permission.READ_MEDIA_AUDIO,
          Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
      )
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
      // android 13 and above
      requestPermissionsLauncher.launch(
        arrayOf(
          Manifest.permission.READ_MEDIA_IMAGES,
          Manifest.permission.READ_MEDIA_VIDEO,
          // Manifest.permission.READ_MEDIA_AUDIO
        )
      )
    } else {
      // android 12
      requestPermissionsLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    activityScope.cancel()
  }
}