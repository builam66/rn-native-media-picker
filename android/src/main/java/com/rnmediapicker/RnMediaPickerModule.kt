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
import com.rnmediapicker.constants.DefaultConstants
import com.rnmediapicker.constants.IntentConstants
import com.rnmediapicker.constants.ResultConstants
import com.rnmediapicker.library.LibraryActivity
import com.rnmediapicker.library.MediaAsset
import com.rnmediapicker.library.MediaPickerResponse

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
        val response: MediaPickerResponse? = data.getParcelableExtra(IntentConstants.SELECTED_ITEMS)
        libraryPickerPromise?.resolve(convertResponse(ResultConstants.SUCCESS, response))
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

  private fun convertResponse(resultCode: Int, response: MediaPickerResponse?): ReadableMap {
    val assets = Arguments.createArray().apply {
      response?.assets?.forEach { item ->
        // Convert MediaAsset to ReadableMap
        val readableMap = Arguments.createMap()
        // Use reflection or data class introspection to iterate through properties
        for (property in MediaAsset::class.java.declaredFields) {
          property.isAccessible = true
          val propertyName = property.name
          // Add property to ReadableMap based on its type
          when (val propertyValue = property.get(item)) {
            is String -> readableMap.putString(propertyName, propertyValue)
            is Int -> readableMap.putInt(propertyName, propertyValue)
            is Double -> readableMap.putDouble(propertyName, propertyValue)
            is Boolean -> readableMap.putBoolean(propertyName, propertyValue)
            // Handle other types as needed
            null -> readableMap.putNull(propertyName)
          }
        }
        // Add the ReadableMap to the assets array
        pushMap(readableMap)
      }
    }
    return Arguments.createMap().apply {
      putInt(DefaultConstants.RES_RESULT_CODE, resultCode)
      putArray(DefaultConstants.RES_ASSETS, assets)
    }
  }

  companion object {
    const val NAME = "RnMediaPicker"
    const val LAUNCH_LIBRARY_REQUEST = 1
  }
}
