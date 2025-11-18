package com.example.lottomatic.utility;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnection extends DeviceConnection {
    private final BluetoothDevice device;
    private BluetoothSocket socket;

    // UUID for Bluetooth connection; replace with your own UUID if necessary
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothConnection(BluetoothDevice device) {
        this.device = device;
    }

    @SuppressLint("MissingPermission")
    @Override
    public DeviceConnection connect() throws EscPosConnectionException {
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            return this;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EscPosConnectionException("Unable to connect to Bluetooth device.");
        }
    }

    @Override
    public DeviceConnection disconnect() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream = null;
        socket = null;
        return this;
    }
}