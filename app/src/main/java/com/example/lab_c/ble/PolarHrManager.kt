@file:Suppress("MissingPermission")
package com.example.lab_c.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

private const val TAG = "PolarHrManager"
private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
private val HR_MEAS_UUID    = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
private val CCC_DESCRIPTOR  = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * `onHrValue` => callback for BPM updates
 * `onConnectionChanged` => callback for connected/disconnected
 */
class PolarHrManager(
    private val context: Context,
    private val onHrValue: (Int) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit
) {
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GATT Connected -> discoverServices()")
                onConnectionChanged(true)
                if (hasBtConnectPermission()) {
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Disconnected. status=$status, newState=$newState")
                onConnectionChanged(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val hrChar = gatt.getService(HR_SERVICE_UUID)?.getCharacteristic(HR_MEAS_UUID)
                if (hrChar != null) {
                    enableHrNotifications(gatt, hrChar)
                } else {
                    Log.e(TAG, "No Heart Rate char found in services!")
                }
            } else {
                Log.e(TAG, "Service discovery failed, status=$status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HR_MEAS_UUID) {
                parseHrValue(characteristic.value)
            }
        }
    }

    fun connect(addr: String) {
        if (!hasBtConnectPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT => cannot connect.")
            return
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return
        val device: BluetoothDevice = adapter.getRemoteDevice(addr)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        onConnectionChanged(false)
    }

    private fun enableHrNotifications(gatt: BluetoothGatt, hrChar: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(hrChar, true)
        val desc = hrChar.getDescriptor(CCC_DESCRIPTOR)
        desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(desc)
    }

    private fun parseHrValue(data: ByteArray) {
        if (data.isEmpty()) return
        val flags = data[0].toInt()
        val is16Bit = (flags and 0x01) != 0

        val bpm = if (is16Bit) {
            if (data.size >= 3) {
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            } else 0
        } else {
            if (data.size >= 2) {
                data[1].toInt() and 0xFF
            } else 0
        }
        onHrValue(bpm)
    }

    private fun hasBtConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
