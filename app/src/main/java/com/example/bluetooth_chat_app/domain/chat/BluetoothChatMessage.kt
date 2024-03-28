package com.example.bluetooth_chat_app.domain.chat

data class BluetoothChatMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
)
