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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.swapnil.googlenavigationapp.ui.theme.GoogleNavigationAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                18.878460,
                72.930399
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
        cameraPositionState: CameraPositionState
    ) {
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
        // Define a mutable list to store the route points
        val latLngList = remember { mutableStateOf<List<LatLng>>(emptyList()) }
        val circlePoints = generateCirclePoints(currentLocation, 10.00)
        // Define a mutable list to store the route steps
        val routeSteps = remember { mutableStateOf<List<RouteStep>>(emptyList()) }
        // Function to fetch route asynchronously

        LaunchedEffect(currentLocation) {
            if (permissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }) {
                suspend fun fetchRoute(source: String, destination: String) {
                    val apiKey = "AIzaSyC2GeVN63qnAS6E3KG50tZNIYcRJw5MMH8"
                    val url = "https://maps.googleapis.com/maps/api/directions/json" +
                            "?origin=$source" +
                            "&destination=$destination" +
                            "&key=$apiKey"

                    val route = withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val jsonResponse = StringBuilder()
                            val reader = BufferedReader(InputStreamReader(connection.inputStream))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                jsonResponse.append(line)
                            }
                            reader.close()

                            parseRouteFromJson(jsonResponse.toString())
                        } else {
                            // Handle error cases
                            null
                        }
                    }

                    // Update the mutable state with the fetched route points
                    latLngList.value = route ?: emptyList()
                    Log.d("route", "Google map Route points as list: $latLngList")
                }
                // Function to fetch route asynchronously
                suspend fun fetchSteps(source: String, destination: String) {
                    val apiKey = "AIzaSyC2GeVN63qnAS6E3KG50tZNIYcRJw5MMH8" // Replace with your API key
                    val url = "https://maps.googleapis.com/maps/api/directions/json" +
                            "?origin=$source" +
                            "&destination=$destination" +
                            "&key=$apiKey"

                    val route = withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val jsonResponse = StringBuilder()
                            val reader = BufferedReader(InputStreamReader(connection.inputStream))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                jsonResponse.append(line)
                            }
                            reader.close()

                            parseRouteStepsFromJson(jsonResponse.toString())
                        } else {
                            // Handle error cases
                            null
                        }
                    }

                    // Update the mutable state with the fetched route steps
                    routeSteps.value = route ?: emptyList()

                    // Log the list of steps
                    route?.forEachIndexed { index, step ->
                        Log.d("Step $index", "Distance: ${step.distance}, Maneuver: ${step.maneuver}")
                    }
                }
                //get location
                startLocationUpdates()
                //get route
                fetchRoute(
                    "${currentLocation.latitude},${currentLocation.longitude}",
                    "18.879959,72.932034"
                )
                //get steps
                fetchSteps(
                    "${currentLocation.latitude},${currentLocation.longitude}",
                    "18.879959,72.932034"
                )
            } else {
                launchMultiplePermissions.launch(permissions)
            }
        }

// Inside your LocationScreen composable function
        var orientationAngle by remember { mutableStateOf(0f) } // Initial orientation angle

// Create SensorManager instance
        val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

// Register listeners for accelerometer and magnetometer sensors
        val accelerometerSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
        val magnetometerSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }

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
                        val azimuthDegrees = Math.toDegrees(azimuthRadians).toFloat()
                        orientationAngle = azimuthDegrees // Update the orientation angle
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
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

// Load your desired bitmap image from resources
        val originalBitmap: Bitmap = remember { BitmapFactory.decodeResource(context.resources, R.drawable.navigation) }

// Define the desired width and height for the resized image
        val desiredWidth = 100 // Specify the desired width in pixels
        val desiredHeight = 100 // Specify the desired height in pixels

// Resize the original bitmap to the desired dimensions
        val resizedBitmap: Bitmap = remember { Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true) }

// Rotate the bitmap to match the phone's orientation
        val rotatedBitmap: Bitmap = remember(orientationAngle) { rotateBitmap(resizedBitmap, orientationAngle) }

// Create a BitmapDescriptor from the rotated bitmap
        val bitmapDescriptor: BitmapDescriptor = remember(rotatedBitmap) { BitmapDescriptorFactory.fromBitmap(rotatedBitmap) }
        
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Draw the destination marker
                val destination = LatLng(18.879959, 72.932034)
                // Define a transparent sky blue color
                val transparentSkyBlue = Color(0x3F00BFFF)
                Marker(
                    state = MarkerState(position = destination),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    title = "Destination",
                    snippet = "Your destination is here!!!"
                )

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
                    color = Color.Green,
                    width = 10f
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your location is ${currentLocation.latitude} and ${currentLocation.longitude}",
                    color = Color.Black
                )
                Button(onClick = {
                    val gmmIntentUri = Uri.parse("google.navigation:q=18.879959,72.932034")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }) {
                    Text(text = "Get Directions")
                }

            }
        }
    }

    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun generateCirclePoints(center: LatLng, radiusMeters: Double): List<LatLng> {
        val numPoints = 100
        val points = mutableListOf<LatLng>()
        val radiusAngle = 2 * PI / numPoints

        for (i in 0 until numPoints) {
            val theta = i * radiusAngle
            val x = center.longitude + radiusMeters / 111000 * cos(theta)
            val y = center.latitude + radiusMeters / 111000 * sin(theta)
            points.add(LatLng(y, x))
        }
        return points
    }
    // Function to parse the JSON response and extract route points
    fun parseRouteFromJson(jsonResponse: String): List<LatLng>? {
        val routePoints = mutableListOf<LatLng>()

        val jsonObject = JSONObject(jsonResponse)
        val routes = jsonObject.getJSONArray("routes")

        if (routes.length() > 0) {
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val polyline = steps.getJSONObject(i).getJSONObject("polyline")
                val points = decodePolyline(polyline.getString("points"))
                routePoints.addAll(points)
            }
        }

        return routePoints
    }
    fun parseRouteStepsFromJson(jsonResponse: String): List<RouteStep>? {
        val routeSteps = mutableListOf<RouteStep>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val routes = jsonObject.getJSONArray("routes")

            if (routes.length() > 0) {
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")

                for (i in 0 until steps.length()) {
                    val stepJson = steps.getJSONObject(i)
                    val distance = stepJson.getJSONObject("distance").getString("text")
                    val maneuver = if (stepJson.has("maneuver")) stepJson.getString("maneuver") else ""
                    val polyline = stepJson.getJSONObject("polyline").getString("points")
                    routeSteps.add(RouteStep(distance, maneuver, polyline))
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return routeSteps
    }
    @Composable
    fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

    data class RouteStep(
        val distance: String,
        val maneuver: String,
        val polyline: String
    )

    // Function to calculate angles between route steps
    fun calculateAngles(routeSteps: List<RouteStep>): List<Double> {
        val angles = mutableListOf<Double>()

        for (i in 0 until routeSteps.size - 1) {
            val currentStep = routeSteps[i]
            val nextStep = routeSteps[i + 1]

            // Calculate angle between current and next step
            val angle = calculateAngleBetweenSteps(currentStep, nextStep)
            angles.add(angle)
        }

        return angles
    }

    fun calculateAngleBetweenSteps(currentStep: RouteStep, nextStep: RouteStep): Double {
        // Get maneuver information for current and next steps
        val currentManeuver = currentStep.maneuver
        val nextManeuver = nextStep.maneuver

        // Define angles for each maneuver type (in degrees)
        val leftTurnAngle = 90.0
        val rightTurnAngle = -90.0
        val straightAngle = 0.0 // No turn

        // Calculate angle based on maneuver information
        return when {
            currentManeuver.contains("left", ignoreCase = true) && nextManeuver.contains("right", ignoreCase = true) -> {
                // Left turn followed by right turn
                (leftTurnAngle - rightTurnAngle) / 2 // Angle between left and right turn
            }
            currentManeuver.contains("right", ignoreCase = true) && nextManeuver.contains("left", ignoreCase = true) -> {
                // Right turn followed by left turn
                (rightTurnAngle - leftTurnAngle) / 2 // Angle between right and left turn
            }
            else -> {
                // No specific turn detected, assume straight
                straightAngle
            }
        }
    }
    // Function to decode polyline points
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }
}