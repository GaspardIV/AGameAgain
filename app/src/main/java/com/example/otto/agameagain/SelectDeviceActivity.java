package com.example.otto.agameagain;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SelectDeviceActivity extends AppCompatActivity {

    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE = 3;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 4;

    Map<BluetoothDevice, String> addresses = new HashMap<>();
    ListView listView;
    Button searchButton;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action", action);
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                onSearchFinished();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName(), address = device.getAddress(), rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));

                if (!addresses.containsKey(device)) {
                    String repr;
                    if (name == null || name.equals("")) {
                        repr = (address + " [RSSI " + rssi + "dBm]");
                    } else {
                        repr = (name + " [RSSI " + rssi + "dBm]");
                    }
                    bluetoothDevices.add(repr);
                    addresses.put(device, repr);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };
    ArrayList<String> bluetoothDevices = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    BluetoothAdapter bluetoothAdapter;

    private AdapterView.OnItemClickListener listViewOnItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String deviceDescrition = bluetoothDevices.get(position);
            BluetoothDevice bluetoothDevice = null;
            for (Map.Entry entry : addresses.entrySet()) {
                if (entry.getValue().equals(deviceDescrition)) {
                    bluetoothDevice = (BluetoothDevice) entry.getKey();
                }
            }
            if (bluetoothDevice != null) {
                if (isPaired(bluetoothDevice)) {
                    bluetoothAdapter.cancelDiscovery();
                    startMapActivity(bluetoothDevice);
                } else {
                    Toast.makeText(SelectDeviceActivity.this, "This device is not paired, or unreachable!", Toast.LENGTH_LONG).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        bluetoothDevice.createBond();
                    }
                }
            }
        }
    };

    private void startMapActivity(BluetoothDevice bluetoothDevice) {
        Intent intent = new Intent (this, GameActivity.class);
        intent.putExtra(Constants.BT_DEVICE, bluetoothDevice);
        startActivity(intent);
    }


    private boolean isPaired(BluetoothDevice bluetoothDevice) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(bluetoothDevice.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onSearchFinished() {
        searchButton.setEnabled(true);
        searchButton.setText(R.string.search);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initBtAdapter();
        checkAndAskForNextPermission();
    }


    private void checkAndAskForNextPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE);
            } else if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE);
            } else if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN}, BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE);
            } else if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        checkAndAskForNextPermission();
    }

    private void initBtAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        searchButton = findViewById(R.id.searchButton);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bluetoothDevices);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(listViewOnItemClick);
    }


    public void onSearchButtonClicked(View view) {
        startBtSearch();
    }

    private void startBtSearch() {
        bluetoothDevices.clear();
        addresses.clear();
        arrayAdapter.notifyDataSetChanged();
        searchButton.setEnabled(false);
        searchButton.setText(R.string.searching);
        bluetoothAdapter.startDiscovery();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }


}

