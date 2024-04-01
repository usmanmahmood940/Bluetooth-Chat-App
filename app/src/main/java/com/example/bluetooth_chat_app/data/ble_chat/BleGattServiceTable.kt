package com.example.bluetooth_chat_app.data.ble_chat

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log

fun BluetoothGatt.printGattTable() {
    if (services.isEmpty()) {
        Log.d("printGattTable", "No service and characteristic available, call discoverServices() first?")
        return
    }
    services.forEach { service ->
        val characteristicsTable = service.characteristics.joinToString(
            separator = "\n|--",
            prefix = "|--"
        ) { it.uuid.toString() }
        Log.d("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        service.characteristics.forEach {
            Log.d("Readable","${it.isReadable()}")
            Log.d("Writable","${it.isWritable()}")
            Log.d("WritableWithoutResponse","${it.isWritableWithoutResponse()}")

        }
    }
}

fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
    return properties and property != 0
}