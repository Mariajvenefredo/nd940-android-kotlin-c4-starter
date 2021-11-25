package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminder: ReminderDataItem
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var resolutionForResult: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                var accepted = true
                permissions.forEach { permission ->
                    if (!permission.value) {
                        accepted = false
                    }
                }
                if (accepted) {
                    checkPermissionsAndCreateGeofence()
                } else {
                    warningLocationPermission()
                }
            }
        resolutionForResult =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                if (activityResult.resultCode == RESULT_OK) {
                    checkDeviceLocationAndCreateGeofence()
                } else {
                    warningLocationPermission()
                }
            }

        binding.viewModel = _viewModel

        return binding.root
    }

    private fun warningLocationPermission() {
        Snackbar.make(
            binding.saveReminderCLayout,
            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(
                    SaveReminderFragmentDirections
                        .actionSaveReminderFragmentToSelectLocationFragment()
                )
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value ?: 0.0
            val longitude = _viewModel.longitude.value ?: 0.0
            val remind = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(remind)) {
                reminder = remind
                checkPermissionsAndCreateGeofence()
            }
        }
    }

    private fun checkPermissionsAndCreateGeofence() {
        if (locationPermissionApproved()) {
            checkDeviceLocationAndCreateGeofence()
        } else {
            requestLocationPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude,
                reminder.longitude,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(reminder)
                _viewModel.onClear()

                Toast.makeText(
                    requireContext(), R.string.geofence_added,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(), R.string.geofence_not_added,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun checkDeviceLocationAndCreateGeofence() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this.requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("POI_APP", "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.saveReminderCLayout,
                    R.string.error_happened, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                createGeofence()
            }
        }
    }

    private fun requestLocationPermissions() {
        if (locationPermissionApproved()) {
            return
        }
        val permissions = if (_viewModel.runningQOrLater) {
            _viewModel.qPermissionList
        } else {
            _viewModel.beforeQPermissionList
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    @TargetApi(29)
    private fun locationPermissionApproved(): Boolean {
        val foregroundLocationApproved = checkPermission(FINE_LOCATION_PERMISSION)
        val backgroundPermissionApproved =
            if (_viewModel.runningQOrLater) {
                checkPermission(BACKGROUND_LOCATION_PERMISSION)
            } else {
                true
            }
        return foregroundLocationApproved and backgroundPermissionApproved
    }

    private fun checkPermission(
        permission: String
    ): Boolean {
        val appContext = this.requireContext()
        val permissionGranted = PackageManager.PERMISSION_GRANTED

        return permissionGranted ==
                ActivityCompat.checkSelfPermission(
                    appContext,
                    permission
                )
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        const val GEOFENCE_RADIUS_IN_METERS = 50f

        //Geofence expires after 1 month
        private val GEOFENCE_EXPIRATION = TimeUnit.DAYS.toMillis(30)

        @TargetApi(29)
        private const val BACKGROUND_LOCATION_PERMISSION =
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        private const val FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val ACTION_GEOFENCE_EVENT =
            "locationReminders.ACTION_GEOFENCE_EVENT"
    }
}
