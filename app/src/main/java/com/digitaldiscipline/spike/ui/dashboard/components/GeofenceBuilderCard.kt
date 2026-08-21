package com.digitaldiscipline.spike.ui.dashboard.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldiscipline.spike.data.local.entities.GeofenceZoneEntity
import com.google.android.gms.location.LocationServices
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GeofenceBuilderCard(
    zones: List<GeofenceZoneEntity>,
    isInsideGeofence: Boolean,
    activeGeofenceName: String,
    onSaveZone: (GeofenceZoneEntity) -> Unit,
    onToggleZone: (GeofenceZoneEntity, Boolean) -> Unit,
    onDeleteZone: (GeofenceZoneEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<GeofenceZoneEntity?>(null) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📍", fontSize = 18.sp)
                        Text(
                            text = "WORKPLACE & SCHOOL GEOFENCES",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = if (isInsideGeofence && activeGeofenceName.isNotBlank()) "🟢 ACTIVE: Inside $activeGeofenceName" else "Native perimeter-based focus boundaries",
                        color = if (isInsideGeofence) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isInsideGeofence) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = {
                        editingZone = null
                        showDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+ Add Zone", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (zones.isEmpty()) {
                // Empty state
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📍", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Geofence Boundaries Configured",
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add office, school, or library geofences to automatically silence and block distracting apps whenever the device arrives on site.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Zone List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    zones.forEach { zone ->
                        val isThisActive = isInsideGeofence && (activeGeofenceName.equals(zone.name, ignoreCase = true) || zones.size == 1)
                        GeofenceItemCard(
                            zone = zone,
                            isActiveNow = isThisActive,
                            onToggle = { isEnabled -> onToggleZone(zone, isEnabled) },
                            onEdit = {
                                editingZone = zone
                                showDialog = true
                            },
                            onDelete = { onDeleteZone(zone) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        ConfigurableGeofenceDialog(
            context = context,
            initialZone = editingZone,
            onDismiss = { showDialog = false },
            onSave = { saved ->
                onSaveZone(saved)
                showDialog = false
            }
        )
    }
}

@Composable
private fun GeofenceItemCard(
    zone: GeofenceZoneEntity,
    isActiveNow: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActiveNow) Color(0xFF0C243B) else Color(0xFF1E293B).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isActiveNow) Color(0xFF0284C7) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = zone.name.ifBlank { "Focus Zone" },
                        color = Color(0xFFF8FAFC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActiveNow) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "INSIDE ZONE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Coordinates & Radius
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📌", fontSize = 12.sp)
                Text(
                    text = String.format(Locale.US, "%.4f, %.4f • %dm radius", zone.latitude, zone.longitude, zone.radiusMeters.roundToInt()),
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text("•", color = Color(0xFF64748B), fontSize = 12.sp)
                Text(
                    text = if (zone.restrictionMode == "BLOCK") "⛔ Strict Block" else "🧘 30s Reset",
                    color = if (zone.restrictionMode == "BLOCK") Color(0xFFF87171) else Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConfigurableGeofenceDialog(
    context: Context,
    initialZone: GeofenceZoneEntity?,
    onDismiss: () -> Unit,
    onSave: (GeofenceZoneEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialZone?.name ?: "Main Office HQ") }
    var zoneType by remember { mutableStateOf(initialZone?.zoneType ?: "WORKPLACE") }
    var latStr by remember { mutableStateOf(initialZone?.latitude?.toString() ?: "0.0") }
    var lngStr by remember { mutableStateOf(initialZone?.longitude?.toString() ?: "0.0") }
    var radius by remember { mutableFloatStateOf(initialZone?.radiusMeters ?: 200f) }
    var restrictionMode by remember { mutableStateOf(initialZone?.restrictionMode ?: "BLOCK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialZone == null) "Add Workplace / School Geofence" else "Edit Geofence",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Presets
                Text("Quick Presets:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable {
                            name = "Main Office HQ"
                            zoneType = "WORKPLACE"
                            radius = 200f
                            restrictionMode = "BLOCK"
                        }
                    ) {
                        Text("🏢 Office HQ", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable {
                            name = "School Campus"
                            zoneType = "SCHOOL"
                            radius = 300f
                            restrictionMode = "BLOCK"
                        }
                    ) {
                        Text("👨‍👩‍👧 School", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable {
                            name = "City Library"
                            zoneType = "LIBRARY"
                            radius = 150f
                            restrictionMode = "INTERVENE"
                        }
                    ) {
                        Text("📚 Library", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }

                // Zone Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Zone Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Location GPS coordinate picker
                Button(
                    onClick = {
                        try {
                            val fused = LocationServices.getFusedLocationProviderClient(context)
                            fused.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    latStr = loc.latitude.toString()
                                    lngStr = loc.longitude.toString()
                                }
                            }
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📍 Use Current Location", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngStr,
                        onValueChange = { lngStr = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Radius Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Perimeter Radius:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${radius.roundToInt()} meters", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..1000f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0284C7),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                // Restriction Mode
                Column {
                    Text("Restriction Mode:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (restrictionMode == "BLOCK") Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (restrictionMode == "BLOCK") Color(0xFFEF4444) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { restrictionMode = "BLOCK" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⛔ Strict Block", color = if (restrictionMode == "BLOCK") Color.White else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Admin PIN", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (restrictionMode == "INTERVENE") Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (restrictionMode == "INTERVENE") Color(0xFF10B981) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { restrictionMode = "INTERVENE" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧘 30s Reset", color = if (restrictionMode == "INTERVENE") Color.White else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Camera AI", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull() ?: 0.0
                    val lng = lngStr.toDoubleOrNull() ?: 0.0
                    val zoneToSave = (initialZone ?: GeofenceZoneEntity()).copy(
                        name = name.ifBlank { "Focus Zone" },
                        zoneType = zoneType,
                        latitude = lat,
                        longitude = lng,
                        radiusMeters = radius,
                        restrictionMode = restrictionMode,
                        isEnabled = true
                    )
                    onSave(zoneToSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialZone == null) "Create Geofence" else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(18.dp)
    )
}
