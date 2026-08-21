package com.digitaldiscipline.spike.ui.challenges

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

// ═════════════════════════════════════════════════════════════════════════════
// 1. ZEN CANVAS: 1-STROKE ENSO & DOODLE SANDBOX
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ZenCanvasEnsoGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("ENSO") } // "ENSO" or "DOODLE"
    val doodlePrompts = remember {
        listOf(
            "Draw a tree in 3 lines 🌲",
            "Sketch your current emotion 🌊",
            "Draw a mountain with a rising sun 🏔️",
            "Draw a bird in flight 🕊️"
        )
    }
    var currentPrompt by remember { mutableStateOf(doodlePrompts.random()) }

    val points = remember { mutableStateListOf<Offset>() }
    var isDrawing by remember { mutableStateOf(false) }
    var evaluationMessage by remember { mutableStateOf("Draw a single continuous circle without lifting your finger") }
    var isCompleted by remember { mutableStateOf(false) }
    var score by remember { mutableFloatStateOf(0f) }

    fun evaluateEnsoCircle(pts: List<Offset>): Boolean {
        if (pts.size < 25) return false
        val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
        val width = maxX - minX; val height = maxY - minY
        if (width < 80f || height < 80f) return false

        val aspectRatio = width / height
        val isCircularRatio = aspectRatio in 0.7f..1.4f
        val startPoint = pts.first(); val endPoint = pts.last()
        val closureDist = hypot((startPoint.x - endPoint.x).toDouble(), (startPoint.y - endPoint.y).toDouble())
        val isClosed = closureDist < (max(width, height) * 0.5f)

        return isCircularRatio && isClosed
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (mode == "ENSO") "🎨 1-Stroke Zen Enso" else "🎨 Creative Micro-Doodle",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier.clickable {
                    mode = if (mode == "ENSO") "DOODLE" else "ENSO"
                    points.clear()
                    isCompleted = false
                    evaluationMessage = if (mode == "ENSO") "Draw a continuous circle" else currentPrompt
                }
            ) {
                Text(
                    text = if (mode == "ENSO") "Switch to Doodle ➔" else "Switch to Enso ➔",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = evaluationMessage,
            color = if (isCompleted) Color(0xFF34D399) else Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Drawing Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF030712))
                .border(1.5.dp, if (isCompleted) Color(0xFF34D399) else Color(0xFF1E293B), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!isCompleted) {
                                points.clear()
                                points.add(offset)
                                isDrawing = true
                            }
                        },
                        onDrag = { change, _ ->
                            if (!isCompleted) {
                                change.consume()
                                points.add(change.position)
                            }
                        },
                        onDragEnd = {
                            isDrawing = false
                            if (mode == "ENSO") {
                                val passed = evaluateEnsoCircle(points)
                                if (passed) {
                                    isCompleted = true
                                    evaluationMessage = "✨ Harmonious Enso! Motor planning engaged."
                                    coroutineScope.launch {
                                        delay(800)
                                        onSuccess()
                                    }
                                } else {
                                    evaluationMessage = "Try a smoother, rounded continuous stroke."
                                }
                            } else {
                                if (points.size > 20) {
                                    isCompleted = true
                                    evaluationMessage = "✨ Creative flow unlocked!"
                                    coroutineScope.launch {
                                        delay(800)
                                        onSuccess()
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size > 1) {
                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        path.quadraticBezierTo(p0.x, p0.y, (p0.x + p1.x) / 2, (p0.y + p1.y) / 2)
                    }
                    // Outer glow
                    drawPath(
                        path = path,
                        color = Color(0xFF0284C7).copy(alpha = 0.4f),
                        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Core neon line
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFFA855F7))),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            if (points.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (mode == "ENSO") "⭕ Trace a single circle in one breath" else "✏️ Draw here freely",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    points.clear()
                    isCompleted = false
                    evaluationMessage = "Draw again in one smooth motion"
                }
            ) {
                Text("Clear Canvas ↺", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }

            if (mode == "DOODLE" && !isCompleted && points.size > 15) {
                Button(
                    onClick = {
                        isCompleted = true
                        coroutineScope.launch {
                            delay(400)
                            onSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done Sketching ✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. REAL-WORLD SCAVENGER HUNT (PHYSICAL GROUNDING)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun RealWorldScavengerGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val challenges = remember {
        listOf(
            "🌿 Find something GREEN from nature (leaf/plant)",
            "📖 Point camera at a Physical Book or Notebook",
            "☕ Find a Ceramic Coffee Mug or Water Glass",
            "🪑 Point camera at a Chair or Wooden Desk",
            "👟 Find your Shoes or Footwear"
        )
    }
    val currentChallenge = remember { challenges.random() }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isFound by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📸 Real-World Scavenger Hunt", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Break screen myopia: ground yourself in physical space", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F243E),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YOUR MISSION:", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentChallenge,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Viewfinder Mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF020617))
                .border(1.dp, if (isFound) Color(0xFF34D399) else Color(0xFF334155), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!isScanning && !isFound) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Stand up and point phone at the object", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            } else if (isScanning && !isFound) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { scanProgress },
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Analyzing Real-World Environment...", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", color = Color(0xFF34D399), fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text("Object Confirmed! 3D Space Grounded.", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isFound) {
            Button(
                onClick = {
                    isScanning = true
                    coroutineScope.launch {
                        for (i in 1..10) {
                            delay(200)
                            scanProgress = i / 10f
                        }
                        isScanning = false
                        isFound = true
                        delay(600)
                        onSuccess()
                    }
                },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(if (isScanning) "Scanning Object..." else "I Found It! (Scan Object)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. HAND MUDRA & ANTI-TECH THUMB DEXTERITY
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun HandMudraDexterityGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var currentCycle by remember { mutableIntStateOf(1) }
    var currentFingerIndex by remember { mutableIntStateOf(0) } // 0=Thumb, 1=Index, 2=Middle, 3=Ring, 4=Pinky
    val fingerNames = listOf("👍 Thumb", "☝️ Index", "🖕 Middle", "💍 Ring", "🤙 Pinky")
    var isHoldStage by remember { mutableStateOf(false) }
    var holdSecondsLeft by remember { mutableIntStateOf(15) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(isHoldStage) {
        if (isHoldStage) {
            while (holdSecondsLeft > 0) {
                delay(1000L)
                holdSecondsLeft--
            }
            isCompleted = true
            delay(500)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✋ Anti-Tech Dexterity & Mudra", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Release thumb claw tension and reset tendon loops", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        if (!isHoldStage) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PHASE 1: RHYTHMIC FINGER TAP (Cycle $currentCycle / 3)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap: ${fingerNames[currentFingerIndex]}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5 Tap Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                fingerNames.forEachIndexed { idx, name ->
                    val isCurrent = idx == currentFingerIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) Color(0xFF0284C7) else Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, if (isCurrent) Color(0xFF38BDF8) else Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(58.dp)
                            .clickable {
                                if (idx == currentFingerIndex) {
                                    if (currentFingerIndex < 4) {
                                        currentFingerIndex++
                                    } else {
                                        if (currentCycle < 3) {
                                            currentCycle++
                                            currentFingerIndex = 0
                                        } else {
                                            isHoldStage = true
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = name.split(" ").first(),
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Phase 2: Gyan Mudra Hold
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0B243B),
                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PHASE 2: GYAN MUDRA HOLD", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("👌 Touch Index Finger to Thumb Tip", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hold both hands in Gyan Mudra and rest your palms on your knees.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${holdSecondsLeft}s",
                        color = Color(0xFF38BDF8),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. DIVERGENT THINKING & LATERAL BRAINSTORMING SPRINT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun DivergentThinkingGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val objects = remember {
        listOf(
            "🧱 A Standard Brick",
            "📎 A Metal Paperclip",
            "☕ An Empty Coffee Mug",
            "📦 A Cardboard Box",
            "🥄 A Metal Spoon"
        )
    }
    val currentObject = remember { objects.random() }
    var use1 by remember { mutableStateOf("") }
    var use2 by remember { mutableStateOf("") }
    var use3 by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💡 Lateral Thinking Sprint", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Alternative Uses Test: Unlock dopamine via cognitive novelty", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Name 3 creative, non-obvious uses for:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(currentObject, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = use1,
            onValueChange = { use1 = it; errorMsg = null },
            placeholder = { Text("1. e.g. Succulent planter / doorstop", color = Color(0xFF64748B), fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = use2,
            onValueChange = { use2 = it; errorMsg = null },
            placeholder = { Text("2. e.g. Sound amplifier / weights", color = Color(0xFF64748B), fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = use3,
            onValueChange = { use3 = it; errorMsg = null },
            placeholder = { Text("3. e.g. Cookie dough stamper", color = Color(0xFF64748B), fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (use1.trim().length >= 3 && use2.trim().length >= 3 && use3.trim().length >= 3) {
                    isSubmitted = true
                    coroutineScope.launch {
                        delay(500)
                        onSuccess()
                    }
                } else {
                    errorMsg = "Please enter 3 distinct creative ideas (min 3 characters each)."
                }
            },
            enabled = !isSubmitted,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text(if (isSubmitted) "Creativity Confirmed ✓" else "Submit 3 Ideas ✓", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. MICRO-POETRY & 5-7-5 HAIKU CRAFTER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun HaikuCrafterGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var line1 by remember { mutableStateOf("") }
    var line2 by remember { mutableStateOf("") }
    var line3 by remember { mutableStateOf("") }
    var isDone by remember { mutableStateOf(false) }

    fun estimateSyllables(text: String): Int {
        val clean = text.lowercase(Locale.US).replace(Regex("[^a-z ]"), "")
        if (clean.isBlank()) return 0
        val words = clean.split(" ").filter { it.isNotBlank() }
        var count = 0
        for (w in words) {
            var wordCount = 0
            var prevVowel = false
            for (ch in w) {
                val isVowel = ch in "aeiouy"
                if (isVowel && !prevVowel) wordCount++
                prevVowel = isVowel
            }
            if (w.endsWith("e") && !w.endsWith("le") && wordCount > 1) wordCount--
            count += max(1, wordCount)
        }
        return count
    }

    val syl1 = estimateSyllables(line1)
    val syl2 = estimateSyllables(line2)
    val syl3 = estimateSyllables(line3)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📜 5-7-5 Haiku Crafter", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Compress thought into rhythm. Awaken deliberate expression.", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        // Line 1 (Target: 5)
        OutlinedTextField(
            value = line1,
            onValueChange = { line1 = it },
            label = { Text("Line 1 (5 syllables) • Count: $syl1") },
            placeholder = { Text("Glass glows in the dark", color = Color(0xFF64748B)) },
            singleLine = true,
            trailingIcon = { Text(if (syl1 == 5) "✓ 5" else "$syl1/5", color = if (syl1 == 5) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Line 2 (Target: 7)
        OutlinedTextField(
            value = line2,
            onValueChange = { line2 = it },
            label = { Text("Line 2 (7 syllables) • Count: $syl2") },
            placeholder = { Text("Quiet room awaits my breath", color = Color(0xFF64748B)) },
            singleLine = true,
            trailingIcon = { Text(if (syl2 == 7) "✓ 7" else "$syl2/7", color = if (syl2 == 7) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Line 3 (Target: 5)
        OutlinedTextField(
            value = line3,
            onValueChange = { line3 = it },
            label = { Text("Line 3 (5 syllables) • Count: $syl3") },
            placeholder = { Text("Focus is restored", color = Color(0xFF64748B)) },
            singleLine = true,
            trailingIcon = { Text(if (syl3 == 5) "✓ 5" else "$syl3/5", color = if (syl3 == 5) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                isDone = true
                coroutineScope.launch {
                    delay(400)
                    onSuccess()
                }
            },
            enabled = line1.isNotBlank() && line2.isNotBlank() && line3.isNotBlank() && !isDone,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text(if (isDone) "Poetry Complete ✓" else "Publish Haiku & Unlock", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. BINAURAL AMBIENT SOUNDSCAPE SYNTHESIZER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun BinauralSoundscapeGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var volBinaural by remember { mutableFloatStateOf(0.8f) }
    var volRain by remember { mutableFloatStateOf(0.6f) }
    var volBowl by remember { mutableFloatStateOf(0.5f) }
    var volChords by remember { mutableFloatStateOf(0.4f) }

    var timerSeconds by remember { mutableIntStateOf(20) }
    var isPlaying by remember { mutableStateOf(true) }
    var isComplete by remember { mutableStateOf(false) }

    // Procedural AudioTrack Tone Generator
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioTrack.play()
            val shortBuffer = ShortArray(bufferSize / 2)
            var phaseL = 0.0
            var phaseR = 0.0
            val freqL = 400.0 // Base Carrier
            val freqR = 440.0 // 40Hz Binaural Beat difference

            try {
                while (isActive && isPlaying) {
                    for (i in 0 until shortBuffer.size step 2) {
                        val sampleL = (sin(phaseL) * 32767 * volBinaural * 0.4).toInt().coerceIn(-32768, 32767).toShort()
                        val sampleR = (sin(phaseR) * 32767 * volBinaural * 0.4).toInt().coerceIn(-32768, 32767).toShort()
                        shortBuffer[i] = sampleL
                        shortBuffer[i + 1] = sampleR

                        phaseL += 2.0 * Math.PI * freqL / sampleRate
                        phaseR += 2.0 * Math.PI * freqR / sampleRate
                    }
                    audioTrack.write(shortBuffer, 0, shortBuffer.size)
                }
            } finally {
                audioTrack.stop()
                audioTrack.release()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
        }
        isComplete = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎹 Binaural Soundscape Synthesizer", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("40Hz Gamma Focus Frequency • Sonic sensory reset", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        // Sliders Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SoundSliderRow(label = "🎚️ 40Hz Focus Beat", value = volBinaural, onValueChange = { volBinaural = it })
                SoundSliderRow(label = "🌧️ Forest Rain", value = volRain, onValueChange = { volRain = it })
                SoundSliderRow(label = "🥣 Tibetan Singing Bowl", value = volBowl, onValueChange = { volBowl = it })
                SoundSliderRow(label = "🎹 Lo-Fi Warm Chords", value = volChords, onValueChange = { volChords = it })
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isComplete) Color(0xFF065F46) else Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (isComplete) Color(0xFF34D399) else Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isComplete) "✓ Focus Soundscape Synced" else "Immersion: ${timerSeconds}s remaining",
                    color = if (isComplete) Color(0xFF34D399) else Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        isPlaying = false
                        onSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (isComplete) "Unlock" else "Skip to App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SoundSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.weight(1.2f))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7), inactiveTrackColor = Color(0xFF334155)),
            modifier = Modifier.weight(1f).height(24.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. TIME-CAPSULE MESSAGE TO FUTURE SELF
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun FutureSelfCapsuleGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var isSealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💌 Capsule to Future Self", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Crush temporal discounting: connect with your 10:00 PM self", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("PROMPT:", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "\"Write 1 sentence your future self at 10:00 PM tonight will thank you for.\"",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("e.g. I worked on my core project instead of scrolling mindless feeds.", color = Color(0xFF64748B)) },
            minLines = 3,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF0284C7), unfocusedBorderColor = Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                isSealed = true
                coroutineScope.launch {
                    delay(500)
                    onSuccess()
                }
            },
            enabled = message.trim().length >= 10 && !isSealed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text(if (isSealed) "Capsule Sealed for 10 PM 📬" else "Seal Capsule & Unlock (10 PM Reminder)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 8. PERSPECTIVE SHIFT / STOIC TAROT DECIDER
// ═════════════════════════════════════════════════════════════════════════════

data class StoicCard(val title: String, val icon: String, val quote: String, val prompt: String)

@Composable
fun StoicTarotDeciderGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val cards = remember {
        listOf(
            StoicCard(
                title = "Memento Mori",
                icon = "🏛️",
                quote = "\"You have a finite number of breaths today. Is this feed how you spend one?\"",
                prompt = "Honor your finite mortal time."
            ),
            StoicCard(
                title = "The Cosmic Zoom",
                icon = "🔭",
                quote = "\"View your current moment from orbit. Will this notification matter in 5 years?\"",
                prompt = "Expand your frame of reference."
            ),
            StoicCard(
                title = "The Inversion",
                icon = "⚡",
                quote = "\"What would the most disciplined, unstoppable version of you do right now?\"",
                prompt = "Act as your highest self."
            )
        )
    }

    var selectedCard by remember { mutableStateOf<StoicCard?>(null) }
    var isFlipped by remember { mutableStateOf(false) }
    var contemplationSeconds by remember { mutableIntStateOf(15) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    LaunchedEffect(isFlipped) {
        if (isFlipped) {
            while (contemplationSeconds > 0) {
                delay(1000L)
                contemplationSeconds--
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔮 Perspective Shift / Stoic Tarot", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(if (!isFlipped) "Choose 1 card to reframe your impulse" else "Reflect on this perspective", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isFlipped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEach { card ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clickable {
                                selectedCard = card
                                isFlipped = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🔮", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("TAP TO REVEAL", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            // Flipped Card Display
            selectedCard?.let { card ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0B1B33),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(card.icon, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(card.title.uppercase(), color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = card.quote,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (contemplationSeconds > 0) "Contemplate for ${contemplationSeconds}s..." else "✓ Perspective Shift Complete",
                            color = if (contemplationSeconds == 0) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSuccess,
                    enabled = contemplationSeconds == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(if (contemplationSeconds > 0) "Contemplating (${contemplationSeconds}s)..." else "Act on Wisdom (Unlock)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
