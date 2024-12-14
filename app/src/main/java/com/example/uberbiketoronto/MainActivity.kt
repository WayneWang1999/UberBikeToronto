package com.example.uberbiketoronto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.uberbiketoronto.data.Bike
import com.example.uberbiketoronto.databinding.ActivityMainBinding
import com.example.uberbiketoronto.ui.BikeDetailsFragment
import com.example.uberbiketoronto.util.BikeDataInputs
import com.example.uberbiketoronto.viewmodels.BikeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

//Change the program to calculate the area bikes, if more than average bikes, should move one bike
//to the less than average area.

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap

    private val bikeViewModel: BikeViewModel by viewModels()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_LATITUDE = 43.678053
        private const val DEFAULT_LONGITUDE = -79.409523
        private const val DEFAULT_ZOOM = 12f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupMapFragment()
        binding.titleTextView.setOnClickListener {
            BikeDataInputs(this).generateRandomBikeAndUpload()
        }

        if (!isLocationPermissionGranted()) {
            requestLocationPermission()
        }
    }

    private fun setupUI() {
        binding.swIsReturned.setOnCheckedChangeListener { _, isChecked ->
            // Fetch and apply filter when switch toggles
            bikeViewModel.fetchBikeLocations(isChecked)
            bikeViewModel.startRealTimeListener(isChecked) // Real-time updates with the new filter
        }

        // Default load: show not returned bikes
        bikeViewModel.fetchBikeLocations(false)

        // Observe LiveData for bike locations
        bikeViewModel.bikeLocations.observe(this) { bikes ->
            updateMapMarkers(bikes)
        }

        // Observe error message
        bikeViewModel.errorMessage.observe(this) { message ->
            showToast(message)
        }
    }

    private fun setupMapFragment() {
        val mapFragment = binding.mapFragmentContainer.getFragment<SupportMapFragment>()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        configureMapDefaults()
        drawMapGrid()

        googleMap.setOnMarkerClickListener { marker ->
            val bikeName = marker.title ?: "Unknown Bike"
            val bikeAddress = marker.snippet ?: "Unknown Address"
            showBikeDetails(bikeName, bikeAddress)
            true
        }
    }

    private fun showBikeDetails(bikeName: String, bikeAddress: String) {
        val bikeDetailsFragment = BikeDetailsFragment.newInstance(bikeName, bikeAddress,)
        bikeDetailsFragment.show(supportFragmentManager, BikeDetailsFragment::class.java.simpleName)
    }

    private fun configureMapDefaults() {
        googleMap.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_ZOOM))
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isZoomGesturesEnabled = true
        }
    }

    private fun updateMapMarkers(bikes: List<Bike>) {
        googleMap.clear()
        drawMapGrid()

        if (bikes.isEmpty()) {
            showToast("No bikes available.")
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        bikes.forEach { bike ->
            val location = LatLng(bike.latitude, bike.longitude)
            val bitmapDescriptor = createBitmapDescriptor(R.drawable.bicycleorange)
            googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(bike.bikeName)
                    .snippet(bike.address)
                    .icon(bitmapDescriptor)
            )
            boundsBuilder.include(location)
        }

        adjustCameraToMarkers(boundsBuilder)
    }

    private fun adjustCameraToMarkers(boundsBuilder: LatLngBounds.Builder) {
        val bounds = boundsBuilder.build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        showToast("Bike locations loaded.")
    }

    private fun drawMapGrid() {
        val bounds = LatLngBounds(
            LatLng(43.65, -79.52), // Southwest corner
            LatLng(43.80, -79.32)  // Northeast corner
        )
        drawGrid(googleMap, bounds, 0.010)
    }

    private fun drawGrid(map: GoogleMap, bounds: LatLngBounds, gridSpacing: Double) {
        val gridLines = mutableListOf<PolylineOptions>()

        var lng = bounds.southwest.longitude
        while (lng <= bounds.northeast.longitude) {
            gridLines.add(PolylineOptions().add(LatLng(bounds.southwest.latitude, lng), LatLng(bounds.northeast.latitude, lng)).color(ContextCompat.getColor(this, R.color.black)).width(2f))
            lng += gridSpacing
        }

        var lat = bounds.southwest.latitude
        while (lat <= bounds.northeast.latitude) {
            gridLines.add(PolylineOptions().add(LatLng(lat, bounds.southwest.longitude), LatLng(lat, bounds.northeast.longitude)).color(ContextCompat.getColor(this, R.color.black)).width(2f))
            lat += gridSpacing
        }

        gridLines.forEach { map.addPolyline(it) }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Location permission granted.")
            } else {
                showToast("Location permission denied.")
            }
        }
    }

    private fun createBitmapDescriptor(resourceId: Int): BitmapDescriptor {
        val options = BitmapFactory.Options().apply { inSampleSize = 15 }
        val bitmap = BitmapFactory.decodeResource(resources, resourceId, options)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}