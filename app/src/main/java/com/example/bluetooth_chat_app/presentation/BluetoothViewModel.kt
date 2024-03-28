package com.example.bluetooth_chat_app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth_chat_app.domain.chat.BluetoothController
import com.example.bluetooth_chat_app.domain.chat.BluetoothDeviceDomain
import com.example.bluetooth_chat_app.domain.chat.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
):ViewModel(){
    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ){
        scannedDevices, pairedDevices, uiState ->
        uiState.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),_state.value)

    private var deviceConnectionJob: Job? = null
    init {
        bluetoothController.isConected.onEach {isConnected->
            _state.update {
                it.copy(isConnected = isConnected)
            }
        }.launchIn(viewModelScope)
        bluetoothController.isPaired.onEach {isPaired->
            _state.update {
                it.copy(isPaired = isPaired, isConnecting = false)
            }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach {errorMessage->
            _state.update {
                it.copy(errorMessage = errorMessage)
            }
        }.launchIn(viewModelScope)

    }

    fun connectToDevice(device: BluetoothDeviceDomain){
        _state.update {
            it.copy(isConnecting = true)
        }
        deviceConnectionJob = bluetoothController.connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice(){
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false
            )
        }
    }

    fun watiForIncomingConnections(){
        _state.update {
            it.copy(isConnecting = true)
        }
        deviceConnectionJob = bluetoothController.startBluetoothServer()
            .listen()
    }

    fun startScan(){
        bluetoothController.startDiscovery()
    }

    fun stopScan(){
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach {result ->
            when(result){
                is ConnectionResult.ConnectionEstablished-> {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                }

                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }.catch {throwable ->
            bluetoothController.closeConnection()
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    errorMessage = throwable.message
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}