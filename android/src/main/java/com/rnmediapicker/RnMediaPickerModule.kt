package com.rnmediapicker

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.rnmediapicker.constants.DefaultConstants
import com.rnmediapicker.constants.IntentConstants
import com.rnmediapicker.constants.ResultConstants
import com.rnmediapicker.library.LibraryActivity
import com.rnmediapicker.library.MediaItem

class RnMediaPickerModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var libraryPickerPromise: Promise? = null

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  @ReactMethod
  fun launchLibrary(libraryOptions: ReadableMap, resultPromise: Promise) {
    val currentActivity = currentActivity ?: return resultPromise.resolve(convertResponse(ResultConstants.LIBRARY_NOT_LAUNCH, null))

    currentActivity.let { activity ->
      libraryPickerPromise = resultPromise
      val intent = createLibraryIntent(activity, libraryOptions)
      activity.startActivityForResult(intent, LAUNCH_LIBRARY_REQUEST)
    }
  }

  private val activityEventListener = object : BaseActivityEventListener() {
    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
      if (libraryPickerPromise == null) return

      if (requestCode != LAUNCH_LIBRARY_REQUEST) {
        libraryPickerPromise?.resolve(convertResponse(ResultConstants.LIBRARY_NOT_LAUNCH, null))
        libraryPickerPromise = null
        return
      }

      if (resultCode == Activity.RESULT_OK && data != null) {
        val selectedItems: ArrayList<MediaItem> = data.getParcelableArrayListExtra(IntentConstants.SELECTED_ITEMS) ?: arrayListOf()
        libraryPickerPromise?.resolve(convertResponse(ResultConstants.SUCCESS, selectedItems))
      } else {
        libraryPickerPromise?.resolve(convertResponse(ResultConstants.OTHER_ERROR, null))
      }
      libraryPickerPromise = null
    }
  }

  init {
    // add the activity event listener to the react context
    reactContext.addActivityEventListener(activityEventListener)
  }

  private fun createLibraryIntent(activity: Activity, libraryOptions: ReadableMap): Intent {
    val intent = Intent(activity, LibraryActivity::class.java)
    val pickerOptions = mapToPickerOptions(libraryOptions)
    intent.putExtra(IntentConstants.PICKER_OPTIONS, pickerOptions)
    return intent
  }

  private fun mapToPickerOptions(options: ReadableMap): RnMediaPickerOptions {
    val (mediaType, isMultipleSelection, maxSelection) = options.run {
      Triple(
        getString(DefaultConstants.MEDIA_TYPE) ?: DefaultConstants.MEDIA_TYPE_ALL,
        getBoolean(DefaultConstants.IS_MULTIPLE_SELECTION),
        getInt(DefaultConstants.MAX_SELECTION)
      )
    }
    return RnMediaPickerOptions(
      mediaType = mediaType,
      isMultipleSelection = isMultipleSelection,
      maxSelection = maxSelection
    )
  }

  private fun convertResponse(resultCode: Int, mediaItems: List<MediaItem>?): WritableMap {
    val assets = Arguments.createArray().apply {
      mediaItems?.forEach { item ->
        pushMap(Arguments.createMap().apply {
          putString("mediaUri", item.uri.toString())
          putString("mediaType", item.type.toString())
        })
      }
    }
    return Arguments.createMap().apply {
      putInt("resultCode", resultCode)
      putArray("assets", assets)
    }
  }

  companion object {
    const val NAME = "RnMediaPicker"
    const val LAUNCH_LIBRARY_REQUEST = 1
  }
}
