package com.example.btautoconnecttest;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class BleScanAndSelectDialog {

    private Dialog dlg;
    private ArrayAdapter<String> listadapter;
    private List<String> p2pdevlist;
    List<BluetoothDevice> devices;

    interface dlgdismissEvent {
        void onDialogDismissed(BluetoothDevice device);
    }
    private dlgdismissEvent listener = null;
    void setondlgdismissEvent(dlgdismissEvent listener) {
        this.listener = listener;
    }

    private int selectedindex = -1;

    public BleScanAndSelectDialog(Context context, BluetoothAdapter bluetoothAdapter) {
        dlg = new Dialog(context);
        dlg.setTitle("BLE Scanning");
        dlg.setContentView(R.layout.ble_scan_select_dialog);
        dlg.setCancelable(true);
        dlg.setOnDismissListener(dialog -> {
            Log.d(MainActivity.TAG, "ble scan stop");
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            if(selectedindex != -1)
                if(listener != null) listener.onDialogDismissed(devices.get(selectedindex));
        });

        p2pdevlist = new ArrayList<>();
        devices = new ArrayList<>();
        listadapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, p2pdevlist);

        ListView listview_p2p_device_list = dlg.findViewById(R.id.listview_blescandialog);
        listview_p2p_device_list.setAdapter(listadapter);

        listview_p2p_device_list.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedindex = i;
            dlg.dismiss();
        });
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(device.getName() != null) {
                if(!device.getName().equals("null")) {
//                    Log.d(MainActivity.TAG, "ble scan result : " + device.getName() + "," + device.getAddress());
                    additem(device);
                }
            }
        }
    };

    void additem(BluetoothDevice device) {
        if(!p2pdevlist.contains(device.getName())) {
            listadapter.add(device.getName());
            devices.add(device);
        }
        listadapter.notifyDataSetChanged();
    }

    void dialogon() {
        dlg.show();
    }

}
