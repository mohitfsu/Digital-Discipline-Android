# Proguard and R8 optimization rules for Digital Discipline Production Release Candidate

# 1. Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-keepclassmembers class androidx.compose.material3.** { *; }

# 2. AndroidX Room Database & Entities
-keepclassmembers class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# 3. AndroidX WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 4. Accessibility Service & Core Services
-keep class com.digitaldiscipline.spike.service.DigitalDisciplineAccessibilityService { *; }
-keep class com.digitaldiscipline.spike.ui.MainActivity { *; }

# 5. Security & Crypto (Android Keystore / EncryptedSharedPreferences)
-keepclassmembers class androidx.security.crypto.** { *; }

# 6. Data Classes & Serialization
-keepclassmembers class com.digitaldiscipline.spike.data.local.entities.** { *; }
-keepclassmembers class com.digitaldiscipline.spike.behaviour.** { *; }

# 7. Kotlin Coroutines & Flow
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
