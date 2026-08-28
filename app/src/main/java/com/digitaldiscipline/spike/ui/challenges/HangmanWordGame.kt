package com.digitaldiscipline.spike.ui.challenges

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ═════════════════════════════════════════════════════════════════════════════
// HANGMAN: CLASSIC MINDFUL WORD DECRYPTION CHALLENGE
// ═════════════════════════════════════════════════════════════════════════════

private data class HangmanWord(
    val word: String,
    val hint: String,
    val category: String = "MINDFUL FOCUS"
)

private val HANGMAN_DICTIONARY = listOf(
    // 1. Mindful Focus & Presence
    HangmanWord("MINDFUL", "Present in the current moment", "MINDFULNESS"),
    HangmanWord("FOCUS", "Undivided presence and attention", "DEEP WORK"),
    HangmanWord("BREATHE", "Inhale deeply and reset your nervous system", "BREATHWORK"),
    HangmanWord("AWARENESS", "Observing thoughts without reacting", "MINDFULNESS"),
    HangmanWord("STILLNESS", "Calmness amidst external chaos", "INNER PEACE"),
    HangmanWord("PRESENCE", "Being fully here right now", "MINDFULNESS"),
    HangmanWord("MEDITATE", "Training the mind for calm clarity", "ZEN PRACTICE"),
    HangmanWord("CALMNESS", "A peaceful state of mind", "INNER PEACE"),
    HangmanWord("SILENCE", "Finding quiet within the noise", "REFLECTION"),
    HangmanWord("GROUNDED", "Rooted in physical reality", "SOMATIC AWARENESS"),

    // 2. Self-Discipline & Growth
    HangmanWord("DISCIPLINE", "Consistency over temporary impulse", "CORE VALUE"),
    HangmanWord("WILLPOWER", "Choosing long-term goals over quick dopamine", "SELF CONTROL"),
    HangmanWord("PATIENCE", "The power to pause before action", "VIRTUE"),
    HangmanWord("COURAGE", "Bravery to choose growth over comfort", "GROWTH"),
    HangmanWord("STRENGTH", "Inner fortitude to resist distraction", "RESILIENCE"),
    HangmanWord("RESILIENCE", "Bouncing back stronger from urges", "GROWTH"),
    HangmanWord("MASTERY", "Gradual improvement through deliberate practice", "ACHIEVEMENT"),
    HangmanWord("PROGRESS", "Moving forward one small step at a time", "MOMENTUM"),
    HangmanWord("PERSISTENCE", "Continuing onward despite resistance", "TENACITY"),
    HangmanWord("GRIT", "Passion and perseverance for long-term goals", "CHARACTER"),

    // 3. Mental Clarity & Wisdom
    HangmanWord("CLARITY", "Mental calmness free of digital fog", "SHARP FOCUS"),
    HangmanWord("WISDOM", "Clear perception and sound judgment", "PHILOSOPHY"),
    HangmanWord("PURPOSE", "The deeper reason guiding your daily actions", "INTENTION"),
    HangmanWord("INTENTION", "Acting with deliberate conscious choice", "MINDFUL ACTION"),
    HangmanWord("INSIGHT", "Deep understanding born from quiet contemplation", "CLARITY"),
    HangmanWord("REFLECT", "Looking inward before reacting outward", "SELF AWARENESS"),
    HangmanWord("PERSPECTIVE", "Seeing the bigger picture beyond the screen", "STOIC THOUGHT"),
    HangmanWord("VISION", "Holding a clear picture of who you want to become", "ASPIRATION"),
    HangmanWord("LUCID", "Expressed clearly and easy to understand", "CLEAR MIND"),

    // 4. Emotional Wellness & Balance
    HangmanWord("SERENITY", "Peace of mind and stillness", "TRANQUILITY"),
    HangmanWord("GRATITUDE", "Appreciating the abundance of the present", "WELLBEING"),
    HangmanWord("HARMONY", "Balance across body, mind, and time", "EQUILIBRIUM"),
    HangmanWord("KINDNESS", "Compassion in thought, word, and action", "HEARTFULNESS"),
    HangmanWord("TRANQUIL", "Free from agitation or disturbance", "CALM"),
    HangmanWord("EMPATHY", "Understanding the feelings of others", "CONNECTION"),
    HangmanWord("COMPASSION", "Sympathetic consciousness with desire to help", "VIRTUE"),
    HangmanWord("BALANCE", "Equally distributed focus and energy", "LIFE HARMONY"),
    HangmanWord("GRACE", "Elegance and composure under pressure", "ELEGANCE"),

    // 5. Digital Health & Freedom
    HangmanWord("REWIRE", "Neuroplasticity changing brain pathways", "HABIT REBUILD"),
    HangmanWord("DETOX", "Clearing digital clutter and overstimulation", "DIGITAL HEALTH"),
    HangmanWord("UNPLUG", "Stepping away from screens to live real life", "DISCONNECT"),
    HangmanWord("BOUNDARIES", "Protecting your sacred focus time", "SELF CARE"),
    HangmanWord("FREEDOM", "Living by choice rather than algorithm feeds", "SOVEREIGNTY"),
    HangmanWord("RECHARGE", "Restoring mental energy without mindless scrolling", "RESTORATION"),
    HangmanWord("SIMPLICITY", "Eliminating excess to cherish what matters", "ESSENTIALISM"),
    HangmanWord("PRIORITY", "The single most important focus today", "ESSENTIALISM"),

    // 6. Nature & Grounding
    HangmanWord("SUNSHINE", "Natural daylight resetting circadian rhythm", "NATURE RESET"),
    HangmanWord("MOUNTAIN", "Steadfast, unshakable, and majestic", "STOIC NATURE"),
    HangmanWord("RIVER", "Flowing continuously around obstacles", "FLOW STATE"),
    HangmanWord("FOREST", "Calming trees reducing cortisol and stress", "SHINRIN-YOKU"),
    HangmanWord("HORIZON", "Looking into the far distance to rest your eyes", "EYE RESET"),
    HangmanWord("OCEAN", "Vast, rhythmic waves cultivating deep calm", "SERENITY"),
    HangmanWord("BREEZE", "Gentle refreshing air awakening the senses", "VITALITY"),
    HangmanWord("SUNRISE", "A fresh dawn and a clean slate each morning", "NEW DAY"),

    // 7. Everyday Objects & Fun Words (40 Simple Everyday Words)
    HangmanWord("APPLE", "A crunchy red or green fruit", "EVERYDAY OBJECTS"),
    HangmanWord("BANANA", "A long yellow sweet fruit that monkeys love", "EVERYDAY OBJECTS"),
    HangmanWord("PENCIL", "Used for drawing and writing on paper", "EVERYDAY OBJECTS"),
    HangmanWord("GUITAR", "Musical instrument with six strings to strum", "MUSIC & HOBBIES"),
    HangmanWord("BICYCLE", "Two-wheeled vehicle powered by foot pedals", "TRANSPORT & PLAY"),
    HangmanWord("WINDOW", "Opening in a wall with glass to see outside", "EVERYDAY OBJECTS"),
    HangmanWord("CAMERA", "Device used to take photos and videos", "TECH & CREATIVE"),
    HangmanWord("SUNGLASSES", "Worn on your face to shade eyes from sun", "SUMMER GEAR"),
    HangmanWord("BUTTERFLY", "Colorful insect with delicate fluttering wings", "ANIMALS & NATURE"),
    HangmanWord("DOLPHIN", "Smart ocean mammal that leaps out of water", "ANIMALS & NATURE"),
    HangmanWord("SANDWICH", "Two slices of bread with filling inside", "FOOD & SNACKS"),
    HangmanWord("AIRPLANE", "Large flying vehicle traveling through clouds", "TRAVEL & VEHICLES"),
    HangmanWord("RAINBOW", "Colorful arc across sky after a rain shower", "NATURAL WONDER"),
    HangmanWord("TELESCOPE", "Tube with lenses to see stars and planets", "SCIENCE & SPACE"),
    HangmanWord("PYRAMID", "Ancient giant triangular stone structure in Egypt", "WORLD WONDERS"),
    HangmanWord("CHOCOLATE", "Delicious sweet brown treat made from cocoa", "SWEET TREATS"),
    HangmanWord("BACKPACK", "Bag with straps worn on your back for school", "SCHOOL & GEAR"),
    HangmanWord("UMBRELLA", "Canopy on a stick that keeps rain off you", "EVERYDAY ESSENTIALS"),
    HangmanWord("CANDLE", "Wax cylinder with a burning wick for warm light", "COZY HOME"),
    HangmanWord("GARDEN", "Outdoor backyard plot with flowers and vegetables", "OUTDOORS & HOME"),
    HangmanWord("ISLAND", "Piece of land completely surrounded by sea water", "GEOGRAPHY & TRAVEL"),
    HangmanWord("VOLCANO", "Mountain with a crater that spews glowing lava", "NATURAL WONDER"),
    HangmanWord("CAMPFIRE", "Outdoor wood fire for warmth and marshmallows", "CAMPING & OUTDOORS"),
    HangmanWord("NOTEBOOK", "Book filled with lined pages for taking notes", "STATIONERY & WORK"),
    HangmanWord("KEYBOARD", "Panel of letter buttons to type on a screen", "TECH & TYPING"),
    HangmanWord("BALLOON", "Inflatable rubber sphere that floats at parties", "PARTY & FUN"),
    HangmanWord("CUPCAKE", "Miniature individual cake baked in a paper cup", "BAKERY & TREATS"),
    HangmanWord("WHISTLE", "Small mouth device that blows a piercing beep", "SPORTS & SIGNALS"),
    HangmanWord("TREASURE", "Hidden chest of gold coins and shiny jewels", "ADVENTURE & QUEST"),
    HangmanWord("WATERMELON", "Big round green fruit with juicy red sweet slices", "SUMMER FRUITS"),
    HangmanWord("PENGUIN", "Tuxedo-feathered bird that slides on snow ice", "ANIMALS & NATURE"),
    HangmanWord("KANGAROO", "Australian creature that leaps high with a pouch", "ANIMALS & NATURE"),
    HangmanWord("ELEPHANT", "Massive mammal with floppy ears and long trunk", "ANIMALS & NATURE"),
    HangmanWord("SUNFLOWER", "Tall plant with huge yellow petals facing the sun", "FLOWERS & PLANTS"),
    HangmanWord("LANTERN", "Portable glowing lamp with a carrying handle", "CAMPING & LIGHT"),
    HangmanWord("PUZZLE", "Brain game where you piece clues or tiles together", "BRAIN GAMES"),
    HangmanWord("FIREWORK", "Rocket that explodes into sparkling night sky colors", "FESTIVALS"),
    HangmanWord("HAMMOCK", "Net or fabric sling tied between two shady trees", "RELAXATION"),
    HangmanWord("BLANKET", "Soft warm thick cover to snuggle up in bed", "HOME & COZY"),
    HangmanWord("ORIGAMI", "Japanese art form of folding paper into animals", "CREATIVE CRAFT")
)

@Composable
fun HangmanWordGame(
    timeLimitSeconds: Int = 45,
    maxStrikes: Int = 6,
    onSuccess: () -> Unit
) {
    var wordIndex by remember { mutableIntStateOf(Random.nextInt(HANGMAN_DICTIONARY.size)) }
    val currentWordItem = HANGMAN_DICTIONARY[wordIndex % HANGMAN_DICTIONARY.size]
    val targetWord = currentWordItem.word.uppercase()

    val guessedLetters = remember { mutableStateListOf<Char>() }
    var strikes by remember { mutableIntStateOf(0) }
    var timeLeftSeconds by remember { mutableIntStateOf(timeLimitSeconds) }
    var isSolved by remember { mutableStateOf(false) }
    var resetTrigger by remember { mutableIntStateOf(0) }
    var showGameOverNotice by remember { mutableStateOf(false) }

    // Win condition check: all unique letters in targetWord are in guessedLetters
    val isWordComplete = remember(guessedLetters.toList(), targetWord) {
        targetWord.all { guessedLetters.contains(it) }
    }

    // 45-Second Countdown Timer
    LaunchedEffect(resetTrigger, isSolved) {
        if (!isSolved) {
            timeLeftSeconds = timeLimitSeconds
            while (timeLeftSeconds > 0 && !isSolved) {
                delay(1000L)
                timeLeftSeconds--
            }
            if (timeLeftSeconds <= 0 && !isSolved) {
                showGameOverNotice = true
                delay(1000L)
                // Pick next word and reset
                wordIndex = Random.nextInt(HANGMAN_DICTIONARY.size)
                guessedLetters.clear()
                strikes = 0
                showGameOverNotice = false
                resetTrigger++
            }
        }
    }

    // Check Win
    LaunchedEffect(isWordComplete) {
        if (isWordComplete && !isSolved) {
            isSolved = true
            delay(900L)
            onSuccess()
        }
    }

    // Handle Strike Out
    LaunchedEffect(strikes) {
        if (strikes >= maxStrikes && !isSolved) {
            showGameOverNotice = true
            delay(1200L)
            wordIndex = Random.nextInt(HANGMAN_DICTIONARY.size)
            guessedLetters.clear()
            strikes = 0
            showGameOverNotice = false
            resetTrigger++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔤 MINDFUL HANGMAN",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentWordItem.category,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Timer & Strikes Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Strikes indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (strikes >= 4) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (strikes >= 4) Color(0xFFEF4444) else Color(0xFF334155))
                ) {
                    Text(
                        text = "💀 ${maxStrikes - strikes} left",
                        color = if (strikes >= 4) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Timer
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (timeLeftSeconds <= 10) Color(0xFF7F1D1D) else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, if (timeLeftSeconds <= 10) Color(0xFFEF4444) else Color(0xFF0284C7))
                ) {
                    Text(
                        text = "⏱️ ${timeLeftSeconds}s",
                        color = if (timeLeftSeconds <= 10) Color(0xFFF87171) else Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hint Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hint: ${currentWordItem.hint}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Word Display Slots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            targetWord.forEach { char ->
                val isRevealed = guessedLetters.contains(char) || isSolved
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRevealed) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.5.dp,
                        if (isRevealed) Color(0xFF38BDF8) else Color(0xFF475569)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = 28.dp, height = 38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isRevealed) char.toString() else "",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hangman Scaffold / Strike Visualizer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val parts = listOf("Head 👤", "Torso 👕", "L-Arm 💪", "R-Arm 💪", "L-Leg 👖", "R-Leg 👞")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(maxStrikes) { idx ->
                    val isUsed = idx < strikes
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isUsed) Color(0xFFEF4444) else Color(0xFF334155))
                    )
                }
            }

            Text(
                text = if (strikes == 0) "No strikes yet! Stay sharp." else "Strike $strikes/$maxStrikes: ${parts.getOrElse(strikes - 1) { "" }}",
                color = if (strikes >= 4) Color(0xFFEF4444) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // On-Screen QWERTY / Alphabet Keyboard
        val row1 = listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P')
        val row2 = listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L')
        val row3 = listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')

        KeyboardRow(
            letters = row1,
            targetWord = targetWord,
            guessed = guessedLetters,
            onLetterClick = { char ->
                if (!guessedLetters.contains(char) && strikes < maxStrikes && !isSolved) {
                    guessedLetters.add(char)
                    if (!targetWord.contains(char)) {
                        strikes++
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        KeyboardRow(
            letters = row2,
            targetWord = targetWord,
            guessed = guessedLetters,
            onLetterClick = { char ->
                if (!guessedLetters.contains(char) && strikes < maxStrikes && !isSolved) {
                    guessedLetters.add(char)
                    if (!targetWord.contains(char)) {
                        strikes++
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        KeyboardRow(
            letters = row3,
            targetWord = targetWord,
            guessed = guessedLetters,
            onLetterClick = { char ->
                if (!guessedLetters.contains(char) && strikes < maxStrikes && !isSolved) {
                    guessedLetters.add(char)
                    if (!targetWord.contains(char)) {
                        strikes++
                    }
                }
            }
        )

        // Game Over / Win Notice
        if (showGameOverNotice) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF7F1D1D),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Text(
                    text = "⚠️ The word was: $targetWord! Resetting with new word...",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else if (isSolved) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF064E3B),
                border = BorderStroke(1.dp, Color(0xFF34D399))
            ) {
                Text(
                    text = "🎉 Decrypted! $targetWord (+5m Earned)",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    letters: List<Char>,
    targetWord: String,
    guessed: List<Char>,
    onLetterClick: (Char) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        letters.forEach { char ->
            val isGuessed = guessed.contains(char)
            val isCorrect = isGuessed && targetWord.contains(char)
            val isWrong = isGuessed && !targetWord.contains(char)

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    isCorrect -> Color(0xFF059669)
                    isWrong -> Color(0xFF334155).copy(alpha = 0.4f)
                    else -> Color(0xFF1E293B)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isCorrect -> Color(0xFF34D399)
                        isWrong -> Color(0xFF1E293B)
                        else -> Color(0xFF334155)
                    }
                ),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = 30.dp, height = 38.dp)
                    .clickable(enabled = !isGuessed) {
                        onLetterClick(char)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = char.toString(),
                        color = when {
                            isCorrect -> Color.White
                            isWrong -> Color(0xFF64748B)
                            else -> Color(0xFFF1F5F9)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
