package com.swapnil.googlenavigationapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.currentCameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.swapnil.googlenavigationapp.data.RouteStep
import com.swapnil.googlenavigationapp.ui.theme.GoogleNavigationAppTheme
import com.swapnil.googlenavigationapp.viewmodel.LocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swapnil.googlenavigationapp.views.CustomMarkerBitmap
import com.swapnil.googlenavigationapp.views.StepCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean = false
    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    override fun onResume() {
        super.onResume()
        if (locationRequired) {
            startLocationUpdates()
        }
    }
    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {

        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {

            var currentLocation by remember { mutableStateOf(LatLng(0.toDouble(), 0.toDouble())) }
            val cameraPosition = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    currentLocation, 20f
                )
            }
            val origin = LatLng(
                12.868231,
                77.652530
            )//You can add your area location it's for camera position

            val cameraPositionStateNew = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(origin, 15f)
            }

            var cameraPositionState by remember {
                mutableStateOf(cameraPosition)
            }
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    for (location in p0.locations) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        // Get the accuracy radius from the location object
                        cameraPositionState = CameraPositionState(
                            position = CameraPosition.fromLatLngZoom(
                                currentLocation, 20f
                            )
                        )
                    }
                }
            }
            GoogleNavigationAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen(this@MainActivity, currentLocation, cameraPositionStateNew)
                }
            }
        }
    }


    @Composable
    private fun LocationScreen(
        context: Context,
        currentLocation: LatLng,
        cameraPositionState: CameraPositionState,
        viewModel: LocationViewModel = viewModel()
    ) {

        val houses = viewModel.houses
        val locations = viewModel.locations
        val geoFence = viewModel.geoFence

        val latLngList = remember { mutableStateOf<List<LatLng>>(emptyList()) }
        val destination = remember { mutableStateOf(LatLng(12.868388360936915, 77.65279794706925)) }
        val circlePoints = viewModel.generateCirclePoints(currentLocation, 20.00)
        val routeSteps = remember { mutableStateOf<List<RouteStep>>(emptyList()) }

        val transparentSkyBlue = Color(0x3F00BFFF)
        val transparentYellow = Color(0x33FFFF00)
        val geoFenceMarkerColors = remember { mutableStateOf<Map<LatLng, Float>>(emptyMap()) }
        val houseMarkerColors = remember { mutableStateOf<Map<LatLng, Float>>(emptyMap()) }

        var orientationAngle by remember { mutableStateOf(0f) } // Initial orientation angle
        var azimuthDegrees by remember { mutableStateOf(0f) }
        val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
        val accelerometerSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
        val magnetometerSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }

        val originalBitmap: Bitmap = remember { BitmapFactory.decodeResource(context.resources, R.drawable.navigation) }
        val desiredWidth = 150 // Specify the desired width in pixels
        val desiredHeight = 150 // Specify the desired height in pixels
        val resizedBitmap: Bitmap = remember { Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true) }
        val rotatedBitmap: Bitmap = remember(orientationAngle) { viewModel.rotateBitmap(resizedBitmap, orientationAngle) }
        val bitmapDescriptor: BitmapDescriptor = remember(rotatedBitmap) { BitmapDescriptorFactory.fromBitmap(rotatedBitmap) }
        val step1Details = routeSteps.value.getOrNull(0)
        val step2Details = routeSteps.value.getOrNull(1)

        val sensorEventListener = remember {
            object : SensorEventListener {
                private val gravity = FloatArray(3)
                private val geomagnetic = FloatArray(3)
                private var lastUpdateTime = 0L

                override fun onSensorChanged(event: SensorEvent) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime < 100) { // Throttle updates to occur every 100 milliseconds
                        return
                    }

                    lastUpdateTime = currentTime

                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> gravity.apply { event.values.copyInto(this) }
                        Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic.apply { event.values.copyInto(this) }
                    }

                    val rotationMatrix = FloatArray(9)
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                    if (success) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRadians = orientation[0].toDouble()
                        azimuthDegrees = Math.toDegrees(azimuthRadians).toFloat()
                        orientationAngle = azimuthDegrees // Update the orientation angle
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
        }

        val launchMultiplePermissions =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
            { permissionMaps ->
                val areGranted = permissionMaps.values.reduce { acc, next -> acc && next }
                if (areGranted) {
                    locationRequired = true
                    startLocationUpdates()
                    Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

        // Define a function to calculate and display the route to the clicked marker
        suspend fun showRouteToMarker(markerLocation: LatLng) {

            startLocationUpdates()
            // Fetch route from current location to clicked marker's location
            latLngList.value = viewModel.fetchRoute(
                "${currentLocation.latitude},${currentLocation.longitude}",
                "${markerLocation.latitude},${markerLocation.longitude}"
            )!!

            //get steps
            routeSteps.value= viewModel.fetchSteps(
                "${currentLocation.latitude},${currentLocation.longitude}",
                "${destination.value.latitude},${destination.value.longitude}"
            )!!
        }

        // Function to update the color of house markers
        fun updateHouseMarkerColor(location: LatLng, color: Float) {
            houseMarkerColors.value = houseMarkerColors.value + (location to color)
        }

        fun updateGeoFenceMarkerColor(parkLocation: LatLng, color: Float) {
            // Update the color of the park marker in the mutable state
            val updatedColors = geoFenceMarkerColors.value.toMutableMap()
            updatedColors[parkLocation] = color
            geoFenceMarkerColors.value = updatedColors
        }

        LaunchedEffect(currentLocation) {
            if (permissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }) {

                //get location
                startLocationUpdates()
                //get route
                latLngList.value=viewModel.fetchRoute(
                    "${currentLocation.latitude},${currentLocation.longitude}",
                    "${destination.value.latitude},${destination.value.longitude}"
                )!!
                //get steps
                routeSteps.value= viewModel.fetchSteps(
                    "${currentLocation.latitude},${currentLocation.longitude}",
                    "${destination.value.latitude},${destination.value.longitude}"
                )!!
            } else {
                launchMultiplePermissions.launch(permissions)
            }
        }

        LaunchedEffect(destination.value) {
            destination.value?.let { markerLocation ->
                showRouteToMarker(markerLocation)
            }
        }

        LaunchedEffect(orientationAngle) {
            orientationAngle = azimuthDegrees // Update the orientation angle
            // Delay to throttle updates
            delay(100) // Adjust the delay time as needed
        }

        DisposableEffect(Unit) {
            // Register sensor listeners with optimized sampling rate
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorEventListener, magnetometerSensor, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                // Unregister sensor listeners
                sensorManager.unregisterListener(sensorEventListener)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                locations.forEachIndexed { index, location ->
                    val bitmap = CustomMarkerBitmap(index + 1)
                    val customIcon = BitmapDescriptorFactory.fromBitmap(bitmap)

                    // Add the marker to the map
                    Marker(
                        state = rememberMarkerState(position = location),
                        title = "Marker ${index + 1}",
                        snippet = "Marker ${index + 1} in Bengaluru",
                        icon = customIcon,
                        onClick = {
                            // When the marker is clicked, update clickedMarkerLocation state
                            destination.value = location
                            true
                        }
                    )
                }

                houses.forEachIndexed { index, location ->
                    // Add the marker to the map
                    Marker(
                        state = rememberMarkerState(position = location),
                        title = "Marker ${index + 1}",
                        snippet = "Marker ${index + 1} in Bengaluru",
                        icon = BitmapDescriptorFactory.defaultMarker(houseMarkerColors.value[location] ?: BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = {
                            val distance = viewModel.calculateDistance(currentLocation, location)
                            if (distance <= 20.00) { // Assuming the radius of the circles is 10.00 meters
                                // Update the color of the house marker to blue when clicked
                                updateHouseMarkerColor(location, BitmapDescriptorFactory.HUE_GREEN)
                                Toast.makeText(context, "You clicked on Marker ${index + 1} at location $location", Toast.LENGTH_SHORT).show()
                            }
                            false // Return false to indicate that the event is not consumed
                        }
                    )
                }
                Marker(
                    state = MarkerState(position = currentLocation),
                    icon = bitmapDescriptor,
                    title = "current location",
                    snippet = "You are here!!!"
                )
                Polygon(
                    points = circlePoints,
                    fillColor = transparentSkyBlue,
                    strokeColor = Color.Blue,
                    strokeWidth = 5.0f
                )

                Polyline(
                    points = latLngList.value,
                    color = Color(0xff0654c2),
                    width = 15f
                )
                geoFence.forEachIndexed { index,geoFenceMarker ->
                    // Create marker for each geoFenceMarker
                    Marker(
                        state = MarkerState(position = geoFenceMarker.location),
                        icon = BitmapDescriptorFactory.defaultMarker(geoFenceMarkerColors.value[geoFenceMarker.location] ?: BitmapDescriptorFactory.HUE_ORANGE),                        title = "Park",
                        snippet = "Park is here!!!",
                        onClick = {
                            if(viewModel.calculateDistance(currentLocation, geoFenceMarker.location) <= 20.00 + geoFenceMarker.radius)
                            {
                                Toast.makeText(context, "You clicked on Marker ${index + 1} at location ${geoFenceMarker.location}", Toast.LENGTH_SHORT).show()
                                // Update the color of the geoFenceMarker marker to green when clicked
                                updateGeoFenceMarkerColor(geoFenceMarker.location, BitmapDescriptorFactory.HUE_GREEN)
                            }
                            false // Return false to indicate that the event is not consumed
                        }
                    )

                    // Generate the boundary points for each geoFenceMarker's geofence
                    val geoFenceBoundary = viewModel.generateCirclePoints(geoFenceMarker.location, geoFenceMarker.radius)

                    // Draw polygon on the map to represent the geofence around each geoFenceMarker
                    Polygon(
                        points = geoFenceBoundary,
                        fillColor = transparentYellow,
                        strokeColor = colorResource(id = R.color.Orange),
                        strokeWidth = 5.0f
                    )
                }
            }
            if (step1Details != null && step2Details != null) {
                StepCard(currentStep = step1Details, nextStep = step2Details)
            }
        }
    }
}
