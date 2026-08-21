package com.digitaldiscipline.spike.intervention

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.logging.EventLogger

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_UNLOCK_DURATION_SEC = "extra_unlock_sec"

        var onInterventionCompleted: ((packageName: String, durationMs: Long) -> Unit)? = null
    }

    private var targetPackage: String = ""
    private var targetAppName: String = "App"
    private var unlockSeconds: Int = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: "unknown"
        targetAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Application"
        unlockSeconds = intent.getIntExtra(EXTRA_UNLOCK_DURATION_SEC, 60)

        val startRender = System.currentTimeMillis()
        EventLogger.log(
            source = "ACTIVITY_INTERVENTION",
            packageName = targetPackage,
            eventType = "ACTIVITY_SHOWN",
            details = "Target: $targetAppName"
        )

        setContent {
            InterventionScreen(
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                unlockSeconds = unlockSeconds,
                onComplete = { durationMs ->
                    EventLogger.log(
                        source = "ACTIVITY_INTERVENTION",
                        packageName = targetPackage,
                        eventType = "INTERVENTION_COMPLETED",
                        details = "Duration: ${unlockSeconds}s"
                    )
                    onInterventionCompleted?.invoke(targetPackage, durationMs)
                    finish()
                },
                onExitHome = {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back press from returning directly into the blocked target app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}

@Composable
fun InterventionScreen(
    targetPackage: String,
    targetAppName: String,
    unlockSeconds: Int,
    onComplete: (durationMs: Long) -> Unit,
    onExitHome: () -> Unit
) {
    var isWaiting by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(10) }

    LaunchedEffect(isWaiting) {
        if (isWaiting) {
            val timer = object : CountDownTimer(10000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    secondsLeft = ((millisUntilFinished / 1000) + 1).toInt()
                }

                override fun onFinish() {
                    secondsLeft = 0
                    onComplete(unlockSeconds * 1000L)
                }
            }
            timer.start()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DIGITAL DISCIPLINE",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$targetAppName Paused",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your parent has enabled a screen-time rule.\nComplete an action to unlock temporarily:",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick Interventions",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isWaiting) {
                        LinearProgressIndicator(
                            progress = { (10 - secondsLeft) / 10f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Mindful pause: ${secondsLeft}s remaining",
                            color = Color(0xFFFBBF24),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { isWaiting = true },
                        enabled = !isWaiting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⏳ WAIT 10 SECONDS", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onComplete(unlockSeconds * 1000L) },
                        enabled = !isWaiting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🏋️ COMPLETE 10 SQUATS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onExitHome,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Text("🏠 EXIT TO HOME SCREEN", color = Color(0xFF94A3B8))
            }
        }
    }
}
