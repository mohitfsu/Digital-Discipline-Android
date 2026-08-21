package com.digitaldiscipline.spike.cloud.models

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class FamilyDto(
    @DocumentId
    val familyId: String = "",
    val familyName: String = "",
    val ownerParentId: String = "",
    val subscriptionTier: String = "FREE", // "FREE", "FAMILY_PLUS"
    @ServerTimestamp
    val createdAt: Date? = null
)
