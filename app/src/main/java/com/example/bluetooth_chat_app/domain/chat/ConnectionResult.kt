package com.example.bluetooth_chat_app.domain.chat

sealed interface ConnectionResult {
    object ConnectionEstablished : ConnectionResult

    data class TransferSucceeded(val message: BluetoothChatMessage) : ConnectionResult
    data class Error(val message: String) : ConnectionResult
}