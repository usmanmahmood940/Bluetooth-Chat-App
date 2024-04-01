package com.example.bluetooth_chat_app.data.ble_chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.bluetooth_chat_app.di.Constants.CHARACTERISTICS_UUID
import com.example.bluetooth_chat_app.di.Constants.SERVICE_UUID
import com.example.bluetooth_chat_app.domain.chat.BluetoothChatMessage
import com.example.bluetooth_chat_app.domain.chat.BluetoothController
import com.example.bluetooth_chat_app.domain.chat.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID


@SuppressLint("MissingPermission")
class AndroidBleController(
    private val context: Context
): BluetoothController {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDevice>>
        get() = _pairedDevices.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    override val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if(!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
                return
            }
            result.device.let {device->
                _scannedDevices.update {
                    if(it.contains(device)){
                        it
                    }
                    else{
                        it + device
                    }
                }

            }
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()

                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.d("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.d("BluetoothGattCallback", "Discovered ${services.size} services for ${device.name}")
//                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.d("BluetoothGattCallback", "Device : ${gatt.device.name}")
            Log.d("BluetoothGattCallback", "onCharacteristicChanged: ${String(value)}")

        }
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothGattCallback", "it came in if}")
                // Characteristic write successful, handle it here
                val receivedMessage = characteristic?.value?.let { String(it) }
                // Process received message
            } else {
                Log.d("BluetoothGattCallback", "it came in else block}")
                // Characteristic write failed, handle it here
            }
        }
    }
    init{
        updatePairedDevices()
    }
    override fun startScanning() {
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) ) {
            return
        } else {
            bleScanner.startScan(null, scanSettings, scanCallback)
            updatePairedDevices()
        }
    }


    override fun stopScanning() {
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        } else {
            bleScanner.stopScan(scanCallback)
        }
    }

    override fun connectDevice(device: BluetoothDevice){
        device.connectGatt(context, false, gattCallback)
    }

    override fun sendData(message:BluetoothChatMessage){
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUID.fromString(SERVICE_UUID))
            service?.let {
                val characteristic = it.getCharacteristic(UUID.fromString(CHARACTERISTICS_UUID))
                Log.d("BluetoothGattCallback", "Characteristic: $characteristic")
                characteristic?.let {char->
                    Log.d("BluetoothGattCallback", "Property Write ${BluetoothGattCharacteristic.PROPERTY_WRITE}")
                    Log.d("BluetoothGattCallback", "Property Write response ${BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                // The characteristic supports write operations

                                val check = gatt.writeCharacteristic(
                                    char,
                                    message.toByteArray(),
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                )
                                Log.d("BluetoothGattCallback", "Check :  ${check}")
                            } else {
                                Log.d("BluetoothGattCallback", "Characteristic doesn't support write operations")
                            }
                        } catch (e: Exception) {
                            Log.d("BluetoothGattCallback", "Failed to send message ${e.message}")
                        }
                    }
                    else{
                        char.setValue("123123");
                        char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        val isSuccessful = gatt.writeCharacteristic(char)
                        if (isSuccessful){
                            Log.d("BluetoothGattCallback", "Message sent successfully")
                        }
                        else{
                            gatt.discoverServices()
                            Log.d("BluetoothGattCallback", "Failed to send message")
                        }
                    }

                }
            }

        }
    }


    private fun updatePairedDevices(){
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.let { devices ->
                _pairedDevices.update { devices.toList()  }
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}