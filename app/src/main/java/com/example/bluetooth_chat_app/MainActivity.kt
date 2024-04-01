package com.example.bluetooth_chat_app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetooth_chat_app.presentation.BleViewModel
import com.example.bluetooth_chat_app.presentation.BluetoothUiState
import com.example.bluetooth_chat_app.presentation.components.DeviceScreen
import com.example.bluetooth_chat_app.ui.theme.BluetoothChatAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val _scannedDevices = MutableStateFlow<MutableList<ScanResult>>(mutableListOf())



    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if(!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)){
                return
            }
            result.device.let {device->
                val indexQuery = _scannedDevices.value.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) { // A scan result already exists with the same address
                    _scannedDevices.value[indexQuery] = result
                    _scannedDevices.value = _scannedDevices.value
                }
                else{
                    _scannedDevices.value.add(result)
                    _scannedDevices.value = _scannedDevices.value
                }
            }
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED-> {
                    Log.d("USMAN_TAG", "Connected")
                }
                ACTION_GATT_DISCONNECTED -> {
                    Log.d("USMAN_TAG", "Disconnected")

                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(ACTION_GATT_CONNECTED)
            addAction(ACTION_GATT_DISCONNECTED)
        }
    }
    private val isBluetoothEnabled :Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private var isScanning = false
        set(value) {
            field = value
        }
    val enableBLueetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ isEnabled ->

    }
    val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(
        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300
    )
    val displayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == 300) {
            // Device is discoverable
        }
    }
    val permissionLauncher  = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
        val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }
        if(canEnableBluetooth){
//            enableBLueetoothLauncher.launch(
//                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            )
            displayLauncher.launch(discoverableIntent)
        }
    }

    // Code to manage Service lifecycle.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        }
        setContent {
            BluetoothChatAppTheme {
                val viewModel = hiltViewModel<BleViewModel>()
                val state by viewModel.state.collectAsState()


//                LaunchedEffect(key1 = state.errorMessage){
//                    state.errorMessage?.let { error ->
//                        Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                LaunchedEffect(key1 = state.isConnected){
//                    if(state.isConnected){
//                        Toast.makeText(applicationContext, "You're Connected", Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                LaunchedEffect(key1 = state.isPaired){
//                    if(state.isPaired){
//                        Toast.makeText(applicationContext, "Device Paired", Toast.LENGTH_LONG).show()
//                    }
//                }
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeviceScreen(
                        state = state,
                        onScanClick = viewModel::startScanning,
                        onStopScanClick = viewModel::stopScanning,
                        onDeviceClick =viewModel::connectDevice,
                        onStartServerClick = viewModel::startServer,
                        onSendMessage= viewModel::sendMessage

                    )
//                    when{
//                        state.isConnecting -> {
//                            Column (
//                                modifier = Modifier.fillMaxSize(),
//                                horizontalAlignment = Alignment.CenterHorizontally,
//                                verticalArrangement = Arrangement.Center
//                            ){
//                                CircularProgressIndicator()
//                                Text(text = "Connecting .....")
//                            }
//                        }
//                        state.isConnected ->{
//                            ChatScreen(
//                                state = state,
//                                onDisconnect = { },
//                                onSendMessage = {  }
//                            )
//                        }
//                        else->{
//                            DeviceScreen(
//                                scannedDevices = scanDevices.value,
//                                state = state,
//                                onScanClick = {startBleScan()},
//                                onStopScanClick = {},
//                                onDeviceClick ={}
//
//                            )
//                        }
//                    }

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        } else {
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        } else {
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(),
                RECEIVER_EXPORTED
            )
        }
        else{
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        }


    }
    companion object{
        private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }

    private fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }



}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothChatAppTheme {

    }
}