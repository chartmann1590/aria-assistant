package com.aria.assistant.skill

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.permission.PermissionResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    suspend fun currentLocation(): SkillResult<String> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val perm = permissionManager.ensure(PhoneCapability.LOCATION)
        if (perm is PermissionResult.Denied) {
            cont.resume(SkillResult.NeedsPermission(PhoneCapability.LOCATION))
            return@suspendCancellableCoroutine
        }
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val placeName = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "${location.latitude}, ${location.longitude}"
                }
                cont.resume(SkillResult.Success(placeName))
            } else {
                cont.resume(SkillResult.Failure("Could not determine location"))
            }
        }.addOnFailureListener { e ->
            cont.resume(SkillResult.Failure("Location unavailable: ${e.message}"))
        }
    }

    fun reverseGeocode(lat: Double, lng: Double): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.LOCATION)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.LOCATION)
        }
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                SkillResult.Success(addresses[0].getAddressLine(0))
            } else {
                SkillResult.Failure("No address found for that location")
            }
        } catch (e: Exception) {
            SkillResult.Failure("Geocoding failed: ${e.message}")
        }
    }

    fun navigateTo(place: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.LOCATION)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.LOCATION)
        }
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(place)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return SkillResult.Success("Opening navigation to $place")
        }
        return SkillResult.Failure("No navigation app available")
    }

    fun nearby(query: String): SkillResult<String> {
        val perm = permissionManager.ensure(PhoneCapability.LOCATION)
        if (perm is PermissionResult.Denied) {
            return SkillResult.NeedsPermission(PhoneCapability.LOCATION)
        }
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return SkillResult.Success("Searching nearby for $query")
        }
        return SkillResult.Failure("No maps app available")
    }
}
