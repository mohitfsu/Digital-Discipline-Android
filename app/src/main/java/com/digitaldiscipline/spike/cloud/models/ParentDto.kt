package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class ParentDto(
    @DocumentId
    val parentId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "OWNER", // "OWNER", "GUARDIAN"
    @ServerTimestamp
    val createdAt: Date? = null
)
