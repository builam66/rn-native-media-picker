package com.rnmediapicker.library

import android.os.Parcel
import android.os.Parcelable

data class MediaPickerResponse(
  val resultCode: Int,
  val assets: List<MediaAsset>
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readInt(),
    parcel.createTypedArrayList(MediaAsset) ?: emptyList()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(resultCode)
    parcel.writeTypedList(assets)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MediaPickerResponse> {
    override fun createFromParcel(parcel: Parcel): MediaPickerResponse {
      return MediaPickerResponse(parcel)
    }

    override fun newArray(size: Int): Array<MediaPickerResponse?> {
      return arrayOfNulls(size)
    }
  }
}
