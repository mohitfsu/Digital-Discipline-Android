package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class PairingCodeDto(
    @DocumentId
    val code: String = "", // 6-digit numeric or alphanumeric (e.g. "482910")
    val familyId: String = "",
    val childId: String = "",
    val childName: String = "",
    val createdByParentId: String = "",
    val expiresAtTimestampMs: Long = 0L,
    val isUsed: Boolean = false,
    val pairedDeviceId: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val usedAt: Date? = null
)
