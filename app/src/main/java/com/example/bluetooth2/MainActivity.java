package com.example.bluetooth2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BT";

    BluetoothAdapter bluetoothAdapter;
    private LinearLayout linearLayout;
    private ArrayAdapter<String> strArrayAdapterND;
    private TextView textApp, textNewDevices, text_my_mac;
    ArrayList<String> listPD;
    ArrayList<String> listND;
    private final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkAndRequestPermissions();
        registerReceivers();
        checkBlueToothAvailable();

        ListView listViewPairedDevices = findViewById(R.id.listPairedDevices);
        ListView listViewNewDevices = findViewById(R.id.listNewDevices);
        linearLayout = findViewById(R.id.linearLayout);
        textApp = findViewById(R.id.text_app);
        textNewDevices = findViewById(R.id.text_devices);
        Button scan_devices = findViewById(R.id.scanDevices);
        Button makeVisibleOtherDevices = findViewById(R.id.makeVisibleMyDevice);
        text_my_mac = findViewById(R.id.text_my_mac);

        listPD = new ArrayList<>();
        listND = new ArrayList<>();

        ArrayAdapter<String> strArrayAdapterPD = new ArrayAdapter<>(this, R.layout.list_view_items, R.id.row, listPD);
        listViewPairedDevices.setAdapter(strArrayAdapterPD);

        strArrayAdapterND = new ArrayAdapter<>(this, R.layout.list_view_items, R.id.row, listND);
        listViewNewDevices.setAdapter(strArrayAdapterND);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Log.v(TAG, "Bluetooth is OFF");
            linearLayout.setBackgroundColor(getResources().getColor(R.color.red));
            textApp.setBackgroundColor(getResources().getColor(R.color.red));
            textNewDevices.setBackgroundColor(getResources().getColor(R.color.red));

        } else {
            Log.v(TAG, "Bluetooth is ON");
            linearLayout.setBackgroundColor(getResources().getColor(R.color.bluetooth));
            textApp.setBackgroundColor(getResources().getColor(R.color.bluetooth));
            textNewDevices.setBackgroundColor(getResources().getColor(R.color.bluetooth));
        }

        listAllPairedDevices();

        scan_devices.setOnClickListener(v -> scanNewDevices());

        makeVisibleOtherDevices.setOnClickListener(v -> visibleForAnotherDevices());

        text_my_mac.setText("My device MAC: " + bluetoothAdapter.getAddress());
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "BroadcastReceiver ---------> action= " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    assert device != null;
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    Log.v(TAG, "BroadcastReceiver ---------> deviceName= " + deviceName + "; deviceHardwareAddress= " + deviceHardwareAddress);
                    if (deviceName == null) {
                        Log.v(TAG, "BroadcastReceiver ---------> deviceName= null");
                    }
                    listND.add(deviceName + " - " + deviceHardwareAddress);
                    strArrayAdapterND.notifyDataSetChanged();

                    if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equalsIgnoreCase(action)) {
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.ACTION_DISCOVERY_STARTED");
                    }
                    if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equalsIgnoreCase(action)) {
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
                        listND.add(" no new device found");
                    }

                } catch (SecurityException e) {
                    Log.v(TAG, "BroadcastReceiver ---------> SecurityException: " + e.getMessage());
                }
            }
        }
    };

    private final BroadcastReceiver receiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.STATE_OFF= " + BluetoothAdapter.STATE_OFF);
                        linearLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        textApp.setBackgroundColor(getResources().getColor(R.color.red));
                        textNewDevices.setBackgroundColor(getResources().getColor(R.color.red));
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.STATE_TURNING_OFF= " + BluetoothAdapter.STATE_TURNING_OFF);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.STATE_ON= " + BluetoothAdapter.STATE_ON);
                        linearLayout.setBackgroundColor(getResources().getColor(R.color.bluetooth));
                        textApp.setBackgroundColor(getResources().getColor(R.color.bluetooth));
                        textNewDevices.setBackgroundColor(getResources().getColor(R.color.bluetooth));
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.STATE_TURNING_ON= " + BluetoothAdapter.STATE_TURNING_ON);
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver receiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE= " + BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.SCAN_MODE_CONNECTABLE= " + BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.v(TAG, "BroadcastReceiver ---------> BluetoothAdapter.SCAN_MODE_NONE= " + BluetoothAdapter.SCAN_MODE_NONE);
                        break;
                }
            }
        }
    };

    private void checkBlueToothAvailable() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bluetooth is available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");


        unregisterReceiver(receiver);
        unregisterReceiver(receiver2);
        unregisterReceiver(receiver3);


        if (bluetoothAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            Log.v(TAG, "permissions not granted: " + listPermissionsNeeded);
            for (String permission : listPermissionsNeeded) {
            }
        } else {
            Log.v(TAG, "permissions granted");
        }
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver2, filter2);

        IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter3.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter3.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter3.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(receiver3, filter3);
    }


    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void listAllPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                Log.v(TAG, "Device: " + deviceName + "; DeviceHardwareAddress= " + deviceHardwareAddress);
                listPD.add(device.getName() + " - " + device.getAddress());
            }
        } else {
            Log.v(TAG, "pairedDevices.size()=" + pairedDevices.size());
            listPD.add("device not found");
        }
    }

    private void scanNewDevices() {
        Log.d(TAG, "Scanning for 12 seconds ...");
        setTitle(R.string.scanning);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

        }
        bluetoothAdapter.startDiscovery();
            if (bluetoothAdapter.isDiscovering()) {
                Log.v(TAG, "find device");
            } else {
                Log.v(TAG, "not discovering, not find device");
            }
            Log.v(TAG, "1 bluetoothAdapter.getState()= " + bluetoothAdapter.getState() + "; bluetoothAdapter.getScanMode()= " + bluetoothAdapter.getScanMode());
    }

    private void visibleForAnotherDevices () {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);

    }

}
//Marcel Parzyszek 4P
