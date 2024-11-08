package com.rnmediapicker.library

import android.os.Parcel
import android.os.Parcelable

data class MediaAsset(
  val uri: String,
  val type: String,
  val mimeType: String,
  val name: String,
  val size: Double,
  val width: Double,
  val height: Double,
  val datetime: String?,
  val duration: Double?,
  val bitrate: Double?
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readString() ?: "",
    parcel.readString()  ?: "",
    parcel.readString()  ?: "",
    parcel.readString()  ?: "",
    parcel.readDouble(),
    parcel.readDouble(),
    parcel.readDouble(),
    parcel.readString(),
    parcel.readValue(Double::class.java.classLoader) as? Double,
    parcel.readValue(Double::class.java.classLoader) as? Double
  ) {
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(uri)
    parcel.writeString(type)
    parcel.writeString(mimeType)
    parcel.writeString(name)
    parcel.writeDouble(size)
    parcel.writeDouble(width)
    parcel.writeDouble(height)
    parcel.writeString(datetime)
    parcel.writeValue(duration)
    parcel.writeValue(bitrate)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MediaAsset> {
    override fun createFromParcel(parcel: Parcel): MediaAsset {
      return MediaAsset(parcel)
    }

    override fun newArray(size: Int): Array<MediaAsset?> {
      return arrayOfNulls(size)
    }
  }
}
