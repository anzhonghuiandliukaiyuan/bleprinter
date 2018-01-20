package com.cat.bleprinter.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cat.bleprinter.R;
import com.cat.bleprinter.constant.FarmConstant;
import com.cat.bleprinter.helper.BlueToothHelper;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static Handler tipsHandler;
    private static final String TAG = ScanActivity.class.getSimpleName();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView tips;
    private ListView listView;
    private String state;           //判断当前蓝牙连接状态
    private boolean mConnected;
    //是都正在扫描的标志位
    private boolean mScanning;
    //定时扫描的线程
    private Handler mHandler;

    private ImageButton img_btn_back;
    private static final int REQUEST_ENABLE_BT = 1;
    // 在十秒后结束扫描
    private static final long SCAN_PERIOD = 10000;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_scan);

        //注册广播
        registerBoradcastReceiver();
        //设置返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_devices);
        }

        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initView();
        initHandler();
        mHandler = new Handler();
        // 检查低功耗蓝牙是否支持
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 初始化 一个 Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查当前设备是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (state != null){
            this.finish();
        }

        //关闭当前页面
        img_btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void registerBoradcastReceiver() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(ScanActivity.this);
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(FarmConstant.SATE_ACTION_NAME);
        //注册广播
        broadcastManager.registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    private void initHandler() {
        progressDialog = new ProgressDialog(ScanActivity.this);

        tipsHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = (String) msg.obj;
                Toast.makeText(ScanActivity.this, message, Toast.LENGTH_LONG).show();

            }
        };
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(FarmConstant.SATE_ACTION_NAME)) {

                Log.e(TAG, "收到连接状态广播，列表界面");

                showProgressBar(false,null);
                String state = intent.getStringExtra("state");

                Toast.makeText(ScanActivity.this, state, Toast.LENGTH_LONG).show();
                finish();
            }

        }

    };

    private void initView() {
        img_btn_back = (ImageButton) findViewById(R.id.btn_backToMainActivity);
        listView = (ListView) findViewById(R.id.listView);
//      tips = (TextView) findViewById(R.id.textView13);
    }

    protected void showToast(String msg) {
        Toast.makeText(ScanActivity.this, msg, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onDestroy() {
        //取消注册
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case android.R.id.home:
                this.finish(); // back button
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 确保蓝牙开启，若未曾开启，弹框提示是否开启
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // 初始化 list view adapter.并开始扫描
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(this);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 如果用户选择不开启蓝牙
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        //不要忘记清除设备列表
        mLeDeviceListAdapter.clear();

    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // 十秒钟后停止扫描
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final BleDevice bleDevice = mLeDeviceListAdapter.getDevice(position);
        if (bleDevice == null) {
            return;
        }

        BluetoothDevice device = bleDevice.device;

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;

        }

        showProgressBar(true,"正在连接蓝牙设备，请稍候...");

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()){
                    showToast("连接超时");
                }
                showProgressBar(false,null);
            }
        },SCAN_PERIOD);

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        new BlueToothHelper(this, device.getAddress(), mBluetoothManager).startConnect();
    }


    // 设备扫描的回调函数
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    final BleDevice bleDevice = new BleDevice();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("device", device.getAddress());
                            bleDevice.device = device;
                            bleDevice.rssi = rssi;
                            mLeDeviceListAdapter.addDevice(bleDevice);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class BleDevice {
        BluetoothDevice device;
        int rssi;
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView rssi;
    }

    private void showProgressBar(boolean enable,String msg) {
        if (enable) {
            progressDialog.setProgress(R.layout.progressbar_large);
            progressDialog.setMessage(msg);
            progressDialog.show();
        } else {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

    }

    // 设备列表的适配器类
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BleDevice> mLeDevices;
        //这是动态加载布局的一种方式，跟set
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BleDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BleDevice device) {
            BluetoothDevice dev = device.device;
            for (int i = 0; i < mLeDevices.size(); i++) {
                final BleDevice bleDevice = mLeDeviceListAdapter.getDevice(i);
                if (dev.getAddress().equalsIgnoreCase(bleDevice.device.getAddress()))
                    return;
            }mLeDevices.add(device);

        }

        public BleDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
//                view = mInflator.inflate(R.layout.list_device_item, null);
                view = LayoutInflater.from(ScanActivity.this).inflate(R.layout.list_device_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.text_device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.text_device_name);
                viewHolder.rssi = (TextView) view.findViewById(R.id.text_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BleDevice BleDevice = mLeDevices.get(i);
            BluetoothDevice device = BleDevice.device;
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.rssi.setText("RSSI: " + BleDevice.rssi + "db");

            return view;
        }
    }
}
