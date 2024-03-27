package com.example.bluetooth_chat_app.domain.chat

typealias BluetoothDeviceDomain = BluetoothDevice
data class BluetoothDevice (
    val name: String?,
    val address: String
)