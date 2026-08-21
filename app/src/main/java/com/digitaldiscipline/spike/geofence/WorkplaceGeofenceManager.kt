package com.digitaldiscipline.spike.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.digitaldiscipline.spike.data.local.entities.GeofenceZoneEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class WorkplaceGeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context.applicationContext)
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, GEOFENCE_PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation
    }

    fun registerGeofences(
        zones: List<GeofenceZoneEntity>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot register geofences: ACCESS_FINE_LOCATION not granted")
            onFailure(SecurityException("ACCESS_FINE_LOCATION permission not granted"))
            return
        }

        val enabledZones = zones.filter { it.isEnabled && it.latitude != 0.0 && it.longitude != 0.0 }
        if (enabledZones.isEmpty()) {
            removeAllGeofences(onSuccess, onFailure)
            return
        }

        val geofenceList = enabledZones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.radiusMeters.coerceAtLeast(50f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.i(TAG, "Successfully registered ${geofenceList.size} geofence zones")
                    onSuccess()
                }
                addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to register geofences: ${exception.message}", exception)
                    onFailure(exception)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while adding geofences", e)
            onFailure(e)
        }
    }

    fun removeAllGeofences(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i(TAG, "Successfully removed all geofences")
                onSuccess()
            }
            addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofences: ${exception.message}", exception)
                onFailure(exception)
            }
        }
    }

    companion object {
        private const val TAG = "WorkplaceGeofenceMgr"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 9921
    }
}
