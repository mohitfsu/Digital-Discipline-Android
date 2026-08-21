package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class ChildDto(
    @DocumentId
    val childId: String = "",
    val name: String = "",
    val age: Int = 10,
    val avatarId: String = "avatar_1",
    val activePairingCode: String? = null,
    val pairingCodeExpiresAt: Long? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)
