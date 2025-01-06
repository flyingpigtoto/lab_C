package com.example.lab_c.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.lab_c.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var btnBackRunning: Button
    private lateinit var googleMap: GoogleMap

    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        btnBackRunning = findViewById(R.id.btnBackRunning)
        btnBackRunning.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            startActivity(intent)
            finish()
        }

        mapView = findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }

        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val routePoints = intent.getSerializableExtra("ROUTE_POINTS") as? ArrayList<DoubleArray>
        routePoints?.let {
            if (it.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .color(resources.getColor(R.color.purple_500))
                    .width(5f)
                var firstPoint: LatLng? = null

                it.forEach { coord ->
                    val lat = coord[0]
                    val lng = coord[1]
                    val alt = coord[2]
                    val latLng = LatLng(lat, lng)
                    polylineOptions.add(latLng)
                    if (firstPoint == null) {
                        firstPoint = latLng
                    }
                }

                googleMap.addPolyline(polylineOptions)
                firstPoint?.let { fp ->
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fp, 15f))
                }
            }
        }
    }

    // MapView lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }

        mapView.onSaveInstanceState(mapViewBundle)
        super.onSaveInstanceState(outState)
    }
}
