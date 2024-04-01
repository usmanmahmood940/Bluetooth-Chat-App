package com.example.bluetooth_chat_app.di

import android.content.Context
import com.example.bluetooth_chat_app.data.ble_chat.AndroidBleController
import com.example.bluetooth_chat_app.data.ble_chat.BlePeripheralManager
import com.example.bluetooth_chat_app.domain.chat.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext conext: Context): BluetoothController {
        return AndroidBleController(conext)
    }

    @Provides
    @Singleton
    fun provideBleServerManager(@ApplicationContext conext: Context): BlePeripheralManager {
        return BlePeripheralManager(conext)
    }
}