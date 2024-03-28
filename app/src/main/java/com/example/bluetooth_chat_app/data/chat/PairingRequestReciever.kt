package com.example.bluetooth_chat_app.data.chat

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PairingRequestReciever(
    private val onPairingRequest: (BluetoothDevice) -> Unit,
    private val onBondStateChanged: (BluetoothDevice, Int) -> Unit

) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        when(intent?.action){
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
//                device?.let {
//                    onPairingRequest(it)
//                }
                Log.d("USMAN-TAG", "Pairing request")
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                when(state) {
                    BluetoothDevice.BOND_BONDED -> {
                        onBondStateChanged(device!!, state)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Log.d("USMAN-TAG", "Bonding")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.d("USMAN-TAG", "None")
                    }
                }
            }
        }
    }
}
