package com.digitaldiscipline.spike.ui.challenges

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Design tokens
private val BgCard = Color(0xFF0F172A)
private val BorderDefault = Color(0xFF1E293B)
private val PrimaryCyan = Color(0xFF38BDF8)
private val EmeraldGreen = Color(0xFF34D399)
private val AlertRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val HighlightAmber = Color(0xFFFBBF24)

// ─────────────────────────────────────────────────────────────────────────────
// 1. 30-SECOND STROOP COLOR-WORD CONFLICT SPRINT
// ─────────────────────────────────────────────────────────────────────────────
data class StroopItem(val word: String, val inkColor: Color, val inkName: String)

@Composable
fun StroopChallengeGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val colorPalettes = remember {
        listOf(
            "RED" to Color(0xFFEF4444),
            "BLUE" to Color(0xFF38BDF8),
            "GREEN" to Color(0xFF22C55E),
            "YELLOW" to Color(0xFFFBBF24),
            "PURPLE" to Color(0xFFA855F7)
        )
    }

    fun generateItem(): StroopItem {
        val wordEntry = colorPalettes.random()
        val inkEntry = colorPalettes.filter { it.first != wordEntry.first }.random()
        return StroopItem(word = wordEntry.first, inkColor = inkEntry.second, inkName = inkEntry.first)
    }

    var currentItem by remember { mutableStateOf(generateItem()) }
    var secondsRemaining by remember { mutableIntStateOf(30) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalAttempts by remember { mutableIntStateOf(0) }
    var isError by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val targetCorrect = 6
    val accuracy = if (totalAttempts > 0) (correctCount * 100) / totalAttempts else 100

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0 && !isFinished) {
            delay(1000L)
            secondsRemaining--
        }
        isFinished = true
        if (correctCount >= targetCorrect && accuracy >= 75) {
            delay(400)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with 30s Countdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧠 30s Stroop Conflict Sprint", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (secondsRemaining <= 5) AlertRed.copy(alpha = 0.25f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (secondsRemaining <= 5) AlertRed else PrimaryCyan)
            ) {
                Text(
                    text = "${secondsRemaining}s left",
                    color = if (secondsRemaining <= 5) AlertRed else PrimaryCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tap the INK COLOR, not the text", color = HighlightAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Score: $correctCount ($accuracy%)", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar for 30s duration
        LinearProgressIndicator(
            progress = { ((30 - secondsRemaining) / 30f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = PrimaryCyan,
            trackColor = BorderDefault
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (!isFinished) {
            // Stimulus Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(2.dp, if (isError) AlertRed else PrimaryCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().height(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = currentItem.word,
                        color = currentItem.inkColor,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            if (isError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("❌ Match font color, ignore the word meaning!", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Color Choice Buttons
            val choices = remember(currentItem) {
                val correct = currentItem.inkName
                val others = colorPalettes.map { it.first }.filter { it != correct }.shuffled().take(3)
                (others + correct).shuffled()
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    choices.take(2).forEach { choiceName ->
                        val colorHex = colorPalettes.first { it.first == choiceName }.second
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.5.dp, colorHex.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f).height(50.dp).clickable {
                                totalAttempts++
                                if (choiceName == currentItem.inkName) {
                                    isError = false
                                    correctCount++
                                    currentItem = generateItem()
                                } else {
                                    isError = true
                                    currentItem = generateItem()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(choiceName, color = colorHex, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    choices.drop(2).take(2).forEach { choiceName ->
                        val colorHex = colorPalettes.first { it.first == choiceName }.second
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.5.dp, colorHex.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f).height(50.dp).clickable {
                                totalAttempts++
                                if (choiceName == currentItem.inkName) {
                                    isError = false
                                    correctCount++
                                    currentItem = generateItem()
                                } else {
                                    isError = true
                                    currentItem = generateItem()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(choiceName, color = colorHex, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        } else {
            // 30s Completed Summary
            val passed = correctCount >= targetCorrect && accuracy >= 75
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (passed) EmeraldGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, if (passed) EmeraldGreen else AlertRed),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (passed) "🎯 30s Sprint Completed!" else "⏱️ Sprint Needs Focus", color = if (passed) EmeraldGreen else AlertRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Score: $correctCount Correct • $accuracy% Accuracy", color = TextWhite, fontSize = 14.sp)
                    if (!passed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                secondsRemaining = 30
                                correctCount = 0
                                totalAttempts = 0
                                isFinished = false
                                isError = false
                                currentItem = generateItem()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RETRY 30s SPRINT", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. 30-SECOND MENTAL MATH SPRINT
// ─────────────────────────────────────────────────────────────────────────────
data class MathSprintProblem(val text: String, val answer: Int, val choices: List<Int>)

@Composable
fun MathSprintGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    fun generateProblem(): MathSprintProblem {
        val type = (1..3).random()
        return when (type) {
            1 -> {
                val a = (6..12).random()
                val b = (6..9).random()
                val c = (4..18).random()
                val ans = (a * b) - c
                val dist = listOf(ans - 4, ans + 4, ans + 10).shuffled()
                MathSprintProblem("($a × $b) − $c", ans, (dist + ans).shuffled())
            }
            2 -> {
                val a = (24..68).random()
                val b = (18..54).random()
                val ans = a + b
                val dist = listOf(ans - 10, ans + 10, ans - 2).shuffled()
                MathSprintProblem("$a + $b", ans, (dist + ans).shuffled())
            }
            else -> {
                val a = (12..25).random()
                val b = (3..5).random()
                val c = (10..30).random()
                val ans = (a * b) + c
                val dist = listOf(ans - 10, ans + 10, ans + 5).shuffled()
                MathSprintProblem("($a × $b) + $c", ans, (dist + ans).shuffled())
            }
        }
    }

    var currentProblem by remember { mutableStateOf(generateProblem()) }
    var secondsRemaining by remember { mutableIntStateOf(30) }
    var solvedCount by remember { mutableIntStateOf(0) }
    var isError by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    val targetSolves = 5

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0 && !isFinished) {
            delay(1000L)
            secondsRemaining--
        }
        isFinished = true
        if (solvedCount >= targetSolves) {
            delay(400)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡ 30s Mental Math Sprint", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (secondsRemaining <= 5) AlertRed.copy(alpha = 0.25f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (secondsRemaining <= 5) AlertRed else PrimaryCyan)
            ) {
                Text(
                    text = "${secondsRemaining}s left",
                    color = if (secondsRemaining <= 5) AlertRed else PrimaryCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Target: Solve ≥ $targetSolves equations", color = TextMuted, fontSize = 12.sp)
            Text("Solved: $solvedCount / $targetSolves", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { ((30 - secondsRemaining) / 30f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = EmeraldGreen,
            trackColor = BorderDefault
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (!isFinished) {
            // Problem Display
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(2.dp, if (isError) AlertRed else PrimaryCyan),
                modifier = Modifier.fillMaxWidth().height(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${currentProblem.text} = ?",
                        color = TextWhite,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("❌ Calculation error. Keep going!", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Choices
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    currentProblem.choices.take(2).forEach { choice ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(50.dp).clickable {
                                if (choice == currentProblem.answer) {
                                    isError = false
                                    solvedCount++
                                    currentProblem = generateProblem()
                                } else {
                                    isError = true
                                    currentProblem = generateProblem()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$choice", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    currentProblem.choices.drop(2).take(2).forEach { choice ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(50.dp).clickable {
                                if (choice == currentProblem.answer) {
                                    isError = false
                                    solvedCount++
                                    currentProblem = generateProblem()
                                } else {
                                    isError = true
                                    currentProblem = generateProblem()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$choice", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            val passed = solvedCount >= targetSolves
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (passed) EmeraldGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, if (passed) EmeraldGreen else AlertRed),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (passed) "🎯 30s Math Sprint Passed!" else "⏱️ Target Not Reached", color = if (passed) EmeraldGreen else AlertRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Solved $solvedCount / $targetSolves required", color = TextWhite, fontSize = 14.sp)
                    if (!passed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                secondsRemaining = 30
                                solvedCount = 0
                                isFinished = false
                                isError = false
                                currentProblem = generateProblem()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RETRY 30s SPRINT", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. 3-STAGE PROGRESSIVE WORKING MEMORY MATRIX (~30s Total Paced)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MemoryMatrixGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var stage by remember { mutableIntStateOf(1) }
    val totalStages = 3

    val gridSize = when (stage) {
        3 -> 4
        else -> 3
    }
    val patternLength = when (stage) {
        1 -> 4
        2 -> 5
        else -> 5
    }

    val totalCells = gridSize * gridSize
    var sequence by remember(stage) {
        mutableStateOf(
            mutableListOf<Int>().apply {
                while (size < patternLength) {
                    val next = (0 until totalCells).random()
                    if (!contains(next)) add(next)
                }
            }.toList()
        )
    }

    var activeHighlight by remember { mutableStateOf<Int?>(null) }
    var isMemorizing by remember { mutableStateOf(true) }
    val userSelections = remember(stage) { mutableStateListOf<Int>() }
    var isError by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        isMemorizing = true
        userSelections.clear()
        isError = false
        delay(600)
        for (pos in sequence) {
            activeHighlight = pos
            delay(700)
            activeHighlight = null
            delay(250)
        }
        isMemorizing = false
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
            Text("🧩 30s Spatial Memory Matrix", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isMemorizing) Color(0xFF0284C7) else EmeraldGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Stage $stage/3 • " + if (isMemorizing) "MEMORIZE" else "${userSelections.size}/$patternLength RECALLED",
                    color = if (isMemorizing) TextWhite else EmeraldGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isMemorizing) "Memorize glowing pattern (${patternLength} tiles)..." else "Tap all $patternLength tiles in exact sequence",
            color = TextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Grid (3x3 or 4x4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until gridSize) {
                        val index = (row * gridSize) + col
                        val isLit = activeHighlight == index
                        val isUserPicked = userSelections.contains(index)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isLit -> PrimaryCyan
                                isUserPicked -> EmeraldGreen
                                else -> Color(0xFF1E293B)
                            },
                            border = BorderStroke(1.dp, if (isLit || isUserPicked) PrimaryCyan else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (gridSize == 4) 48.dp else 56.dp)
                                .clickable(enabled = !isMemorizing && !isCompleted && !isUserPicked) {
                                    val expectedNext = sequence[userSelections.size]
                                    if (index == expectedNext) {
                                        isError = false
                                        userSelections.add(index)
                                        if (userSelections.size == patternLength) {
                                            if (stage < totalStages) {
                                                stage++
                                            } else {
                                                isCompleted = true
                                                coroutineScope.launch {
                                                    delay(350)
                                                    onSuccess()
                                                }
                                            }
                                        }
                                    } else {
                                        isError = true
                                        userSelections.clear()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isUserPicked) {
                                    Text("✓", color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("❌ Sequence broken! Repeating stage $stage.", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. 30-SECOND DEEP STOIC MINDFUL READING FRICTION
// ─────────────────────────────────────────────────────────────────────────────
data class WisdomQuote(val text: String, val author: String, val question: String, val answer: String, val distractors: List<String>)

@Composable
fun MindfulReadingGame(onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val quotes = remember {
        listOf(
            WisdomQuote(
                text = "You have power over your mind — not outside events or social feeds. Realize this right now, and you will find instant self-mastery and strength.",
                author = "Marcus Aurelius, Meditations",
                question = "According to Marcus Aurelius, where does true power reside?",
                answer = "In mastering your own conscious mind",
                distractors = listOf("In reacting to outside notifications", "In checking infinite feeds", "In quick algorithmic updates")
            ),
            WisdomQuote(
                text = "No person is free who is not master of oneself. Freedom is not the absence of restraint; it is the deliberate mastery over impulsive digital urges.",
                author = "Epictetus, Enchiridion",
                question = "What is the true foundation of freedom described here?",
                answer = "Deliberate self-mastery over impulsive urges",
                distractors = listOf("Endless scrolling without friction", "Passive automatic habits", "Instant dopamine gratification")
            ),
            WisdomQuote(
                text = "You do not rise to the level of your goals. You fall to the level of your systems. Every intentional pause is a vote for the person you wish to become.",
                author = "James Clear, Atomic Habits",
                question = "What directly shapes your long-term daily habit outcome?",
                answer = "The intentional systems and pauses you build",
                distractors = listOf("Relying purely on willpower alone", "Unchecked automatic app opens", "Giving in to instant friction-free impulses")
            )
        )
    }

    val selectedQuote = remember { quotes.random() }
    var readingTimeLeft by remember { mutableIntStateOf(25) }
    var canAnswer by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (readingTimeLeft > 0) {
            delay(1000L)
            readingTimeLeft--
        }
        canAnswer = true
    }

    val choices = remember(selectedQuote) {
        (selectedQuote.distractors + selectedQuote.answer).shuffled()
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
            Text("📖 30s Mindful Wisdom Reflection", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!canAnswer) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, PrimaryCyan)) {
                    Text("${readingTimeLeft}s pause", color = PrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, if (!canAnswer) PrimaryCyan.copy(alpha = 0.7f) else Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "“${selectedQuote.text}”",
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— ${selectedQuote.author}",
                    color = PrimaryCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!canAnswer) {
            LinearProgressIndicator(
                progress = { ((25 - readingTimeLeft) / 25f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = PrimaryCyan,
                trackColor = BorderDefault
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Read thoughtfully. Comprehension check unlocks at 25s...", color = TextMuted, fontSize = 12.sp)
        } else {
            Text(selectedQuote.question, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice ->
                    val isThisCorrect = choice == selectedQuote.answer
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isCompleted) {
                            if (isThisCorrect) {
                                isCompleted = true
                                isError = false
                                coroutineScope.launch {
                                    delay(400)
                                    onSuccess()
                                }
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text(
                            text = choice,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("❌ Incorrect reflection. Review the quote above.", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. 30-SECOND DUAL-PROMPT INTENTIONALITY JOURNAL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IntentionalityFrictionGame(targetAppName: String, onSuccess: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var intentionText by remember { mutableStateOf("") }
    var nextTaskText by remember { mutableStateOf("") }
    var secondsRemaining by remember { mutableIntStateOf(30) }

    val minCharsIntention = 20
    val minCharsNextTask = 15
    val hasValidIntention = intentionText.trim().length >= minCharsIntention
    val hasValidNextTask = nextTaskText.trim().length >= minCharsNextTask
    val canSubmit = secondsRemaining == 0 && hasValidIntention && hasValidNextTask

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
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
            Text("✍️ 30s Intentional Purpose Journal", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (secondsRemaining == 0) EmeraldGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (secondsRemaining == 0) EmeraldGreen else PrimaryCyan)
            ) {
                Text(
                    text = if (secondsRemaining > 0) "${secondsRemaining}s pause" else "Ready ✓",
                    color = if (secondsRemaining == 0) EmeraldGreen else PrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ((30 - secondsRemaining) / 30f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = PrimaryCyan,
            trackColor = BorderDefault
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt 1: Why open now?
        Text("1. Specific conscious purpose for opening $targetAppName:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = intentionText,
            onValueChange = { intentionText = it },
            placeholder = { Text("e.g. Check marketing campaign stats...", color = TextMuted, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderDefault,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Prompt 2: What offline task next?
        Text("2. Constructive offline task you will do after 10m:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = nextTaskText,
            onValueChange = { nextTaskText = it },
            placeholder = { Text("e.g. Complete math practice worksheet...", color = TextMuted, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderDefault,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (canSubmit) {
                    coroutineScope.launch {
                        delay(200)
                        onSuccess()
                    }
                }
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                disabledContainerColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = if (secondsRemaining > 0) "WAIT ${secondsRemaining}s TO UNLOCK" else if (!hasValidIntention || !hasValidNextTask) "COMPLETE BOTH PROMPTS" else "UNLOCK ACCESS",
                color = if (canSubmit) Color(0xFF0F172A) else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
