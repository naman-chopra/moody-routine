package com.ndev.moodyroutine.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.ndev.moodyroutine.util.AppLogger

object LocationClient {
    private var activeListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun getLastOrCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onResult(null)
            return
        }

        try {
            // Check best last known location across available providers
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null } ?: continue
                if (bestLocation == null || (loc.hasAccuracy() && bestLocation.hasAccuracy() && loc.accuracy < bestLocation.accuracy)) {
                    bestLocation = loc
                }
            }

            if (bestLocation != null) {
                onResult(bestLocation)
                return
            }

            // On Android 30+, query current location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val provider = when {
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> LocationManager.PASSIVE_PROVIDER
                }
                lm.getCurrentLocation(
                    provider,
                    CancellationSignal(),
                    ContextCompat.getMainExecutor(context)
                ) { loc ->
                    onResult(loc)
                }
            } else {
                onResult(null)
            }
        } catch (e: SecurityException) {
            AppLogger.e("LocationClient", "Security exception getting location in FOSS client", e)
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(
        context: Context,
        intervalMs: Long,
        onLocation: (Location) -> Unit
    ) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        try {
            removeLocationUpdates(context)

            val listener = LocationListener { location ->
                onLocation(location)
            }
            activeListener = listener

            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> LocationManager.PASSIVE_PROVIDER
            }

            lm.requestLocationUpdates(
                provider,
                intervalMs,
                0f,
                listener,
                Looper.getMainLooper()
            )
            AppLogger.i("LocationClient", "FOSS LocationClient registered updates on provider: $provider")

            // Immediate initial fix
            getLastOrCurrentLocation(context) { loc ->
                if (loc != null) onLocation(loc)
            }
        } catch (e: SecurityException) {
            AppLogger.e("LocationClient", "Security exception requesting updates in FOSS client", e)
        }
    }

    fun removeLocationUpdates(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        activeListener?.let {
            try {
                lm.removeUpdates(it)
                AppLogger.i("LocationClient", "FOSS LocationClient updates removed")
            } catch (_: Exception) {}
            activeListener = null
        }
    }
}
