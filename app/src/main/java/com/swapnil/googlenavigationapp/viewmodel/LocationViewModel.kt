package com.swapnil.googlenavigationapp.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.swapnil.googlenavigationapp.data.GeoFence
import com.swapnil.googlenavigationapp.data.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LocationViewModel : ViewModel() {

    val houses = listOf(
        LatLng(12.868182, 77.652101),
        LatLng(12.868156, 77.652015),
        LatLng(12.868022, 77.652164),
    )
    val locations = listOf(
        LatLng(12.868388360936915, 77.65279794706925),
        LatLng(12.869664401167288, 77.65164191494513),
        LatLng(12.869299610851801, 77.65172640432372),
        LatLng(12.86889752204588, 77.6518962362415),
        LatLng(12.868513171849363, 77.65161116123667),
        LatLng(12.868992131234684, 77.65021611334063),
        LatLng(12.868613694265344, 77.64976727184364),
        LatLng(12.868282561448728, 77.64974907556675),
        LatLng(12.86788638411157, 77.64999169259215),

        LatLng(12.866621934186847, 77.65008652542305),
        LatLng(12.866950910801158, 77.64725958699304),
        LatLng(12.866944703687675, 77.64514575025817),
        LatLng(12.866758490552183, 77.64508208047367),
        LatLng(12.866479170589734, 77.64489107112014),
    )
    val geoFence = listOf(
        GeoFence(LatLng(12.86497512997075, 77.6466757648296), 28.0), // Park 1
        GeoFence(LatLng(12.865082339978722, 77.64618156781864), 28.0), // Park 2
    )
    // Calculate the distance between two LatLng points
    fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results)
        return results[0]
    }
    // Function to fetch route asynchronously
    suspend fun fetchRoute(source: String, destination: String):List<LatLng>? {
        val apiKey = "Your_API_key"
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
        Log.d("route", "Google map Route points as list: $route")
        return route
    }
    // Function to fetch steps asynchronously
    suspend fun fetchSteps(source: String, destination: String): List<RouteStep>? {
        val apiKey = "Your_API_key" // Replace with your API key
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

        // Log the list of steps
        route?.forEachIndexed { index, step ->
            Log.d("Step $index", "Distance: ${step.distance}, Maneuver: ${step.maneuver}")
        }
        return route
    }

    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun generateCirclePoints(center: LatLng, radiusMeters: Double): List<LatLng> {
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
                    val maneuver =
                        if (stepJson.has("maneuver")) stepJson.getString("maneuver") else ""
                    val polyline = stepJson.getJSONObject("polyline").getString("points")
                    routeSteps.add(RouteStep(distance, maneuver, polyline))
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return routeSteps
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
