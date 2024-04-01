package com.example.bluetooth_chat_app.data.ble_chat

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeAdvertiser.*
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import com.example.bluetooth_chat_app.di.Constants.CHARACTERISTICS_UUID
import com.example.bluetooth_chat_app.di.Constants.SERVICE_UUID
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*



@SuppressLint("MissingPermission")
class BlePeripheralManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val _messages: MutableStateFlow<List<BluetoothChatMessage>> = MutableStateFlow(emptyList())
    val messages:StateFlow<List<BluetoothChatMessage>>
        get() = _messages.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val advertiser: BluetoothLeAdvertiser? by lazy {
        bluetoothAdapter.bluetoothLeAdvertiser
    }

    private var gattServer: BluetoothGattServer? = null

    // Define your service UUID
    private val serviceUuid: UUID = UUID.fromString(SERVICE_UUID)
    // Define your characteristic UUID
    private val characteristicUuid: UUID = UUID.fromString(CHARACTERISTICS_UUID)

    fun startAdvertising() {
        if (bluetoothAdapter.isEnabled && advertiser != null) {
            startGattServer()
            startAdvertisingService()
        }
    }


    private fun startGattServer() {

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        // Create the service and characteristic
        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            characteristicUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        // Add the characteristic to the service
        service.addCharacteristic(characteristic)
        // Add the service to the GATT server
        gattServer?.addService(service)
    }

    private fun startAdvertisingService() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()

        advertiser?.startAdvertising(settings, data, advertisingCallback)
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        // Implement callbacks for GATT server events, if needed
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Handle device connection
                Log.d("USMAN-TAG", "Device connected: ${device?.address}")
                Toast.makeText(context, "Device connected: ${device?.address}", Toast.LENGTH_SHORT).show()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle device disconnection
                Log.d("USMAN-TAG", "Device disconnected: ${device?.address}")
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            // Handle read request for a characteristic
            Log.d("USMAN-TAG", "Read request for characteristic: ${characteristic?.uuid}")
            // Respond with the characteristic value
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            // Handle write request for a characteristic
            Log.d("USMAN-TAG", "Write request for characteristic: ${characteristic?.uuid}, Value: ${value?.contentToString()}")
            // Process the received data
            characteristic?.value = value
            // Send a response if needed
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            // Handle write request for a descriptor
            Log.d("USMAN-TAG", "Write request for descriptor: ${descriptor?.uuid}, Value: ${value?.contentToString()}")
            // Send a response if needed
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            // Handle execute write request
            Log.d("USMAN-TAG", "Execute write request: $execute")
        }
    }

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d("USMAN-TAG", "Advertising started successfully")
            // Advertising started successfully
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d("USMAN-TAG", "Advertising failed, handle error ")

            // Advertising failed, handle error
        }
    }

    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertisingCallback)
        gattServer?.close()
    }
}
