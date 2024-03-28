package com.example.bluetooth_chat_app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import com.example.bluetooth_chat_app.ui.theme.BluetoothChatAppTheme

@Composable
fun ChatMessage(
    message: BluetoothChatMessage,
    modifier: Modifier = Modifier
) {

}

//@Preview
//@Composable
//fun ChatMessagePreview() {
//    BluetoothChatAppTheme {
//        ChatMessage(
//            message = BluetoothChatMessage("Hello World", "Pixel 6", true)
//        )
//    }
//
//}