package com.example.bluetooth_chat_app.domain.chat


fun String.toBLuetoothChatMessage(isFromLocalUser:Boolean): BluetoothChatMessage {
    val (senderName, message) = split("#")
    return BluetoothChatMessage(message, senderName, isFromLocalUser)
}
fun BluetoothChatMessage.toByteArray(): ByteArray {
    return "$senderName#$message".encodeToByteArray()
}