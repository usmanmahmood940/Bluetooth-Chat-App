package com.example.bluetooth_chat_app.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConected : StateFlow<Boolean>
    val isPaired : StateFlow<Boolean>
    val scannedDevices : StateFlow<List<BluetoothDevice>>
    val pairedDevices : StateFlow<List<BluetoothDevice>>
    val errors:SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer(): Flow<ConnectionResult>
    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>

    suspend fun trySendMessage(message: String):BluetoothChatMessage?
    fun closeConnection()

    fun release()
}