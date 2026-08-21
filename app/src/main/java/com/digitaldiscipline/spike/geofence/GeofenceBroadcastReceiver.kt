package com.digitaldiscipline.spike.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitaldiscipline.spike.data.local.DigitalDisciplineDatabase
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.logging.DiagnosticLogger
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode} - $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        val prefs = PreferencesManager(context.applicationContext)
        val database = DigitalDisciplineDatabase.getInstance(context.applicationContext)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        val firstZoneId = triggeringGeofences.firstOrNull()?.requestId ?: ""
                        val zone = if (firstZoneId.isNotBlank()) database.geofenceZoneDao().getZoneById(firstZoneId) else null
                        val zoneName = zone?.name ?: "Designated Focus Zone"

                        Log.i(TAG, "Entered geofence zone: $zoneName (id: $firstZoneId)")
                        prefs.setIsInsideGeofence(true, zoneName)
                        DiagnosticLogger.log(eventType = "GEOFENCE_ENTER", packageName = zoneName, details = "Entered geofence perimeter: $zoneName")
                    }

                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.i(TAG, "Exited geofence zone")
                        prefs.setIsInsideGeofence(false, "")
                        DiagnosticLogger.log(eventType = "GEOFENCE_EXIT", packageName = "ALL", details = "Exited geofence perimeter")
                    }

                    else -> {
                        Log.w(TAG, "Unknown geofence transition: $geofenceTransition")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence transition event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.digitaldiscipline.spike.ACTION_GEOFENCE_EVENT"
    }
}
