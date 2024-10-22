package com.rnmediapicker

import android.os.Parcel
import android.os.Parcelable
import com.rnmediapicker.constants.DefaultConstants

data class RnMediaPickerOptions(val mediaType: String, val isMultipleSelection: Boolean, val maxSelection: Int) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readString() ?: DefaultConstants.MEDIA_TYPE_ALL,
    parcel.readByte() != 0.toByte(),
    parcel.readInt()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(mediaType)
    parcel.writeByte(if (isMultipleSelection) 1 else 0)
    parcel.writeInt(maxSelection)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<RnMediaPickerOptions> {
    override fun createFromParcel(parcel: Parcel): RnMediaPickerOptions {
      return RnMediaPickerOptions(parcel)
    }

    override fun newArray(size: Int): Array<RnMediaPickerOptions?> {
      return arrayOfNulls(size)
    }
  }
}
