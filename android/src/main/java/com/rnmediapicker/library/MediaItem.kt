package com.rnmediapicker.library

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MediaItem(
  val uri: Uri?,
  val type: MediaType
) : Parcelable {

  constructor(parcel: Parcel) : this(
    parcel.readParcelable(Uri::class.java.classLoader),
    MediaType.values()[parcel.readInt()]
  )

  /**
   * Describe the contents of the Parcelable instance.
   *
   * @return An integer bitmask indicating the set of special object types
   *         marshaled by the Parcelable.
   */

  override fun describeContents(): Int {
    return 0
  }

  /**
   * Write the MediaItem's data to the provided Parcel.
   *
   * @param dest The Parcel in which the MediaItem should be written.
   * @param flags Additional flags about how the MediaItem should be written.
   */

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeParcelable(uri, flags)
    dest.writeInt(type.ordinal)
  }

  /**
   * Companion object for the Parcelable.Creator interface.
   * Used for creating instances of MediaItem from a Parcel.
   */

  companion object CREATOR : Parcelable.Creator<MediaItem> {

    /**
     * Create a new MediaItem instance from the provided Parcel.
     *
     * @param parcel The Parcel containing the MediaItem's data.
     * @return A new MediaItem instance.
     */

    override fun createFromParcel(parcel: Parcel): MediaItem {
      return MediaItem(parcel)
    }


    /**
     * Create a new array of MediaItem instances.
     *
     * @param size The size of the array to create.
     * @return An array of MediaItem instances.
     */

    override fun newArray(size: Int): Array<MediaItem?> {
      return arrayOfNulls(size)
    }
  }
}
