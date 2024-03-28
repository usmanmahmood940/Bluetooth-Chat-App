package com.example.bluetooth_chat_app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetooth_chat_app.presentation.BluetoothViewModel
import com.example.bluetooth_chat_app.presentation.components.DeviceScreen
import com.example.bluetooth_chat_app.ui.theme.BluetoothChatAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy{
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled :Boolean
        get() = bluetoothAdapter?.isEnabled == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBLueetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ isEnabled ->

        }
        val permissionLauncher  = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                true
            }
            if(canEnableBluetooth && !isBluetoothEnabled){
                enableBLueetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }


        setContent {
            BluetoothChatAppTheme {
                val viewModel = hiltViewModel<BluetoothViewModel>()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(key1 = state.errorMessage){
                    state.errorMessage?.let { error ->
                        Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(key1 = state.isConnected){
                    if(state.isConnected){
                        Toast.makeText(applicationContext, "You're Connected", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(key1 = state.isPaired){
                    if(state.isPaired){
                        Toast.makeText(applicationContext, "Device Paired", Toast.LENGTH_LONG).show()
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    when{
                        state.isConnecting -> {
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ){
                                CircularProgressIndicator()
                                Text(text = "Connecting .....")
                            }
                        }
                        else->{
                            DeviceScreen(
                                state = state,
                                onScanClick = viewModel::startScan,
                                onStopScanClick = viewModel::stopScan,
                                onDeviceClick =viewModel::connectToDevice,
                                onStartServer = viewModel::watiForIncomingConnections

                            )
                        }
                    }

                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothChatAppTheme {

    }
}