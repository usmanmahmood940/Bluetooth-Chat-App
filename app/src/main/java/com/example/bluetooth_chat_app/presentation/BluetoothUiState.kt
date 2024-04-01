package com.example.bluetooth_chat_app.presentation

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import com.example.bluetooth_chat_app.domain.chat.BluetoothDeviceDomain

data class BluetoothUiState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val messages: List<BluetoothChatMessage> = emptyList(),

)
