package com.sshattered.dotmatrixcontroller;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "MainActivity";
    private final int REQUEST_ENABLE_BT = 101;
    private Bluetooth bluetooth;
    private Button btnSelectDevice, btnSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectDevice = findViewById(R.id.btnSelectDevice);
        btnSet = findViewById(R.id.btnSet);
        btnSet.setOnClickListener(this);
        btnSelectDevice.setOnClickListener(this);

        bluetooth = new Bluetooth(MainActivity.this);
        requestPermission();

        populateMatrixView();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnSelectDevice.getId()) {
            showBluetoothDevices();
        }else if(view.getId() == btnSet.getId()){
            checkMatrix();
        }
    }

    private void checkMatrix(){
        LinearLayout matrixView = findViewById(R.id.matrixView);
        //Log.d(TAG, matrixView.getChildAt(0).getTag() + "");

        byte[] matrices = new byte[8];
        for(int i = 0; i < matrixView.getChildCount(); i++){
            LinearLayout row = (LinearLayout) matrixView.getChildAt(i);
            //Log.d(TAG, row.getChildCount() + "");
            for(int j = 0; j < 8; j++){
                LinearLayout view = (LinearLayout) row.getChildAt(j);
                ColorDrawable drawable = (ColorDrawable) view.getBackground();
                if(drawable.getColor() == Color.RED){
                    //Let's use bit-manipulation here
                    matrices[i] = (byte)(matrices[i] | (1<<j));
                }
            }
            Log.d(TAG, matrices[i] + " ");
        }

        LinearLayout sel = findViewById(R.id.layoutMatrixSelection);
        int selec = 0;
        for(int i = 0; i < 4; i++){
            LinearLayout view = (LinearLayout) sel.getChildAt(i);
            ColorDrawable drawable = (ColorDrawable) view.getBackground();
            if(drawable.getColor() == Color.RED){
                selec = i;
                break;
            }
        }

        byte[] bot = new byte[9];
        bot[0] = (byte)selec;
        System.arraycopy(matrices, 0, bot, 1, matrices.length);
        bluetooth.write(bot);
    }

    private void showBluetoothDevices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.dialog_round_corner, null));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ListView listView = new ListView(this);
        linearLayout.addView(listView);

        List<BluetoothDevice> devices = bluetooth.getPairedDevices();
        DeviceAdapter adapter = new DeviceAdapter(MainActivity.this, 0, devices);
        listView.setAdapter(adapter);

        builder.setView(linearLayout);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetooth.connect(devices.get(i));
                alertDialog.dismiss();
            }
        });

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_ENABLE_BT
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    REQUEST_ENABLE_BT
            );
        }
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult o) {
                    if (o.getResultCode() == RESULT_OK) {
                        Toast.makeText(MainActivity.this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    }else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Exiting");
                        builder.setMessage("Permission was denied");
                        builder.setPositiveButton("Close", (v, a)->{
                            finish();
                        });
                        builder.show();
                    }
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BT && grantResults[0] == 0) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                if(permissions[0].equals(Manifest.permission.BLUETOOTH_CONNECT))
                    bluetooth.enableBluetooth(activityResultLauncher);
            }else{
                if(permissions[0].equals(Manifest.permission.BLUETOOTH))
                    bluetooth.enableBluetooth(activityResultLauncher);
            }
        }
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private void populateMatrixView() {
        LinearLayout matrixView = findViewById(R.id.matrixView);
        LinearLayout layoutMatrixSelection = findViewById(R.id.layoutMatrixSelection);
        matrixView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = matrixView.getWidth();
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1.0f
                );
                params.setMargins(
                        pxToDp(4),
                        pxToDp(4),
                        pxToDp(4),
                        pxToDp(4));

                LinearLayout[] rows = new LinearLayout[8];
                for (int y = 0; y < 8; y++) {
                    rows[y] = new LinearLayout(MainActivity.this);
                    rows[y].setLayoutParams(new ViewGroup.LayoutParams(
                            width,
                            width / 8
                    ));
                    rows[y].setTag("Row" + y);
                    rows[y].setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout[] views = new LinearLayout[8];
                    for (int x = 0; x < 8; x++) {
                        views[x] = new LinearLayout(MainActivity.this);
                        views[x].setBackground(new ColorDrawable(Color.parseColor("#212121")));
                        views[x].setLayoutParams(params);
                        views[x].setTag("Row" + y + ":Col" + x);
                        views[x].setOnClickListener((view) -> {
                            ColorDrawable drawable = (ColorDrawable) view.getBackground();
                            if (drawable.getColor() == Color.RED)
                                view.setBackground(new ColorDrawable(Color.parseColor("#212121")));
                            else
                                view.setBackground(new ColorDrawable(Color.RED));
                        });
                        rows[y].addView(views[x]);
                    }
                    matrixView.addView(rows[y]);
                }
                matrixView.setRotation(90);
                matrixView.invalidate();

                LinearLayout[] matrixSelections = new LinearLayout[4];
                for (int i = 0; i < 4; i++) {
                    matrixSelections[i] = new LinearLayout(MainActivity.this);
                    matrixSelections[i].setBackground(new ColorDrawable(Color.parseColor("#212121")));
                    matrixSelections[i].setLayoutParams(params);
                    matrixSelections[i].setOnClickListener((view -> {
                        for(int c = 0; c < 4; c++)
                            matrixSelections[c].setBackground(new ColorDrawable(Color.parseColor("#212121")));
                        view.setBackground(new ColorDrawable(Color.RED));
                    }));
                    layoutMatrixSelection.addView(matrixSelections[i]);
                }
                matrixSelections[0].setBackground(new ColorDrawable(Color.RED));
                layoutMatrixSelection.invalidate();
                matrixView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
}