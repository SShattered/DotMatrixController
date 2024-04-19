package com.sshattered.dotmatrixcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends Thread{

    private final String TAG = "BLUETOOTH";
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private InputStream is;
    private OutputStream os;
    private final Activity activity;
    private IBluetooth iBluetooth = null;
    private BluetoothListeners bluetoothListeners;

    @SuppressLint("MissingPermission")
    public Bluetooth(Activity activity) {
        this.activity = activity;
        bluetoothManager = activity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice btDevice) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothDevice = btDevice;
                try{
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                    bluetoothSocket.connect();
                    os = bluetoothSocket.getOutputStream();
                    setBTStatus("Connected!");
                }catch (IOException connectException) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "Could not close the client socket", closeException);
                    }
                }catch (Exception exception){
                    Log.d("BLUETOOTH", exception.toString());
                }finally {
                    if(!bluetoothSocket.isConnected())
                        setBTStatus("Couldn't connect");
                }
            }
        }).start();
    }

    public void setListeners(BluetoothListeners bluetoothListeners){
        this.bluetoothListeners = bluetoothListeners;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        while(true){
            try{
                int rec = is.read();
                if(rec != -1) bluetoothListeners.onByteReceived(rec);
            }catch (Exception e){
                Log.d(TAG, e.toString());
                break;
            }
        }
    }

    public void write(int data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    os.write(data);
                }catch (Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        }).start();
    }

    public void write(byte[] data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    os.write(data);
                }catch (Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        }).start();
    }

    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public void enableBluetooth(ActivityResultLauncher<Intent> activityResultLauncher) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activityResultLauncher.launch(enableBtIntent);
    }

    public void setIBluetooth(IBluetooth iBT){
        iBluetooth = iBT;
    }

    private void setBTStatus(String status){
        if(iBluetooth != null)
            iBluetooth.onStatusChanged(status);
    }

    public List<BluetoothDevice> getPairedDevices() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            List<BluetoothDevice> myList = new ArrayList<>();
            if(!pairedDevices.isEmpty())
                myList.addAll(pairedDevices);
            return myList;
        }
        return new ArrayList<BluetoothDevice>();
    }

}
