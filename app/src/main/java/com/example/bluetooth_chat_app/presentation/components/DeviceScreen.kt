package com.example.bluetooth_chat_app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth_chat_app.domain.chat.BluetoothDeviceDomain
import com.example.bluetooth_chat_app.presentation.BluetoothUiState

@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onStartServer: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ){
        BluetoothDeviceList(
            pairedDevices = state.pairedDevices,
            scannedDevices =state.scannedDevices ,
            onDeviceClick = onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            )
        Row (
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onScanClick) {
                Text(text = "Start Scan")
            }
            Button(onClick = onStopScanClick ) {
                Text(text = "Stop Scan")
            }
            Button(onClick = onStartServer ) {
                Text(text = "Start Server ")
            }

        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices:List<BluetoothDeviceDomain>,
    scannedDevices:List<BluetoothDeviceDomain>,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = modifier
    ){
        item{
           Text(
               text = "Paired Devices",
               fontWeight = FontWeight.Bold,
               fontSize = 24.sp,
               modifier = Modifier.padding(16.dp)
               )
        }
        items(pairedDevices){device->
            Text(
                text = device.name?:"Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceClick(device) }
                    .padding(16.dp),
                fontSize = 18.sp
            )
        }

        item{
            Text(
                text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(scannedDevices){device->
            Text(
                text = device.name?:"Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceClick(device) }
                    .padding(16.dp),
                fontSize = 18.sp
            )
        }

    }
}