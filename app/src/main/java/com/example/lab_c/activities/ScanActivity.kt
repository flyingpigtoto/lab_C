package com.example.lab_c.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lab_c.R

class ScanActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var listViewDevices: ListView
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val devicesList = mutableListOf<String>()
    private val devicesMap = mutableMapOf<String, String>() // Address => Name

    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    private val TAG = "ScanActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        btnScan = findViewById(R.id.btnScan)
        listViewDevices = findViewById(R.id.listViewDevices)
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
        listViewDevices.adapter = arrayAdapter

        initializeLaunchers()

        btnScan.setOnClickListener {
            scanDevices()
        }

        listViewDevices.setOnItemClickListener { _, _, position, _ ->
            val selected = devicesList[position]
            val address = selected.substringAfter("\n") // Extract the address part
            // Navigate to TrackingActivity with selected Polar address
            val intent = Intent(this, TrackingActivity::class.java).apply {
                putExtra("POLAR_ADDRESS", address)
            }
            startActivity(intent)
            finish()
        }

        // Register for Bluetooth discovery broadcasts
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)

        // Check Bluetooth + Permissions on start
        checkBluetoothAndPermissions()
    }

    private fun initializeLaunchers() {
        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Bluetooth enabled by user.")
            } else {
                Toast.makeText(this, "Bluetooth is required for scanning.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "User denied enabling Bluetooth.")
                finish()
            }
        }

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allGranted = false
                    Log.e(TAG, "Permission denied: ${it.key}")
                } else {
                    Log.d(TAG, "Permission granted: ${it.key}")
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "All permissions are required for scanning.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun scanDevices() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show()
            promptEnableBluetooth()
            return
        }

        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            requestMissingPermissions()
            return
        }

        devicesList.clear()
        devicesMap.clear()
        arrayAdapter.notifyDataSetChanged()

        try {
            val started = bluetoothAdapter.startDiscovery()
            if (started) {
                Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Bluetooth discovery started.")
            } else {
                Toast.makeText(this, "Failed to start scanning.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to start Bluetooth discovery.")
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to start discovery.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "SecurityException: ${e.message}")
        }
    }

    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestMissingPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun checkBluetoothAndPermissions() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }

        if (!hasRequiredPermissions()) {
            requestMissingPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
            Log.d(TAG, "BroadcastReceiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receiver unregister failed: ${e.message}")
        }

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            try {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                    Log.d(TAG, "Bluetooth discovery canceled.")
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied to cancel discovery.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "SecurityException: ${e.message}")
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (hasRequiredPermissions()) {
                            // Check if the device has a non-null name and isn't already in our map
                            if (it.name != null && !devicesMap.containsKey(it.address)) {
                                devicesList.add("${it.name}\n${it.address}")
                                devicesMap[it.address] = it.name ?: "Unknown Device"
                                arrayAdapter.notifyDataSetChanged()
                            } else {
                                // ELSE: either device name is null or we already added this address
                                Log.d(TAG, "Skipping device: name='${it.name}', address='${it.address}'")
                            }
                        } else {
                            // Permissions not granted
                            Log.e(TAG, "Permissions not granted - can't access device info.")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (devicesList.isEmpty()) {
                        Toast.makeText(this@ScanActivity, "No devices found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ScanActivity, "Discovery Finished", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}
