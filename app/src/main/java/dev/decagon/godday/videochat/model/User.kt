package dev.decagon.godday.videochat.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize


@Parcelize
data class User(
    var displayName: String? = null,
    var email: String? = null,
    var photoUrl: String? = null
): Parcelable