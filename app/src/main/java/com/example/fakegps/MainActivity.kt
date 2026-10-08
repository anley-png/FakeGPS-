package com.example.fakegps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fakegps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default: Eiffel Tower
        if (binding.etLat.text.isNullOrBlank()) binding.etLat.setText("48.858370")
        if (binding.etLng.text.isNullOrBlank()) binding.etLng.setText("2.294481")

        binding.btnStart.setOnClickListener { onStartClicked() }

        binding.btnStop.setOnClickListener {
            MockLocationService.stop(this)
            updateUi(false)
        }

        binding.btnOpenDevSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun onStartClicked() {
        val lat = binding.etLat.text.toString().toDoubleOrNull()
        val lng = binding.etLng.text.toString().toDoubleOrNull()

        if (lat == null || lng == null || lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            Toast.makeText(this, R.string.invalid_coords, Toast.LENGTH_SHORT).show()
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION
            )
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }

        MockLocationService.start(this, lat, lng)
        updateUi(true)
    }

    private fun updateUi(running: Boolean) {
        binding.btnStart.isEnabled = !running
        binding.btnStop.isEnabled = running
        binding.tvStatus.setText(if (running) R.string.status_running else R.string.status_stopped)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStartClicked()
            } else {
                Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi(MockLocationService.isRunning)
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val REQUEST_NOTIFICATIONS = 2
    }
}