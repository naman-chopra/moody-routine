package com.ndev.moodyroutine.service

import android.content.Context
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import com.ndev.moodyroutine.util.AppLogger

class GeofenceManager(private val context: Context) {
    fun updateGeofences(routines: List<Routine>, modes: List<Mode>) {
        AppLogger.d("GeofenceManager", "FOSS build: Geofence triggers managed via LocationTracker")
    }

    fun removeAllGeofences() {
        AppLogger.d("GeofenceManager", "FOSS build: removeAllGeofences called")
    }
}
