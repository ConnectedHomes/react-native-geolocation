package co.uk.hive.reactnativegeolocation.sample

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.uk.hive.reactnativegeolocation.geofence.GeofenceLog
import co.uk.hive.reactnativegeolocation.geofence.GeofenceServiceLocator
import co.uk.hive.reactnativegeolocation.location.CurrentPositionRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStatusCodes


class MainActivity : AppCompatActivity() {

    companion object {
        const val LOCATION_PERMISSION_CODE = 101
        const val BACKGROUND_LOCATION_PERMISSION_CODE = 102
        const val LOCATION_ACCURACY_RESOLUTION_CODE = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun log(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        GeofenceLog.d(message)
    }

    // <<========== GET LOCATION ===========>>

    @RequiresApi(Build.VERSION_CODES.N)
    fun getLocation(view: View) {
        val controller = GeofenceServiceLocator.getLocationController(this)
        if (!controller.isLocationEnabled) {
            log("Location not enabled!")
        }
        if (!controller.hasPermissions()) {
            log("Permissions not granted!")
            checkPermission()
        }

        // Attempt to get current location!
        controller.getCurrentPosition(CurrentPositionRequest(10000), {
            // Success
            log("${it.latitude} / ${it.longitude}")
        }, {
            // Failed
            if (it is ResolvableApiException) {
                val status = it.status
                if (status.statusCode == LocationSettingsStatusCodes.SUCCESS) {
                    log("LocationSettingsStatusCodes.SUCCESS")
                }
                if (status.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(this@MainActivity, LOCATION_ACCURACY_RESOLUTION_CODE)
                    } catch (e: IntentSender.SendIntentException) {
                        log("PendingIntent unable to execute request.")
                    }
                }
                if (status.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    log("Location settings are inadequate, and cannot be fixed here.")
                }
            } else {
                log(it.toString())
            }
        })
        return
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fine Location permission is granted
            // Check if current android version >= 11, if >= 11 check for Background Location permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Background Location Permission is granted so do your work here
                    log("All required permissions granted!")
                } else {
                    // Ask for Background Location Permission
                    askPermissionForBackgroundUsage()
                }
            }
        } else {
            // Fine Location Permission is not granted so ask for permission
            askForLocationPermission()
        }
    }

    private fun askForLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Location Permission Needed!")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE) })
                    .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog, which ->
                        // Permission is denied by the user
                        log("ACCESS_FINE_LOCATION denied!")
                    })
                    .create().show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun askPermissionForBackgroundUsage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Background Location Permission Needed!, tap \"Allow all time in the next screen\"")
                    .setPositiveButton("OK") { dialog, which -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_PERMISSION_CODE) }
                    .setNegativeButton("CANCEL") { dialog, which ->
                        // User declined for Background Location Permission.
                        log("ACCESS_BACKGROUND_LOCATION denied!")
                    }
                    .create().show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted location permission
                // Now check if android version >= 11, if >= 11 check for Background Location Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Background Location Permission is granted so do your work here
                        log("ACCESS_BACKGROUND_LOCATION already granted!")
                    } else {
                        // Ask for Background Location Permission
                        askPermissionForBackgroundUsage()
                    }
                }
            } else {
                // User denied location permission
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted for Background Location Permission.
                log("ACCESS_BACKGROUND_LOCATION granted!")
            } else {
                // User declined for Background Location Permission.
                log("ACCESS_BACKGROUND_LOCATION denied!")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_ACCURACY_RESOLUTION_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    log("Location accuracy setting enabled!")
                }
                RESULT_CANCELED -> {
                    log("User cancelled location accuracy improvement request!")
                }
                else -> {}
            }
        }
    }

    // <<========== END OF GET LOCATION ===========>>
}