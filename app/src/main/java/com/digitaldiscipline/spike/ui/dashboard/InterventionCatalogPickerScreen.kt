package com.digitaldiscipline.spike.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.intervention.catalog.InterventionCatalog
import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionCatalogPickerScreen(
    coroutineScope: CoroutineScope,
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allInterventions = remember { InterventionCatalog.getAllInterventions() }
    val savedEnabledIds by preferencesManager.enabledInterventionsFlow.collectAsState(initial = emptySet())

    // If user hasn't saved custom subset yet, all 35 are active by default
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(savedEnabledIds) {
        if (!hasInitialized) {
            selectedIds = if (savedEnabledIds.isEmpty()) {
                allInterventions.map { it.id }.toSet()
            } else {
                savedEnabledIds
            }
            hasInitialized = true
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<InterventionCategory, Boolean>() }

    // By default expand all
    LaunchedEffect(Unit) {
        InterventionCategory.values().forEach {
            expandedCategories[it] = true
        }
    }

    val filteredInterventions = remember(searchQuery, allInterventions) {
        if (searchQuery.isBlank()) allInterventions
        else allInterventions.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedByCategory = remember(filteredInterventions) {
        filteredInterventions.groupBy { it.category }
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable { onNavigateBack() }
                            ) {
                                Text(
                                    text = "← Back",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "INTERVENTIONS",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Active Friction Catalog",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    preferencesManager.setEnabledInterventions(selectedIds)
                                    val activeCategories = allInterventions
                                        .filter { selectedIds.contains(it.id) }
                                        .map { it.category.name }
                                        .toSet()
                                    preferencesManager.setEnabledCategories(activeCategories)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Saved ${selectedIds.size} active interventions", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Save (${selectedIds.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search 35 interventions (e.g. pushups, breathing, reading)...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedIds.size} of ${allInterventions.size} Selected",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cross-select fitness, reading, breathing freely",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedIds = allInterventions.map { it.id }.toSet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Select All", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedIds = emptySet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Guidance Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Digital Discipline adapts to what works for you. Enable the types of friction you enjoy — like combining Fitness with Reading or Breathing.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            InterventionCategory.values().forEach { category ->
                val categoryItems = groupedByCategory[category] ?: emptyList()
                if (categoryItems.isNotEmpty()) {
                    val isExpanded = expandedCategories[category] ?: true
                    val categorySelectedCount = categoryItems.count { selectedIds.contains(it.id) }
                    val isAllCategorySelected = categorySelectedCount == categoryItems.size

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (categorySelectedCount > 0) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories[category] = !isExpanded
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (category) {
                                            InterventionCategory.MOVEMENT -> "🏃"
                                            InterventionCategory.UPPER_BODY -> "💪"
                                            InterventionCategory.BREATHING -> "🫁"
                                            InterventionCategory.MEDITATION -> "🧘"
                                            InterventionCategory.YOGA_MOBILITY -> "🧘‍♂️"
                                            InterventionCategory.PHYSICAL_RESET -> "💧"
                                            InterventionCategory.COGNITIVE -> "🧠"
                                            InterventionCategory.CREATIVE_FLOW -> "🎨"
                                            InterventionCategory.MINDFUL_PERSPECTIVE -> "🔮"
                                        },
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = formatCategoryTitle(category),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$categorySelectedCount of ${categoryItems.size} active",
                                            color = if (categorySelectedCount > 0) Color(0xFF34D399) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAllCategorySelected) "Deselect All" else "Select All",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                selectedIds = if (isAllCategorySelected) {
                                                    selectedIds - categoryItems.map { it.id }.toSet()
                                                } else {
                                                    selectedIds + categoryItems.map { it.id }.toSet()
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categoryItems.forEach { item ->
                                        val isSelected = selectedIds.contains(item.id)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF131C2E).copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedIds = if (isSelected) {
                                                        selectedIds - item.id
                                                    } else {
                                                        selectedIds + item.id
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(item.iconEmoji, fontSize = 20.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = item.title,
                                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = item.description,
                                                            color = Color(0xFF64748B),
                                                            fontSize = 11.sp,
                                                            maxLines = 2
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val durationOrReps = if (item.defaultReps > 0) "${item.defaultReps} reps" else "${item.defaultDurationSeconds}s"
                                                        Text(
                                                            text = "$durationOrReps • +${item.rewardSeconds / 60}m earned",
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedIds = if (checked) {
                                                            selectedIds + item.id
                                                        } else {
                                                            selectedIds - item.id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF0284C7),
                                                        uncheckedColor = Color(0xFF475569)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatCategoryTitle(category: InterventionCategory): String {
    return when (category) {
        InterventionCategory.MOVEMENT -> "Movement & Fitness"
        InterventionCategory.UPPER_BODY -> "Upper Body & Posture"
        InterventionCategory.BREATHING -> "Breathing Exercises"
        InterventionCategory.MEDITATION -> "Mindfulness & Meditation"
        InterventionCategory.YOGA_MOBILITY -> "Yoga & Mobility"
        InterventionCategory.PHYSICAL_RESET -> "Physical Reset"
        InterventionCategory.COGNITIVE -> "Cognitive & Reading"
        InterventionCategory.CREATIVE_FLOW -> "Creative Flow & Expression"
        InterventionCategory.MINDFUL_PERSPECTIVE -> "Mindful Perspective & Audio"
    }
}
