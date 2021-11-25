package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*


@TargetApi(29)
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var locationCallback: LocationCallback
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var resolutionForResult: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var currentLocation: Location
    private lateinit var lastMarker: Marker

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.apply {
            viewModel = _viewModel
            selectPoint.text = context?.getText(R.string.select_poi) ?: ""
        }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this.requireActivity())

        locationCallback = handleLocationCallback()
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    map.isMyLocationEnabled = true
                    turnOnLocationRequest()
                } else {
                    warningLocationPermissions()
                    moveCamara(LATLNG_GOOGLE)
                }
            }
        resolutionForResult =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    checkPermissionsAndEnableMyLocation()
                } else {
                    moveCamara(LATLNG_GOOGLE)
                }
            }
        return binding.root
    }

    private fun turnOnLocationRequest() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

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
                    Log.d(
                        "POI_APP",
                        "Error geting location settings resolution: " + sendEx.message
                    )
                }
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                trackLocation()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        setMapLongClick(map)
        setMapStyle(map)
        checkPermissionsAndEnableMyLocation()
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

        } catch (e: Resources.NotFoundException) {
            Log.e("MAP", "Can't find style. Error: ", e)
        }
    }

    private fun warningLocationPermissions() {
        Snackbar.make(
            binding.layoutMap,
            R.string.permission_denied_explanation, Snackbar.LENGTH_SHORT
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

    private fun handleLocationCallback() = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            var lastLocation = locationResult.lastLocation
            if (::currentLocation.isInitialized) {
                if (currentLocation.latitude != lastLocation.latitude ||
                    currentLocation.longitude != lastLocation.longitude
                ) {
                    currentLocation = locationResult.lastLocation
                    getCurrentLocation()
                }
            } else {
                currentLocation = locationResult.lastLocation
                getCurrentLocation()
            }
        }
    }

    private fun onLocationSelected(latLng: LatLng) {
        val poiName = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )
        val poi = PointOfInterest(latLng, "POI", poiName)

        _viewModel.selectedPOI.value = poi
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            _viewModel.selectedPOI.value = poi
            _viewModel.reminderTitle.value = poi.name
            val latLng = poi.latLng

            map.handleMapClick(latLng)
        }
        map.setOnMapLongClickListener { latLng ->
            map.handleMapClick(latLng)
        }
    }

    private fun GoogleMap.handleMapClick(latLng: LatLng) {
        if (::lastMarker.isInitialized) {
            lastMarker.remove()
        }

        lastMarker = addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.dropped_pin))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        )

        moveCamara(latLng)

        binding.apply {
            confirmPOI.setOnClickListener {
                onLocationSelected(latLng)
            }

            discardPOI.setOnClickListener {
                confirmPOI.visibility = View.GONE
                discardPOI.visibility = View.GONE
                lastMarker.remove()
                if (::currentLocation.isInitialized) {
                    moveCamara(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    )
                }
            }

            confirmPOI.visibility = View.VISIBLE
            discardPOI.visibility = View.VISIBLE
        }
    }

    private fun checkPermissionsAndEnableMyLocation() {

        if (checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        } else {
            turnOnLocationRequest()
        }
    }

    @SuppressLint("MissingPermission")
    private fun trackLocation() {
        val locationRequest = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(2000)

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private fun getCurrentLocation() {
        if (::currentLocation.isInitialized) {
            val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            moveCamara(latLng)
        }
    }

    private fun moveCamara(
        latLng: LatLng
    ) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL)
        map.animateCamera(cameraUpdate)
    }

    companion object {
        private const val ZOOM_LEVEL = 15f
        private const val LAT_GOOGLE = 37.42229563649184
        private const val LNG_GOOGLE = -122.08422917783443
        private val LATLNG_GOOGLE = LatLng(LAT_GOOGLE, LNG_GOOGLE)
    }
}

