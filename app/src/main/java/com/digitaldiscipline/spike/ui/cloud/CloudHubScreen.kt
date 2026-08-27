package com.digitaldiscipline.spike.ui.cloud

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.cloud.AuthState
import com.digitaldiscipline.spike.cloud.CloudRepository
import com.digitaldiscipline.spike.cloud.FirebaseAuthManager
import com.digitaldiscipline.spike.cloud.PairingManager
import com.digitaldiscipline.spike.cloud.models.*
import com.digitaldiscipline.spike.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudHubScreen(
    context: Context,
    coroutineScope: CoroutineScope,
    authManager: FirebaseAuthManager,
    cloudRepository: CloudRepository,
    pairingManager: PairingManager,
    syncManager: SyncManager,
    onNavigateToPairDevice: () -> Unit,
    onBack: () -> Unit
) {
    val authState by authManager.authState.collectAsState()

    var emailInput by remember { mutableStateOf("parent@example.com") }
    var passwordInput by remember { mutableStateOf("Discipline123!") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthLoading by remember { mutableStateOf(false) }

    // Family & Child Management State
    var activeFamily by remember { mutableStateOf<FamilyDto?>(null) }
    var userFamilies by remember { mutableStateOf<List<FamilyDto>>(emptyList()) }
    var newFamilyName by remember { mutableStateOf("") }
    var newChildName by remember { mutableStateOf("") }
    var newChildAge by remember { mutableStateOf("10") }
    var generatedPairingCode by remember { mutableStateOf<String?>(null) }
    var selectedChildForPairing by remember { mutableStateOf<ChildDto?>(null) }
    var isCreatingFamily by remember { mutableStateOf(false) }
    var showCreateNewFamilyForm by remember { mutableStateOf(false) }

    // Cloud Policy Editor State
    var cloudPolicy by remember { mutableStateOf<CloudPolicyDto?>(null) }
    var isPolicySaving by remember { mutableStateOf(false) }

    val durationOptions = listOf(
        10 to "10s (Test)",
        60 to "1 min",
        300 to "5 min",
        600 to "10 min",
        900 to "15 min",
        1800 to "30 min",
        3600 to "60 min"
    )

    // Load parent's own families when signed in
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) {
            val userId = (authState as AuthState.SignedIn).userId
            val res = cloudRepository.getFamiliesForParent(userId)
            val fams = res.getOrNull() ?: emptyList()
            userFamilies = fams
            if (fams.isNotEmpty()) {
                activeFamily = fams.first()
            } else {
                activeFamily = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Cloud Console", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text("◀ Back to Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (authState is AuthState.SignedIn) {
                        TextButton(onClick = {
                            authManager.signOut()
                            activeFamily = null
                            userFamilies = emptyList()
                            generatedPairingCode = null
                            selectedChildForPairing = null
                            cloudPolicy = null
                        }) {
                            Text("Sign Out", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF090D16))
            )
        },
        containerColor = Color(0xFF090D16)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = authState) {
                is AuthState.SignedOut -> {
                    // Admin Authentication Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☁️", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Admin Cloud Console", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Sign in to manage policies across all devices.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Recommended 1-Tap Login
                            Button(
                                onClick = {
                                    authManager.signInWithDevAccount("admin@example.com")
                                    Toast.makeText(context, "Signed in as demo admin", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("⚡ QUICK TEST DEMO LOGIN (Main Workspace)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Sign In with Admin Account:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Admin Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (authError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Auth Notice: $authError",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        isAuthLoading = true
                                        authError = null
                                        coroutineScope.launch {
                                            val res = authManager.signInWithEmail(emailInput.trim(), passwordInput.trim())
                                            isAuthLoading = false
                                            if (res.isFailure) {
                                                authError = res.exceptionOrNull()?.message ?: "Sign in failed"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Sign In as Administrator", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is AuthState.SignedIn -> {
                    val userId = state.userId
                    val userEmail = state.email ?: "admin@digitaldiscipline.com"

                    // Quick Return Bar at top
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("◀ Click here anytime to Return to Child Dashboard", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Dashboard ➔", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Signed In Account Header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF059669), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Admin Console Active", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(userEmail, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("👤", fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Workspace & Managed Devices Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("1. FAMILY GROUP & CHILD DEVICES", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (activeFamily == null || showCreateNewFamilyForm) {
                                Text(
                                    text = if (activeFamily == null) "Create your Family Group to get started:" else "Add Another Family Group:",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newFamilyName,
                                        onValueChange = { newFamilyName = it },
                                        label = { Text("Family Name") },
                                        placeholder = { Text("e.g. My Family / Home") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newFamilyName.isNotBlank()) {
                                                isCreatingFamily = true
                                                coroutineScope.launch {
                                                    val res = cloudRepository.createFamily(newFamilyName.trim(), userId)
                                                    isCreatingFamily = false
                                                    if (res.isSuccess) {
                                                        activeFamily = res.getOrNull()
                                                        showCreateNewFamilyForm = false
                                                        newFamilyName = ""
                                                        Toast.makeText(context, "Family Group created!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        if (isCreatingFamily) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Text("Create")
                                        }
                                    }
                                }

                                if (showCreateNewFamilyForm && activeFamily != null) {
                                    TextButton(onClick = { showCreateNewFamilyForm = false }) {
                                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                // Active Family Banner
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("👨‍👩‍👧 ${activeFamily!!.familyName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Family ID: ${activeFamily!!.familyId.take(12)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                        Text("Protected", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Devices List Flow for this workspace
                                val children by cloudRepository.getChildrenFlow(activeFamily!!.familyId).collectAsState(initial = emptyList())

                                if (children.isEmpty()) {
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "No managed devices in ${activeFamily!!.familyName} yet.\nAdd a device or team member below:",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    Text("Managed Devices in Workspace (${children.size}) — Select device to configure:", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    children.forEach { child ->
                                        val isSelected = selectedChildForPairing?.childId == child.childId
                                        Surface(
                                            onClick = {
                                                selectedChildForPairing = child
                                                coroutineScope.launch {
                                                    val polRes = cloudRepository.getCloudPolicy(activeFamily!!.familyId, child.childId)
                                                    cloudPolicy = polRes.getOrNull()
                                                }
                                            },
                                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("📱 ${child.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(if (isSelected) "▶ Active in Policy Editor" else "Tap to select & edit policy", color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 10.sp)
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Button(
                                                        onClick = {
                                                            selectedChildForPairing = child
                                                            coroutineScope.launch {
                                                                val codeRes = pairingManager.generatePairingCode(
                                                                    familyId = activeFamily!!.familyId,
                                                                    childId = child.childId,
                                                                    childName = child.name,
                                                                    parentId = userId
                                                                )
                                                                if (codeRes.isSuccess) {
                                                                    generatedPairingCode = codeRes.getOrNull()
                                                                }
                                                                val polRes = cloudRepository.getCloudPolicy(activeFamily!!.familyId, child.childId)
                                                                cloudPolicy = polRes.getOrNull()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("Pair Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                cloudRepository.deleteChild(activeFamily!!.familyId, child.childId)
                                                                if (selectedChildForPairing?.childId == child.childId) {
                                                                    selectedChildForPairing = null
                                                                    cloudPolicy = null
                                                                    generatedPairingCode = null
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Device", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Add Device Input Row
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newChildName,
                                        onValueChange = { newChildName = it },
                                        label = { Text("Device / User Name") },
                                        placeholder = { Text("e.g. Work Phone / Alex") },
                                        modifier = Modifier.weight(1.5f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (newChildName.isNotBlank()) {
                                                coroutineScope.launch {
                                                    val res = cloudRepository.createChild(
                                                        familyId = activeFamily!!.familyId,
                                                        name = newChildName.trim(),
                                                        age = newChildAge.toIntOrNull() ?: 18
                                                    )
                                                    if (res.isSuccess) {
                                                        val created = res.getOrNull()
                                                        selectedChildForPairing = created
                                                        val polRes = cloudRepository.getCloudPolicy(activeFamily!!.familyId, created!!.childId)
                                                        cloudPolicy = polRes.getOrNull()
                                                        Toast.makeText(context, "Added ${newChildName.trim()}!", Toast.LENGTH_SHORT).show()
                                                        newChildName = ""
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Text("+ Add Device")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Generated Pairing Code Display
                    if (generatedPairingCode != null && selectedChildForPairing != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔗 SINGLE-USE PAIRING CODE", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("For Device: ${selectedChildForPairing!!.name}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(2.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = generatedPairingCode!!,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("⏱️ Code expires in 15 minutes • Single-use only", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onNavigateToPairDevice,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pair This Phone With This Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 4. Remote Cloud Policy Editor (for selected child)
                    if (activeFamily != null && selectedChildForPairing != null && cloudPolicy != null) {
                        val currentChildName = selectedChildForPairing!!.name
                        val currentChildId = selectedChildForPairing!!.childId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("2. REMOTE POLICY FOR DEVICE: ${currentChildName.uppercase()}", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Text("Active Policy Version: v${cloudPolicy!!.version}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            isPolicySaving = true
                                            coroutineScope.launch {
                                                val nextVersion = cloudPolicy!!.version + 1
                                                val updatedPolicy = cloudPolicy!!.copy(
                                                    version = nextVersion,
                                                    updatedBy = userEmail
                                                )
                                                val res = cloudRepository.saveCloudPolicy(activeFamily!!.familyId, currentChildId, updatedPolicy)
                                                isPolicySaving = false
                                                if (res.isSuccess) {
                                                    cloudPolicy = updatedPolicy
                                                    Toast.makeText(context, "Policy v$nextVersion pushed for $currentChildName!", Toast.LENGTH_SHORT).show()
                                                    syncManager.triggerImmediateSync()
                                                }
                                            }
                                        },
                                        enabled = !isPolicySaving,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Push v${cloudPolicy!!.version + 1} ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Intervention Challenge Settings Card for this Child
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⚙️ INTERVENTION CHALLENGE SETTINGS", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Mindful Pause Dropdown
                                        var pauseExpanded by remember { mutableStateOf(false) }
                                        val pauseOptions = listOf(10 to "10 seconds", 15 to "15 seconds", 30 to "30 seconds")
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Mindful Pause Timer:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                            Box {
                                                OutlinedButton(
                                                    onClick = { pauseExpanded = true },
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("${cloudPolicy!!.pauseDurationSeconds}s ▾", fontSize = 11.sp, color = Color.White)
                                                }
                                                DropdownMenu(expanded = pauseExpanded, onDismissRequest = { pauseExpanded = false }) {
                                                    pauseOptions.forEach { (sec, label) ->
                                                        DropdownMenuItem(
                                                            text = { Text(label, fontSize = 12.sp) },
                                                            onClick = {
                                                                pauseExpanded = false
                                                                cloudPolicy = cloudPolicy!!.copy(pauseDurationSeconds = sec)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Breathing Timer Dropdown
                                        var breathExpanded by remember { mutableStateOf(false) }
                                        val breathOptions = listOf(15 to "15 seconds", 30 to "30 seconds", 60 to "60 seconds")
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Box Breathing Timer:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                            Box {
                                                OutlinedButton(
                                                    onClick = { breathExpanded = true },
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("${cloudPolicy!!.breathingDurationSeconds}s ▾", fontSize = 11.sp, color = Color.White)
                                                }
                                                DropdownMenu(expanded = breathExpanded, onDismissRequest = { breathExpanded = false }) {
                                                    breathOptions.forEach { (sec, label) ->
                                                        DropdownMenuItem(
                                                            text = { Text(label, fontSize = 12.sp) },
                                                            onClick = {
                                                                breathExpanded = false
                                                                cloudPolicy = cloudPolicy!!.copy(breathingDurationSeconds = sec)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Squats Target Dropdown
                                        var squatsExpanded by remember { mutableStateOf(false) }
                                        val squatsOptions = listOf(5 to "5 squats", 10 to "10 squats", 15 to "15 squats", 20 to "20 squats")
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Squats Challenge Reps:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                            Box {
                                                OutlinedButton(
                                                    onClick = { squatsExpanded = true },
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("${cloudPolicy!!.squatsTargetCount} reps ▾", fontSize = 11.sp, color = Color.White)
                                                }
                                                DropdownMenu(expanded = squatsExpanded, onDismissRequest = { squatsExpanded = false }) {
                                                    squatsOptions.forEach { (reps, label) ->
                                                        DropdownMenuItem(
                                                            text = { Text(label, fontSize = 12.sp) },
                                                            onClick = {
                                                                squatsExpanded = false
                                                                cloudPolicy = cloudPolicy!!.copy(squatsTargetCount = reps)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("APP ENFORCEMENT RULES:", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                cloudPolicy!!.rules.forEachIndexed { index, rule ->
                                    var modeExpanded by remember { mutableStateOf(false) }
                                    var durationExpanded by remember { mutableStateOf(false) }

                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rule.appDisplayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Rule: ${rule.mode} • Window: ${if (rule.unlockDurationSeconds < 60) "${rule.unlockDurationSeconds}s" else "${rule.unlockDurationSeconds / 60}m"}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Mode Dropdown
                                                Box {
                                                    OutlinedButton(
                                                        onClick = { modeExpanded = true },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, when (rule.mode) {
                                                            "EARN" -> Color(0xFF38BDF8)
                                                            "BLOCK" -> Color(0xFFEF4444)
                                                            "DELAY" -> Color(0xFFFBBF24)
                                                            else -> Color(0xFF10B981)
                                                        })
                                                    ) {
                                                        Text(
                                                            text = "${rule.mode} ▾",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (rule.mode) {
                                                                "EARN" -> Color(0xFF38BDF8)
                                                                "BLOCK" -> Color(0xFFEF4444)
                                                                "DELAY" -> Color(0xFFFBBF24)
                                                                else -> Color(0xFF10B981)
                                                            }
                                                        )
                                                    }

                                                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                                                        listOf("EARN", "BLOCK", "DELAY", "ALLOW").forEach { mode ->
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        text = when (mode) {
                                                                            "EARN" -> "EARN (Physical Challenge)"
                                                                            "BLOCK" -> "BLOCK (Strict Lock)"
                                                                            "DELAY" -> "DELAY (Mindful Pause)"
                                                                            else -> "ALLOW (Unrestricted)"
                                                                        },
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (rule.mode == mode) FontWeight.Bold else FontWeight.Normal,
                                                                        color = when (mode) {
                                                                            "EARN" -> Color(0xFF38BDF8)
                                                                            "BLOCK" -> Color(0xFFEF4444)
                                                                            "DELAY" -> Color(0xFFFBBF24)
                                                                            else -> Color(0xFF10B981)
                                                                        }
                                                                    )
                                                                },
                                                                onClick = {
                                                                    modeExpanded = false
                                                                    val updatedRules = cloudPolicy!!.rules.toMutableList()
                                                                    updatedRules[index] = rule.copy(mode = mode)
                                                                    cloudPolicy = cloudPolicy!!.copy(rules = updatedRules)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Duration Dropdown
                                                Box {
                                                    OutlinedButton(
                                                        onClick = { durationExpanded = true },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                                    ) {
                                                        val label = durationOptions.find { it.first == rule.unlockDurationSeconds }?.second
                                                            ?: "${rule.unlockDurationSeconds / 60}m"
                                                        Text("$label ▾", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                                    }

                                                    DropdownMenu(expanded = durationExpanded, onDismissRequest = { durationExpanded = false }) {
                                                        durationOptions.forEach { (seconds, label) ->
                                                            DropdownMenuItem(
                                                                text = { Text(label, fontSize = 12.sp) },
                                                                onClick = {
                                                                    durationExpanded = false
                                                                    val updatedRules = cloudPolicy!!.rules.toMutableList()
                                                                    updatedRules[index] = rule.copy(unlockDurationSeconds = seconds)
                                                                    cloudPolicy = cloudPolicy!!.copy(rules = updatedRules)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(top = 6.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Bottom Return Button
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("◀ RETURN TO CHILD DASHBOARD", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                is AuthState.Error -> {
                    Text("Auth Error: ${state.message}", color = Color(0xFFEF4444))
                }
            }
        }
    }
}
