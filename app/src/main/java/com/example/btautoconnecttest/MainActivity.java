package com.example.btautoconnecttest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    /** Nordic Blinky Service UUID. */
    public final static UUID LBS_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    /** BUTTON characteristic UUID. */
    private final static UUID LBS_UUID_BUTTON_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
    /** LED characteristic UUID. */
    private final static UUID LBS_UUID_LED_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123");

    public static final String TAG = "MAINSESSION";
    private static final int REQUEST_PERMISSION_CHECK = 0xAB; // random number
    private static final int REQUEST_ENABLE_BT = 0xCD;

    String[] PermissionsShouldGrant = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothgatt = null;
    BluetoothGattService service_blinky;
    BluetoothGattCharacteristic characteristic_blinky_led;

    TextView tv_preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_preview = findViewById(R.id.tv_preview);


        boolean permissiongrantneed = false;
        for(String s : PermissionsShouldGrant) {
            if(checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                permissiongrantneed = true;
            }
        }
        if(!permissiongrantneed) {
            OnPermissionGranted();
        }
        else {
            requestPermissions(PermissionsShouldGrant, REQUEST_PERMISSION_CHECK);
        }
    }

    void OnPermissionGranted() {
        Log.d(TAG, "PermissionGranted");
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            OnEnvReady();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissiongranted = true;
        for (int grantresult : grantResults) {
            if(grantresult != PackageManager.PERMISSION_GRANTED) {
                permissiongranted = false;
                finish();
            }
        }
        if(permissiongranted) {
            OnPermissionGranted();
        }
        else {
            Toast.makeText(this, "Permission Grant rejected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "BluetoothAdapter.ACTION_REQUEST_ENABLE Failed", Toast.LENGTH_SHORT).show();
                finish();
            }
            else { // Bluetooth Enable Success
                OnEnvReady();
            }
        }
    }

    void OnEnvReady() {
        Log.d(TAG, "Bluetooth Ready");
    }

    public void onBtnClicked(View v) {
        int id = v.getId();
        if(id == R.id.btn_connect) {
            Log.d(TAG, "BLE Scan start");
            BleScanAndSelectDialog dialog = new BleScanAndSelectDialog(this, bluetoothAdapter);
            dialog.setondlgdismissEvent(this::ConnectDevice);
            dialog.dialogon();
        }
        else if(id == R.id.btn_disconnect) {
            DConnectDevice();
        }
        else if(id == R.id.btn_ledtoggle) {
            if(bluetoothgatt != null) {
                led[0] ^= 1;
                CharacteristicWrite(led);
            }
        }
    }
    byte[] led = {0};

    void ConnectDevice(BluetoothDevice device) {
        Toast.makeText(this, "connecting " + device.getName() + ", " + device.getAddress(), Toast.LENGTH_SHORT).show();

        device.createBond(); // it makes peer manager functions works

        bluetoothgatt = device.connectGatt(this, false, GattCallback);
    }

    void DConnectDevice() {
        if(bluetoothgatt!= null) {
            bluetoothgatt.disconnect();
            service_blinky = null;
            characteristic_blinky_led = null;
            bluetoothgatt = null;
        }
    }

    BluetoothGattCallback GattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "STATE_CONNECTED");
                Log.i(TAG, "Discover Services");
                bluetoothgatt.discoverServices();
            }
            else if(newState == BluetoothGatt.STATE_CONNECTING) {
                Log.d(TAG, "STATE_CONNECTING");

            }
            else if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "STATE_DISCONNECTED");

                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());

                DConnectDevice();

            }
            else if(newState == BluetoothGatt.STATE_DISCONNECTING) {
                Log.d(TAG, "STATE_DISCONNECTING");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = bluetoothgatt.getServices();
                Log.d(TAG, "Available UUIDs - " + gattServices.size());
                for (BluetoothGattService gattService : gattServices) {
                    Log.i(TAG, gattService.getUuid().toString());
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    Log.d(TAG, "\tAvailable Characteristics - " + gattCharacteristics.size());
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        Log.i(TAG, "\t"+gattCharacteristic.getUuid().toString());
                    }
                }
                BluetoothGattCharacteristic char_blinky_button;


                service_blinky = bluetoothgatt.getService(LBS_UUID_SERVICE);
                char_blinky_button = service_blinky.getCharacteristic(LBS_UUID_BUTTON_CHAR);
                characteristic_blinky_led = service_blinky.getCharacteristic(LBS_UUID_LED_CHAR);

                if(service_blinky == null || char_blinky_button == null || characteristic_blinky_led == null) {
                    Log.e(TAG, "Discover Failed");
                    DConnectDevice();
                    return;
                }

                bluetoothgatt.setCharacteristicNotification(char_blinky_button, true);
                BluetoothGattDescriptor descriptor = char_blinky_button.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothgatt.writeDescriptor(descriptor);

                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "Device Ready", Toast.LENGTH_SHORT).show());
            }
            else {
                Log.e(TAG, "Service Dicover Failed");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead - " + status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead GATT_SUCCESS");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged - at " + characteristic.getUuid());
            if (characteristic.getUuid().equals(LBS_UUID_BUTTON_CHAR)) {
                byte[] data = characteristic.getValue();
                Log.d(TAG, "received data " + data.length + " bytes - " + data[0]);

                MainActivity.this.runOnUiThread(() -> tv_preview.setText(data[0] == 0? "RELEASED" : "PRESSED"));

            }
        }
    };


    void CharacteristicWrite(byte[] b) {
        if(bluetoothgatt != null) {
            if(service_blinky != null) {
                if(characteristic_blinky_led != null) {
                    characteristic_blinky_led.setValue(b);
                    bluetoothgatt.writeCharacteristic(characteristic_blinky_led);
                }
            }
        }
    }

}