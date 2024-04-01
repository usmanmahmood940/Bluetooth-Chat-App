package com.example.bluetooth_chat_app.presentation

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth_chat_app.data.ble_chat.BlePeripheralManager
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import com.example.bluetooth_chat_app.domain.chat.BluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleController: BluetoothController,
    private val bleServerManager: BlePeripheralManager
) :ViewModel(){
    private val _state = MutableStateFlow(BluetoothUiState())

    val state = combine(
        bleController.scannedDevices,
        bleController.pairedDevices,
        _state
    ){ scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            messages = state.messages
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),_state.value)

    fun startScanning(){
        bleController.startScanning()
    }

    fun stopScanning(){
        bleController.stopScanning()
    }

    fun connectDevice(device: BluetoothDevice){
        bleController.connectDevice(device)
    }

    fun startServer(){
        bleServerManager.startAdvertising()
    }

    fun sendMessage(){
        bleController.sendData(BluetoothChatMessage("Hello","Unknown",true))

    }

}