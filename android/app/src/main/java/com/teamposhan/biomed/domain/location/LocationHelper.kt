package com.teamposhan.biomed.domain.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.Locale

/**
 * Location capture and reverse geocoding for field workers.
 *
 * Design decisions:
 *  - Uses the platform [LocationManager] (no Play Services dependency, so it works on any
 *    phone and offline).
 *  - Actively requests a fix and only falls back to the last known location, because on a
 *    phone where no other app has recently asked for location every provider's last-known
 *    value is null — which is why this used to silently return nothing.
 *  - Reverse geocoding uses the platform [Geocoder]. On most devices with Play Services
 *    this resolves a street/village name; where the service is unavailable (many emulators,
 *    some budget handsets) it degrades gracefully to a readable coordinate string instead
 *    of failing.
 *  - If you supply a Google Maps Geocoding API key, [setMapsApiKey] switches to the
 *    network geocoder, which is considerably more accurate in rural India.
 */
object LocationHelper {

    private var mapsApiKey: String? = null

    /** Optional: call once at startup with a Google Maps Geocoding API key. */
    fun setMapsApiKey(key: String?) {
        mapsApiKey = key?.takeIf { it.isNotBlank() }
    }

    data class Result(
        val label: String,
        val latitude: Double,
        val longitude: Double,
        val accurate: Boolean,
        val source: Source,
    )

    enum class Source { GPS, LAST_KNOWN, COORDINATES_ONLY, NETWORK_GEOCODER, UNAVAILABLE }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Why a location attempt produced nothing — so the UI can say something useful. */
    enum class Failure { NO_PERMISSION, LOCATION_OFF, NO_FIX }

    /** Everything a caller needs: the fix, or the reason there isn't one. */
    sealed interface Outcome {
        data class Found(val result: Result) : Outcome
        data class Failed(val reason: Failure) : Outcome
    }

    /** True when the user has location switched on at the system level. */
    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /** Blocking-safe entry point; call from a coroutine. */
    suspend fun current(context: Context): Result? =
        (locate(context) as? Outcome.Found)?.result

    /**
     * Requests a location, reporting why it failed when it does.
     *
     * @param timeoutMs how long to wait for a live fix before falling back. A cold GPS
     *   start outdoors is typically 5-15s; indoors it may never arrive, which is what the
     *   last-known fallback is for.
     */
    suspend fun locate(context: Context, timeoutMs: Long = 15_000): Outcome = withContext(Dispatchers.Main) {
        if (!hasPermission(context)) return@withContext Outcome.Failed(Failure.NO_PERMISSION)
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext Outcome.Failed(Failure.LOCATION_OFF)
        if (!isLocationEnabled(context)) return@withContext Outcome.Failed(Failure.LOCATION_OFF)

        // A live fix first; last-known only as the fallback.
        val location = requestFix(context, manager, timeoutMs)
            ?: lastKnown(manager)
            ?: return@withContext Outcome.Failed(Failure.NO_FIX)

        val label = withContext(Dispatchers.IO) {
            reverseGeocode(context, location.latitude, location.longitude)
        }
        Outcome.Found(
            Result(
                label = label.text,
                latitude = location.latitude,
                longitude = location.longitude,
                accurate = label.resolved,
                source = label.source,
            )
        )
    }

    /**
     * An active single-shot request. The listener is registered on the main looper and
     * always removed, so a request that times out does not keep the GPS radio awake.
     */
    @Suppress("MissingPermission")
    private suspend fun requestFix(
        context: Context,
        manager: LocationManager,
        timeoutMs: Long,
    ): Location? {
        if (!hasPermission(context)) return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(location)
                    }

                    // Required on API < 30; the default implementations are final no-ops above it.
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }

                val started = runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.isSuccess
                if (!started && continuation.isActive) continuation.resume(null)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers.mapNotNull { p ->
            runCatching {
                if (manager.isProviderEnabled(p)) manager.getLastKnownLocation(p) else null
            }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private data class GeocodeResult(val text: String, val resolved: Boolean, val source: Source)

    private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): GeocodeResult {
        // 1. Network geocoder when a Maps key is available (best for rural addresses).
        mapsApiKey?.let { key ->
            networkGeocode(lat, lon, key)?.let {
                return GeocodeResult(it, true, Source.NETWORK_GEOCODER)
            }
        }

        // 2. Platform geocoder.
        val platform = platformGeocode(context, lat, lon)
        if (platform != null) return platform

        // 3. Always-available fallback so the field worker still gets something usable.
        return GeocodeResult(coordinateLabel(lat, lon), false, Source.COORDINATES_ONLY)
    }

    @Suppress("DEPRECATION")
    private suspend fun platformGeocode(context: Context, lat: Double, lon: Double): GeocodeResult? {
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(6_000) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val latch = java.util.concurrent.CountDownLatch(1)
                        var outcome: GeocodeResult? = null
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                outcome = addresses.firstOrNull()?.let { GeocodeResult(format(it), true, Source.GPS) }
                                latch.countDown()
                            }
                            override fun onError(errorMessage: String?) {
                                latch.countDown()
                            }
                        })
                        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                        outcome
                    } else {
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        addresses?.firstOrNull()?.let { GeocodeResult(format(it), true, Source.GPS) }
                    }
                }.getOrNull()
            }
        }
    }

    private suspend fun networkGeocode(lat: Double, lon: Double, key: String): String? =
        withTimeoutOrNull(8_000) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val url = java.net.URL(
                        "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&key=$key&language=en"
                    )
                    val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 6_000
                        readTimeout = 6_000
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val root = json.parseToJsonElement(body).let { it as? kotlinx.serialization.json.JsonObject }
                        ?: return@runCatching null
                    val results = root["results"] as? kotlinx.serialization.json.JsonArray ?: return@runCatching null
                    val first = results.firstOrNull() as? kotlinx.serialization.json.JsonObject ?: return@runCatching null
                    (first["formatted_address"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                }.getOrNull()
            }
        }

    private fun format(address: android.location.Address): String {
        val parts = listOfNotNull(
            address.locality ?: address.subAdminArea,
            address.subLocality,
            address.adminArea,
            address.postalCode,
        ).distinct()
        return if (parts.isNotEmpty()) parts.joinToString(", ") else coordinateLabel(address.latitude, address.longitude)
    }

    fun coordinateLabel(lat: Double, lon: Double): String =
        "%.5f, %.5f".format(lat, lon)
}
