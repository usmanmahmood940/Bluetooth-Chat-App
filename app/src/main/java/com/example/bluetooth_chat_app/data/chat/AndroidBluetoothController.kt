package com.example.bluetooth_chat_app.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.system.Os.socket
import android.util.Log
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import com.example.bluetooth_chat_app.domain.chat.BluetoothController
import com.example.bluetooth_chat_app.domain.chat.BluetoothDataTransferService
import com.example.bluetooth_chat_app.domain.chat.BluetoothDeviceDomain
import com.example.bluetooth_chat_app.domain.chat.ConnectionResult
import com.example.bluetooth_chat_app.domain.chat.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.util.UUID


@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy{
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    override val isPaired: StateFlow<Boolean>
        get() = _isPaired.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()


    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver {device->
        _scannedDevices.update { devices ->
           val newDevice = device.toBluetoothDeviceDomain()
            if (devices.contains(newDevice)){
                devices
            } else {
                devices + newDevice
            }
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true){
            _isConnected.update { isConnected }
            updatePairedDevices()
        }
    }

    private val pairingRequestReceiver = PairingRequestReciever(
        onPairingRequest = { device ->
            Log.d("USMAN-TAG", "Pairing request callback ${device.name}")
        },
        onBondStateChanged = { device, state ->
            Log.d("USMAN-TAG", "Bonding request callback ${device.name} $state")
            _isPaired.update { true }
        }
    )


    private var currentServerSocket:BluetoothServerSocket? = null
    private var currentClientScoket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
//        context.registerReceiver(pairingRequestReceiver, filter)
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )

    }

    override fun startDiscovery() {
       if (!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
           return
       }
        context.registerReceiver(foundDeviceReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        if(!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow{
            if(!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
                throw SecurityException("Missing permission BLUETOOTH_CONNECT")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(SERVICE_UUID)

            )
            var shouldLoop  = true
            while (shouldLoop){
               currentClientScoket = try {
                   currentServerSocket?.accept()

               }catch (e:Exception){
                   shouldLoop = false
                   null
               }
                emit(ConnectionResult.ConnectionEstablished)
                currentClientScoket?.let {
                    currentServerSocket?.close()
                    shouldLoop = false
                    val service = BluetoothDataTransferService(it)
                    dataTransferService = service
                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceeded(it)
                            }
                    )
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    //client perspective
    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if(!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
                throw SecurityException("Missing permission BLUETOOTH_CONNECT")
            }

            currentClientScoket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            stopDiscovery()
//            val bluetoothDevice  = bluetoothAdapter?.getRemoteDevice(device.address)
//            if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false){
//                bluetoothDevice?.createBond()
//            }else {
                currentClientScoket?.let { socket ->
                    try {
                        Log.d("USMAN-TAG", "Connection Request")
                        socket.connect()
                        emit(ConnectionResult.ConnectionEstablished)
                        BluetoothDataTransferService(socket).also { service ->
                            dataTransferService = service
                            emitAll(
                                service
                                    .listenForIncomingMessages()
                                    .map {
                                        ConnectionResult.TransferSucceeded(it)
                                    }
                            )
                        }
                    } catch (e: Exception) {
                        Log.d("USMAN-TAG", "Error connecting to device ${e.message}")
                        socket.close()
                        currentClientScoket = null
                        emit(ConnectionResult.Error("Connection was interrupted"))

                    }
                }
//            }

        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: String): BluetoothChatMessage? {
        if(!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
            return null
        }
        if (dataTransferService == null){
            return null
        }
        val bluetoothChatMessage = BluetoothChatMessage(
            message = message,
            senderName = bluetoothAdapter?.name?: "Unknown name",
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothChatMessage.toByteArray())

        return bluetoothChatMessage
    }

    override fun closeConnection() {
        currentClientScoket?.close()
        currentServerSocket?.close()
        currentClientScoket = null
        currentServerSocket = null
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        context.unregisterReceiver(pairingRequestReceiver)
        closeConnection()
    }

    private fun updatePairedDevices(){
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map {
                it.toBluetoothDeviceDomain()
            }
            ?.also{devices->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}