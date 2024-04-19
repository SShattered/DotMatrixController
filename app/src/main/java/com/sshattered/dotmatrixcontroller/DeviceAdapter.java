package com.sshattered.dotmatrixcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    public DeviceAdapter(@NonNull Context context, int resource, @NonNull List<BluetoothDevice> objects) {
        super(context, resource, objects);
    }

    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = View.inflate(getContext(), R.layout.device_dialog_layout, null);
        TextView deviceName = convertView.findViewById(R.id.txtDeviceName);
        deviceName.setText(getItem(position).getName());

        return convertView;
    }
}
