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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetooth_chat_app.presentation.BluetoothViewModel
import com.example.bluetooth_chat_app.presentation.components.ChatScreen
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

    private var onResumeState by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



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
                displayLauncher.launch(discoverableIntent)
//
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
                        state.isConnected ->{
                            ChatScreen(
                                state = state,
                                onDisconnect = viewModel::disconnectFromDevice,
                                onSendMessage = viewModel::sendMessage
                            )
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

    override fun onResume() {
        super.onResume()
        onResumeState = !onResumeState
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothChatAppTheme {

    }
}