package com.example.bluetooth_chat_app.domain.chat

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices : StateFlow<List<BluetoothDevice>>
    val pairedDevices : StateFlow<List<BluetoothDevice>>

    val scanCallback: ScanCallback
    fun startScanning()

    fun connectDevice(device: BluetoothDevice)

    fun sendData(message: BluetoothChatMessage)
    fun stopScanning()
}