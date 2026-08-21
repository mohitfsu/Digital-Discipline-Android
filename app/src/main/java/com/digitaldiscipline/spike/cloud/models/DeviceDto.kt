package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class DeviceDto(
    @DocumentId
    val deviceId: String = "",
    val childId: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val isProtectionActive: Boolean = true,
    val activePolicyVersion: Int = 1,
    @ServerTimestamp
    val lastSeen: Date? = null,
    @ServerTimestamp
    val pairedAt: Date? = null
)
