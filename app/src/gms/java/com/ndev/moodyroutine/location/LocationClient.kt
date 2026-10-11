package com.ndev.moodyroutine.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.ndev.moodyroutine.util.AppLogger

object LocationClient {
    private var activeCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun getLastOrCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult(loc)
                } else {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { curLoc ->
                            onResult(curLoc)
                        }
                        .addOnFailureListener {
                            onResult(null)
                        }
                }
            }.addOnFailureListener {
                onResult(null)
            }
        } catch (e: SecurityException) {
            AppLogger.e("LocationClient", "Security exception getting location in GMS client", e)
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(
        context: Context,
        intervalMs: Long,
        onLocation: (Location) -> Unit
    ) {
        try {
            removeLocationUpdates(context)
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                intervalMs
            ).apply {
                setMinUpdateIntervalMillis(intervalMs / 2)
                setMaxUpdateDelayMillis((intervalMs * 1.5).toLong())
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    onLocation(loc)
                }
            }
            activeCallback = callback

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            AppLogger.i("LocationClient", "GMS LocationClient registered updates")

            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onLocation(loc)
            }
        } catch (e: SecurityException) {
            AppLogger.e("LocationClient", "Security exception requesting updates in GMS client", e)
        }
    }

    fun removeLocationUpdates(context: Context) {
        val callback = activeCallback ?: return
        try {
            LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(callback)
            AppLogger.i("LocationClient", "GMS LocationClient updates removed")
        } catch (_: Exception) {}
        activeCallback = null
    }
}
