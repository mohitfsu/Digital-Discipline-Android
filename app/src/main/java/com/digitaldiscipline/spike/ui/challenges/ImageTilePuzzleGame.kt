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
// 3x3 (9-PIECE) 30-SECOND JUMBLED IMAGE PUZZLE GAME
// ═════════════════════════════════════════════════════════════════════════════

// Pre-defined vibrant tile themes with unique color gradients, symbols & patterns
private val PUZZLE_THEMES = listOf(
    listOf(
        Triple("🌟", Color(0xFFF59E0B), Color(0xFFD97706)),
        Triple("💎", Color(0xFF3B82F6), Color(0xFF1D4ED8)),
        Triple("🍀", Color(0xFF10B981), Color(0xFF047857)),
        Triple("⚡", Color(0xFFEAB308), Color(0xFFCA8A04)),
        Triple("🎯", Color(0xFFEF4444), Color(0xFFB91C1C)),
        Triple("🪐", Color(0xFF8B5CF6), Color(0xFF6D28D9)),
        Triple("🌊", Color(0xFF06B6D4), Color(0xFF0E7490)),
        Triple("🔥", Color(0xFFF97316), Color(0xFFC2410C)),
        Triple("👑", Color(0xFFEC4899), Color(0xFFBE185D))
    ),
    listOf(
        Triple("🚀", Color(0xFF6366F1), Color(0xFF4338CA)),
        Triple("🌙", Color(0xFF38BDF8), Color(0xFF0284C7)),
        Triple("☀️", Color(0xFFFBBF24), Color(0xFFD97706)),
        Triple("🌸", Color(0xFFF472B6), Color(0xFFDB2777)),
        Triple("🌺", Color(0xFFFB7185), Color(0xFFE11D48)),
        Triple("🌿", Color(0xFF34D399), Color(0xFF059669)),
        Triple("🏔️", Color(0xFF94A3B8), Color(0xFF475569)),
        Triple("🌲", Color(0xFF10B981), Color(0xFF065F46)),
        Triple("⭐", Color(0xFFFCD34D), Color(0xFFF59E0B))
    )
)

@Composable
fun ImageTilePuzzleGame(
    timeLimitSeconds: Int = 30,
    onSuccess: () -> Unit
) {
    var themeIndex by remember { mutableIntStateOf(Random.nextInt(PUZZLE_THEMES.size)) }
    val currentTheme = PUZZLE_THEMES[themeIndex % PUZZLE_THEMES.size]

    // Correct solved order: [0, 1, 2, 3, 4, 5, 6, 7, 8]
    val solvedState = remember { List(9) { it } }

    // Shuffled tiles state
    var tiles by remember {
        mutableStateOf(generateShuffledTiles())
    }

    var selectedTileIndex by remember { mutableStateOf<Int?>(null) }
    var timeLeftSeconds by remember { mutableIntStateOf(timeLimitSeconds) }
    var isSolved by remember { mutableStateOf(false) }
    var resetCount by remember { mutableIntStateOf(0) }
    var showTimeoutNotice by remember { mutableStateOf(false) }

    // 30-Second Countdown Timer
    LaunchedEffect(resetCount, isSolved) {
        if (!isSolved) {
            timeLeftSeconds = timeLimitSeconds
            while (timeLeftSeconds > 0 && !isSolved) {
                delay(1000L)
                timeLeftSeconds--
            }
            if (timeLeftSeconds <= 0 && !isSolved) {
                // Timeout! Reshuffle & restart timer
                showTimeoutNotice = true
                delay(800L)
                tiles = generateShuffledTiles()
                selectedTileIndex = null
                showTimeoutNotice = false
                resetCount++
            }
        }
    }

    // Check Win Condition
    fun checkWinCondition(current: List<Int>) {
        if (current == solvedState) {
            isSolved = true
            onSuccess()
        }
    }

    // Handle tile tap (Tap 1st tile, then tap 2nd tile to swap)
    fun onTileClick(index: Int) {
        if (isSolved) return
        val currentSelected = selectedTileIndex
        if (currentSelected == null) {
            selectedTileIndex = index
        } else if (currentSelected == index) {
            selectedTileIndex = null // Deselect
        } else {
            // Swap tiles
            val newTiles = tiles.toMutableList()
            val temp = newTiles[currentSelected]
            newTiles[currentSelected] = newTiles[index]
            newTiles[index] = temp
            tiles = newTiles
            selectedTileIndex = null
            checkWinCondition(newTiles)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Timer & Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🧩 9-Piece Image Puzzle",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tap 2 pieces to swap them. Match #1–9!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            // Circular Countdown Timer Badge
            val timerColor = when {
                timeLeftSeconds <= 5 -> Color(0xFFEF4444)
                timeLeftSeconds <= 10 -> Color(0xFFF59E0B)
                else -> Color(0xFF38BDF8)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = timerColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, timerColor.copy(alpha = 0.6f)),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ ${timeLeftSeconds}s",
                        color = timerColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Timer Bar
        LinearProgressIndicator(
            progress = { (timeLeftSeconds.toFloat() / timeLimitSeconds.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = if (timeLeftSeconds <= 5) Color(0xFFEF4444) else Color(0xFF38BDF8),
            trackColor = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timeout Notification Banner
        AnimatedVisibility(
            visible = showTimeoutNotice,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "⏰ Time's up! Reshuffling puzzle & restarting 30s timer...",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 3x3 Puzzle Board
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, if (isSolved) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val pieceId = tiles[index]
                            val isSelected = selectedTileIndex == index
                            val isCorrectSpot = pieceId == index
                            val themeItem = currentTheme[pieceId]

                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "scale"
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onTileClick(index) }
                                    .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(10.dp)),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(themeItem.second, themeItem.third)
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else if (isSolved) 1.5.dp else 0.5.dp,
                                            color = when {
                                                isSelected -> Color.White
                                                isSolved -> Color(0xFF10B981)
                                                isCorrectSpot -> Color.White.copy(alpha = 0.4f)
                                                else -> Color.Black.copy(alpha = 0.3f)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = themeItem.first,
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            text = "${pieceId + 1}",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Selection highlight ring
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Controls / Status
        if (isSolved) {
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SOLVED! Focus challenge completed!",
                        color = Color(0xFFA7F3D0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTileIndex != null) "Piece #${tiles[selectedTileIndex!!] + 1} selected. Tap destination to swap." else "Tap any piece to select",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = {
                        themeIndex++
                        tiles = generateShuffledTiles()
                        selectedTileIndex = null
                        timeLeftSeconds = timeLimitSeconds
                        resetCount++
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Shuffle", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun generateShuffledTiles(): List<Int> {
    val list = (0..8).toMutableList()
    // Guarantee it is actually jumbled and not already solved
    while (list == (0..8).toList()) {
        list.shuffle()
    }
    return list
}
