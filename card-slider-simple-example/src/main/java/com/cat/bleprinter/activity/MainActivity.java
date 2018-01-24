package com.cat.bleprinter.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cat.bleprinter.R;
import com.cat.bleprinter.constant.FarmConstant;
import com.cat.bleprinter.dao.BlueToothDao;
import com.cat.bleprinter.entity.BlueTooth;
import com.cat.bleprinter.exception.BleNoConnectedException;
import com.cat.bleprinter.helper.BlueToothHelper;
import com.cat.bleprinter.util.Arith;
import com.cat.bleprinter.util.ProtocolUtil;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.cat.bleprinter.constant.FarmConstant.CMD_HANDSHAKE;
import static com.cat.bleprinter.constant.FarmConstant.CMD_VERSION;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String[] permissions = {
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.BLUETOOTH_PRIVILEGED",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION"

    };

    public static Handler progressHandler;

    private String version1,version2;
    private void setVersion(String version1,String version2){
        this.version1 = version1;
        this.version2 = version2;
    }

    private PopupMenu popupMenu;
    private ProgressDialog progressDialog;
    private BlueToothDao blueToothDao = new BlueToothDao();

    private ImageView img_setting;
    private ImageButton btn_close;
    private QMUIRoundButton btn_main_start, btn_main_wait, btn_main_penshe, btn_main_paikong,
            btn_main_jiare, btn_main_fanxiang, btn_main_huiliu;
    private TextView txt_main_time,mtimer,txt_shebei_xinghao;
    private TextView conn_state_txt,txt_main_guzhang;
    //试试状态
    private TextView txt_main_dianya,txt_main_dianliu,txt_main_YaLi,txt_main_wendu,txt_main_QiYa,
            txt_main_diancifa_resistance,txt_main_heat_resistance,txt_main_nozzle_resistance;
    //自检结果上报
    private TextView txt_main_shebei, txt_main_penzui, txt_main_dianji, txt_main_jiare,
            txt_main_yali, txt_main_dianci, txt_main_qiya, txt_main_CAN;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothManager bluetoothManager = null;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //注册广播
        registerBoradcastReceiver();

        initView();
        initHandler();
        initLinster();
        initButton(false, true);

        //获取启动界面的Intent
        Intent intent = getIntent();
        //获取设备型号信息
        String shebei = intent.getStringExtra("设备型号");
        //获取设备类型编号 - 安仲辉
        FarmConstant.TYPE = intent.getByteExtra("TYPE", (byte) 0xFF);
        Log.i("TGA", "获取的设备型号："+shebei+";设备型号编号："+ProtocolUtil.bytes2HexString(new byte[]{FarmConstant.TYPE}));
        //为文本赋值
        txt_shebei_xinghao.setText(shebei);


        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(permissions, 1);
        }
        if (BlueToothHelper.mConnected) {
            conn_state_txt.setText("已连接");
            conn_state_txt.setTextColor(Color.GREEN);
            conn_state_txt.setTextColor(getResources().getColor(R.color.green));
        }

        if (!"已连接".equals(conn_state_txt.getText().toString())) {
            //连接设备
            BlueTooth device = blueToothDao.getFirst();
            if (device == null) {
                showToast("请先连接蓝牙设备！");
                finish();
            }
        }

        //进入泵，一开始先发送握手信号，成功握手后才可以操作,启动定时任务 - 安仲辉
//        startHandTimer();
    }

    //接收广播
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(action);

            String bluetoothResult = intent.getStringExtra("BLUETOOTH_RESULT");
            //Toast.makeText(MainActivity.this, bluetoothResult, Toast.LENGTH_LONG).show();


            if (action.equals(FarmConstant.SATE_ACTION_NAME)) {
                Log.e(TAG, "进入状态改变广播");
                if (BlueToothHelper.mConnected) {
                    conn_state_txt.setText("已连接");
                    conn_state_txt.setTextColor(getResources().getColor(R.color.green));
                } else {
                    conn_state_txt.setText("未连接");
                    conn_state_txt.setTextColor(getResources().getColor(R.color.black));
                    tryConnection();
                }
            } else if (action.equals(FarmConstant.TY_RESPONSE_ACTION_NAME)){
                byte[] result = intent.getByteArrayExtra("RESULT");
                Log.i("TGA", "收到的通用应答：" + result[0]+"(命令字)；"+result[1]+"(是否成功)");
                //如果返回的命令字是升级文件的命令字和成功，则解锁发下一包
                //升级取消
//                if(result[1] == 0x00 && result[0] == FarmConstant.CMD_UPDATE){
//                    if(!Lock.isRun){
//                        Lock.isRun = true;
//                        synchronized(Lock.lock){
//                            System.out.println("线程唤醒");
//                            Lock.lock.notify();//只能在同步代码块中使用
//                        }
//                    }
//                }

//                if(result[0] == FarmConstant.CMD_HANDSHAKE){
//                    if (result[1] == 0x00){
////                        showToast("捂手成功，允许建立操作");
//                        Log.i("TGA", "握手成功，允许建立操作");
//                        initButton(false, false);
//
//                        //设置设备已经应答
////                        FarmConstant.ANSWER = true;
//
//                        //握手成功后，获取版本号
//                        byte[] param = {};
//                        byte[] data = ProtocolUtil.packData(CMD_VERSION, param);
//
//                        try {
//                            BlueToothHelper.sendData(data);
//                        } catch (BleNoConnectedException e) {
//                            e.printStackTrace();
//                        }
//
//                    }else if(result[1] == 0x01){
//                        showToast("握手失败");
//                        initButton(false, true);
//                        finish();
//                    }
//                }

                //开始建压
                if (result[0] == FarmConstant.CMD_START){
                    if (result[1] == 0x00){
//                        showToast("建压命令发送成功...");
                        Log.i("TGA", "建压命令发送成功");
                    }else if(result[1] == 0x01){
                        showToast("建压命令发送失败");
                    }
                }
                //喷射控制
                if (result[0] == FarmConstant.CMD_PENSHE){
                    if (result[1] == 0x00){
                        if (btn_main_penshe.getText().toString().equals("启动喷射")){
                            btn_main_penshe.setText("停止喷射");
//                            showToast("喷射命令发送成功");
                            Log.i("TGA", "喷射命令发送成功");
                        }else if (btn_main_penshe.getText().toString().equals("停止喷射")){
                            btn_main_penshe.setText("启动喷射");
//                            showToast("停止喷射命令发送成功");
                            Log.i("TGA", "停止喷射命令发送成功");
                        }
                    }else if (result[1] == 0x01){
                        showToast("喷射命令发送失败");
                    }
                }
                //排空命令通用应答
                if (result[0] == FarmConstant.CMD_PAIKONG){
                    if (result[1] == 0x00){
//                        showToast("命令发送成功");
                        Log.i("TGA", "排空命令发送成功");
                        setMtimer();
                    }else{
                        showDialogFirst("命令发送失败");
                    }
                }
                //加热控制
                if (result[0] == FarmConstant.CMD_JIARE){
                    if (result[1] == 0x00) {
                        if (btn_main_jiare.getText().toString().equals("开始加热")) {
                            btn_main_jiare.setText("停止加热");
//                            showToast("加热命令发送成功");
                            Log.i("TGA", "加热命令发送成功");
                        } else if (btn_main_jiare.getText().toString().equals("停止加热")) {
                            btn_main_jiare.setText("开始加热");
//                            showToast("停止加热命令发送成功");
                            Log.i("TGA", "停止加热命令发送成功");
                        }
                    }else if (result[1] == 0x01){
                        showDialogFirst("加热命令发送失败");
                    }
                }
                //反向阀
                if (result[0] == FarmConstant.CMD_FANFIANG){
                    if (result[1] == 0x00) {
                        if (btn_main_fanxiang.getText().toString().equals("启动反向阀")) {
                            btn_main_fanxiang.setText("停止反向阀");
//                            showToast("启动反向阀命令发送成功");
                            Log.i("TGA", "启动反向阀命令发送成功");
                        } else if (btn_main_fanxiang.getText().toString().equals("停止反向阀")) {
                            btn_main_fanxiang.setText("启动反向阀");
                            Log.i("TGA", "停止反向阀命令发送成功");
//                            showToast("停止反向阀命令发送成功");
                        }
                    }else if (result[1] == 0x01){
                        showDialogFirst("命令发送失败");
                    }
                }
                //回流泵
                if (result[0] == FarmConstant.CMD_HUILIU){
                    if (result[1] == 0x00) {
                        if (btn_main_huiliu.getText().toString().equals("启动回流泵")) {
                            btn_main_huiliu.setText("停止回流泵");
//                            showToast("启动回流泵命令发送成功");
                            Log.i("TGA", "启动回流泵命令发送成功");
                        } else if (btn_main_huiliu.getText().toString().equals("停止回流泵")) {
                            btn_main_huiliu.setText("启动回流泵");
//                            showToast("停止回流泵命令发送成功");
                            Log.i("TGA", "停止回流泵命令发送成功");
                        }
                    }else if (result[1] == 0x01){
                        showDialogFirst("命令发送失败");
                    }
                }
                //待机命令通用应答
                if (result[0] == FarmConstant.CMD_WAIT){
                    if (result[1] == 0x00){
//                        Toast.makeText(MainActivity.this,"命令发送成功",Toast.LENGTH_SHORT).show();
                        Log.i("TGA", "待机命令发送成功");
                        btn_main_wait.setText("待机中");
                    }else{
                        showDialogFirst("待机失败");
                    }
                }
                //故障通知
                if (result[0] == 0x04){
                    if (result[1] == 0x00){
                        byte cmdWait = FarmConstant.CMD_WAIT;//  获取待机命令
                        byte[] param = {};
                        byte[] data = ProtocolUtil.packData(cmdWait, param);
                        try {
                            BlueToothHelper.sendData(data);
                        } catch (BleNoConnectedException e) {
                            e.printStackTrace();
                        }
                        btn_main_wait.setText("待机中");
                        btn_main_start.setEnabled(false);
                        initButton(false, true);
                        txt_main_guzhang.setText("未发生过流");
                    }else {
                        showToast("发生了过流");
                    }
                }

                //
            }else if (action.equals(FarmConstant.ZJ_RESPONSE_ACTION_NAME)) {   //自检结果上报
                Log.e(TAG, "进入响应自检结果广播");
                String[] responseData = intent.getStringArrayExtra("responseData");
                int[] value = intent.getIntArrayExtra("VALUE");
                showProgressBar(false);
                handleResponse(responseData);

                //直接再次修改,急
                if(value[0] != 0){
                    txt_main_nozzle_resistance.setText(value[0]+"Ω");
                }else{
                    txt_main_nozzle_resistance.setText("0");
                }
                if(value[1] != 0){
                    txt_main_heat_resistance.setText(value[1]+"Ω");
                }else{
                    txt_main_heat_resistance.setText("0");
                }
                if(value[2] != 0){
                    txt_main_diancifa_resistance.setText(value[2]+"Ω");
                }else{
                    txt_main_diancifa_resistance.setText("0");
                }
            }else if(action.equals(FarmConstant.ZT_RESPONSE_ACTION_NAME)){      //状态上报
                Log.e(TAG,"进入状态上报响应广播");
                int[] reData = intent.getIntArrayExtra("responseData");
                handleResponse(reData);
            }else if(action.equals(FarmConstant.GZ_RESPONSE_ACTION_NAME)){      //故障通知
                String[] responseData = intent.getStringArrayExtra("responseData");
                handleResponse(responseData);
            }else if (action.equals(FarmConstant.VERSION_RESPONSE_ACTION_NAME)){
                String HWVersion = intent.getStringExtra("HWVersion");//硬件版本号
                String SWVersion = intent.getStringExtra("SWVersion");//软件版本号*/
                setVersion(HWVersion,SWVersion);

            }else if (action.equals(FarmConstant.JY_RESPONSE_ACTION_NAME)){     //建压完成结果上报
                String[] responseData = intent.getStringArrayExtra("responseData");
                handleResponse(responseData);
            }

        }

    };





        //处理接收到的数据
        private void handleResponse(String[] responseData) {
            System.out.println(responseData.length);
            if (responseData.length == 8) {
                //表示接受到的是自检结果上报
                //设置文本显示状态
                List list = new ArrayList();
                list.add(txt_main_shebei);
                list.add(txt_main_penzui);
                list.add(txt_main_dianji);
                list.add(txt_main_jiare);
                list.add(txt_main_yali);
                list.add(txt_main_dianci);
                list.add(txt_main_qiya);
                list.add(txt_main_CAN);
                for(int i = 0;i<responseData.length;i++){
                    ((TextView) list.get(i)).setText(responseData[i]);
                    if (responseData[i] == "异常" | responseData[i] == "断路" | responseData[i] == "短路"){
                        ((TextView) list.get(i)).setTextColor(Color.RED);
                    } else if (responseData[i] == "正常"){
                        ((TextView) list.get(i)).setTextColor(Color.rgb(34, 139, 34));
                    }

                }
            } else if (responseData.length == 1) {

                if (responseData[0].equals("建压完成")) {
                    Toast.makeText(this, "建压完成", Toast.LENGTH_SHORT).show();
                    btn_main_start.setText("建压完成");
                    initButton(true, false);

                    //建压完成之后，建压按钮不可操作
                    btn_main_start.setEnabled(false);
                    btn_main_start.setTextColor(Color.GRAY);

                }else if (responseData[0].equals("未发生过流")){
                    byte cmdWait = FarmConstant.CMD_WAIT;//  获取待机命令
                    byte[] param = {};
                    byte[] data = ProtocolUtil.packData(cmdWait, param);
                    try {
                        BlueToothHelper.sendData(data);
                    } catch (BleNoConnectedException e) {
                        e.printStackTrace();
                    }
                    btn_main_wait.setText("待机中");
                    btn_main_start.setEnabled(false);
                    initButton(false, false);
                    txt_main_guzhang.setText("未发生过流");
                }else if (responseData[0].equals("发生了过流")){
                    showToast("发生了过流");
                }
            }
        }

        private void handleResponse(int[] reData){
            if(reData[0] == 0x05 ){
                if(reData[1] < 1000){
                    txt_main_dianya.setText(reData[1]+"mV");
                }else{
                    txt_main_dianya.setText(Arith.round(reData[1]/1000.0, 2)+"V");
                }

                txt_main_dianliu.setText(reData[2]+"mA");
                txt_main_YaLi.setText(reData[3]/100.0+"bar");
                txt_main_wendu.setText(reData[4]+"");
                txt_main_QiYa.setText(reData[5]+"");
            }
        }

        private void registerBoradcastReceiver () {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(MainActivity.this);
            IntentFilter myIntentFilter = new IntentFilter();
            myIntentFilter.addAction(FarmConstant.SATE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.ZJ_RESPONSE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.TY_RESPONSE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.ZT_RESPONSE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.GZ_RESPONSE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.VERSION_RESPONSE_ACTION_NAME);
            myIntentFilter.addAction(FarmConstant.JY_RESPONSE_ACTION_NAME);

            //注册广播
            broadcastManager.registerReceiver(mBroadcastReceiver, myIntentFilter);
        }

        //尝试连接蓝牙设备
        private void tryConnection () {

            if (BlueToothHelper.mConnected == false) {
                //连接设备
                BlueTooth device = blueToothDao.getFirst();
                if (device != null) {
                    Toast.makeText(MainActivity.this, " 蓝牙设备正在自动连接...", Toast.LENGTH_SHORT).show();
                    new BlueToothHelper(MainActivity.this, device.getMac(), bluetoothManager).startConnect();
                }
            }
        }


        private void initHandler () {
            progressHandler = new Handler();
        }

        private void showProgressBar ( boolean enable){
            if (enable) {
                progressDialog.setProgress(R.layout.progressbar_large);
                progressDialog.setMessage("正在发送...");
                progressDialog.show();
            } else {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

        }

        private void initLinster () {
            //初始化他
            progressDialog = new ProgressDialog(MainActivity.this);


            //开始建压
            btn_main_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    byte cmdStart = FarmConstant.CMD_START;//  获取开始建压命令
                    //先判断蓝牙是否已连接
                    if (!BlueToothHelper.mConnected) {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }

                    byte[] param = {};
                    byte[] data = ProtocolUtil.packData(cmdStart, param);

                    try {
                        BlueToothHelper.sendData(data);
                    } catch (BleNoConnectedException e) {
                        e.printStackTrace();
                    }

                }
            });

            btn_main_wait.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    byte cmdWait = FarmConstant.CMD_WAIT;//  获取待机命令
                    //先判断蓝牙是否已连接
                    if (!BlueToothHelper.mConnected) {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }

                    byte[] param = {};
                    byte[] data = ProtocolUtil.packData(cmdWait, param);

                    try {
                        BlueToothHelper.sendData(data);
                        Toast.makeText(MainActivity.this, "命令发送中...", Toast.LENGTH_SHORT).show();

                    } catch (BleNoConnectedException e) {
                        e.printStackTrace();
                    }


                }
            });

            btn_main_penshe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!BlueToothHelper.mConnected) {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    } else {
                        if (btn_main_penshe.getText().toString().equals("启动喷射")) {

                            byte cmdPenshe = FarmConstant.CMD_PENSHE;   //获取喷射控制命令

                            byte[] param = {FarmConstant.SUCCESS};
                            byte[] data =  ProtocolUtil.packData(cmdPenshe, param);

                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "喷射命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }

                        } else if (btn_main_penshe.getText().toString().equals("停止喷射")) {

                            byte cmdPenshe = FarmConstant.CMD_PENSHE;   //获取喷射控制命令

                            byte[] param = {FarmConstant.FAILED};
                            byte[] data =ProtocolUtil.packData(cmdPenshe, param);

                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "喷射命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            });

            //排空控制
            btn_main_paikong.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    if (BlueToothHelper.mConnected) {
                        //获取排空控制命令
                        final byte cmdPaikong = FarmConstant.CMD_PAIKONG;

                            byte[] param = {FarmConstant.SUCCESS};
                            byte[] data = ProtocolUtil.packData(cmdPaikong, param);

                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "排空命令发送中...", Toast.LENGTH_SHORT).show();
//                                BlueToothHelper.test();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                    } else {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }
                }
            });

            btn_main_jiare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (BlueToothHelper.mConnected) {
                        byte cmdJiaRe = FarmConstant.CMD_JIARE;
                        if (btn_main_jiare.getText().toString().equals("开始加热")) {
                            byte[] param = {FarmConstant.SUCCESS};
                            byte[] data =  ProtocolUtil.packData(cmdJiaRe, param);
                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "加热命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        } else if(btn_main_jiare.getText().toString().equals("停止加热")){
                            byte[] param = {FarmConstant.FAILED};
                            byte[] data = ProtocolUtil.packData(cmdJiaRe, param);
                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "停止加热命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }

                }
            });

            btn_main_fanxiang.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (BlueToothHelper.mConnected) {
                        byte cmdFanXiang = FarmConstant.CMD_FANFIANG;
                        if (btn_main_fanxiang.getText().toString().equals("启动反向阀")) {
                            byte[] param = {FarmConstant.SUCCESS};
                            byte[] data = ProtocolUtil.packData(cmdFanXiang, param);
                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "启动控制命令发送中...", Toast.LENGTH_SHORT).show();

                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        } else if(btn_main_fanxiang.getText().toString().equals("停止反向阀")){
                            byte[] param = {FarmConstant.FAILED};
                            byte[] data = ProtocolUtil.packData(cmdFanXiang, param);
                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "停止控制命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }

                }
            });


            btn_main_huiliu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (BlueToothHelper.mConnected) {
                        byte cmdHuiLiu = FarmConstant.CMD_HUILIU;

                        if (btn_main_huiliu.getText().toString().equals("启动回流泵")) {

                            byte[] param = {FarmConstant.SUCCESS};
                            byte[] data = ProtocolUtil.packData(cmdHuiLiu, param);

                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "启动控制命令发送中...", Toast.LENGTH_SHORT).show();

                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }

                        } else if(btn_main_huiliu.getText().toString().equals("停止回流泵")){
                            byte[] param = {FarmConstant.FAILED};
                            byte[] data = ProtocolUtil.packData(cmdHuiLiu, param);
                            try {
                                BlueToothHelper.sendData(data);
                                Toast.makeText(MainActivity.this, "停止控制命令发送中...", Toast.LENGTH_SHORT).show();
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        showDialogFirst("请先连接蓝牙设备");
                        return;
                    }

                }
            });

            //设置下拉菜单按钮监听
            img_setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setPopupMenu(version1,version2);
                }
            });

            //关闭当前页面
            btn_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

        }


        @Override
        protected void onResume () {

            super.onResume();

            // 确保蓝牙开启，若未曾开启，弹框提示是否开启
           /* if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
            if (!"已连接".equals(conn_state_txt.getText().toString())) {
                tryConnection();
                return;
            }*/

        }


        @Override
        protected void onDestroy () {

            //当用户退出测试界面的时候，需要再次建立握手
            FarmConstant.HAND_ANSWER_STATE = 0;

            //先判断蓝牙是否已连接
            if (BlueToothHelper.mConnected) {
                byte cmdStart = FarmConstant.CMD_BACK;//  获取返回主界面的命令字
                byte[] param = {};
                byte[] data = ProtocolUtil.packData(cmdStart, param);

                try {
                    BlueToothHelper.sendData(data);
                } catch (BleNoConnectedException e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(MainActivity.this, "请先连接蓝牙设备！", Toast.LENGTH_LONG).show();
            }

            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

            super.onDestroy();
        }

        private void initButton (boolean x, boolean none){
            if(none){
                btn_main_wait.setEnabled(false);
                btn_main_wait.setTextColor(Color.GRAY);
                btn_main_start.setEnabled(false);
                btn_main_start.setTextColor(Color.GRAY);
                btn_main_penshe.setEnabled(x);
                btn_main_penshe.setTextColor(Color.GRAY);
                btn_main_jiare.setEnabled(x);
                btn_main_jiare.setTextColor(Color.GRAY);
                btn_main_paikong.setEnabled(x);
                btn_main_paikong.setTextColor(Color.GRAY);
                btn_main_huiliu.setEnabled(x);
                btn_main_huiliu.setTextColor(Color.GRAY);
                btn_main_fanxiang.setEnabled(x);
                btn_main_fanxiang.setTextColor(Color.GRAY);

                return;
            }

           if(x == false){
               btn_main_wait.setEnabled(true);
               btn_main_wait.setTextColor(Color.BLACK);
               btn_main_start.setEnabled(true);
               btn_main_start.setTextColor(Color.rgb(21,145,233));
               btn_main_penshe.setEnabled(x);
               btn_main_penshe.setTextColor(Color.GRAY);
               btn_main_jiare.setEnabled(x);
               btn_main_jiare.setTextColor(Color.GRAY);
               btn_main_paikong.setEnabled(x);
               btn_main_paikong.setTextColor(Color.GRAY);
               btn_main_huiliu.setEnabled(x);
               btn_main_huiliu.setTextColor(Color.GRAY);
               btn_main_fanxiang.setEnabled(x);
               btn_main_fanxiang.setTextColor(Color.GRAY);
           }else if(x == true){
               btn_main_penshe.setEnabled(x);
               btn_main_penshe.setTextColor(Color.rgb(21,145,233));
               btn_main_jiare.setEnabled(x);
               btn_main_jiare.setTextColor(Color.rgb(21,145,233));
               btn_main_paikong.setEnabled(x);
               btn_main_paikong.setTextColor(Color.rgb(21,145,233));
               btn_main_huiliu.setEnabled(x);
               btn_main_huiliu.setTextColor(Color.rgb(21,145,233));
               btn_main_fanxiang.setEnabled(x);
               btn_main_fanxiang.setTextColor(Color.rgb(21,145,233));
           }
        }

        //显示带有两个参数的对话框
        protected void showDialogFirst (String msg1,String msg2){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("温馨提示");
            builder.setItems(new String[]{"硬件版本号："+msg1,"软件版本号："+msg2},null);
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        protected void showDialogFirst (String msg1){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(msg1);
            builder.setTitle("温馨提示");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }

        protected void showToast (String msg){
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        }


        private void initView () {

            //获取显示设备型号的文本
            txt_shebei_xinghao = (TextView) findViewById(R.id.txt_shebei_xinghao);

            //获取按钮
            btn_main_start = (QMUIRoundButton) findViewById(R.id.btn_main_start);
            btn_main_wait = (QMUIRoundButton) findViewById(R.id.btn_main_wait);
            btn_main_penshe = (QMUIRoundButton) findViewById(R.id.btn_main_penshe);
            btn_main_paikong = (QMUIRoundButton) findViewById(R.id.btn_main_paikong);
            btn_main_jiare = (QMUIRoundButton) findViewById(R.id.btn_main_jiare);
            btn_main_fanxiang = (QMUIRoundButton) findViewById(R.id.btn_main_fanxiang);
            btn_main_huiliu = (QMUIRoundButton) findViewById(R.id.btn_main_huiliu);
            conn_state_txt = (TextView) findViewById(R.id.conn_state_txt);


            //获取表示自检结果上报状态的文本
            txt_main_shebei = (TextView) findViewById(R.id.txt_main_shebei);
            txt_main_penzui = (TextView) findViewById(R.id.txt_main_penzui);
            txt_main_dianji = (TextView) findViewById(R.id.txt_main_dianji);
            txt_main_jiare = (TextView) findViewById(R.id.txt_main_jiare);
            txt_main_yali = (TextView) findViewById(R.id.txt_main_yali);
            txt_main_dianci = (TextView) findViewById(R.id.txt_main_dianci);
            txt_main_qiya = (TextView) findViewById(R.id.txt_main_qiya);
            txt_main_CAN = (TextView) findViewById(R.id.txt_main_CAN);

            //获取表示状态上报的值的文本
            txt_main_dianya = (TextView) findViewById(R.id.txt_main_dianya);
            txt_main_dianliu = (TextView) findViewById(R.id.txt_main_dianliu);
            txt_main_YaLi = (TextView) findViewById(R.id.txt_main_YaLi);
            txt_main_wendu = (TextView) findViewById(R.id.txt_main_wendu);
            txt_main_QiYa = (TextView) findViewById(R.id.txt_main_QiYa);
            txt_main_nozzle_resistance = (TextView) findViewById(R.id.txt_main_nozzle_resistance);
            txt_main_heat_resistance = (TextView) findViewById(R.id.txt_main_heat_resistance);
            txt_main_diancifa_resistance = (TextView) findViewById(R.id.txt_main_diancifa_resistance);

            //获取表示故障通知的文本
            txt_main_guzhang = (TextView) findViewById(R.id.txt_main_guzhang);

            //返回按钮
            btn_close = (ImageButton) findViewById(R.id.btn_closeActivity);
            //设置按钮
            img_setting = (ImageView) findViewById(R.id.img_setting);

            //倒计时文本
            txt_main_time = (TextView) findViewById(R.id.txt_main_time);
            mtimer = (TextView) findViewById(R.id.timer);

            txt_main_time.setVisibility(View.INVISIBLE);
            mtimer.setVisibility(View.INVISIBLE);
        }


        @SuppressLint("HandlerLeak")
        private Handler mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                int recLen = msg.arg1;
                switch(msg.what){
                    case 1:
                        if(recLen >= 0){
                            txt_main_time.setVisibility(View.VISIBLE);
                            mtimer.setVisibility(View.VISIBLE);
                            String str = String.valueOf(recLen);
                            mtimer.setText(str);
                        }else if(recLen < 0){
                            txt_main_time.setVisibility(View.INVISIBLE);
                            mtimer.setVisibility(View.INVISIBLE);
                            Log.e(TAG,"reclen"+recLen);
                            byte cmdPaikong = FarmConstant.CMD_JIARE;
                            byte[] param = {FarmConstant.FAILED};
                            byte[] data = ProtocolUtil.packData(cmdPaikong, param);
                            try {
                                BlueToothHelper.sendData(data);
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }

                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };

        //设置计时器
       private void setMtimer() {
            final Timer timer1 = new Timer();
            TimerTask task = new TimerTask() {
               int recLen = 30;
               @Override
               public void run() {
                   if(recLen < 0){
                       timer1.cancel();
                   }
                   Message message = new Message();
                   message.arg1 = recLen;
                   message.what = 1;
                   mhandler.sendMessage(message);
                   recLen--;
               }
           };
           timer1.schedule(task,0,1000);
       }

    //启动握手定时任务 发送三次
    private void startHandTimer() {
        final Timer timer1 = new Timer();
        TimerTask task = new TimerTask() {
            int count = 1;
            @Override
            public void run() {
                if(count == 3 && !FarmConstant.ANSWER){
                    timer1.cancel();
                    finish();
                }
                if(FarmConstant.ANSWER){
                    timer1.cancel();
                }

                //发送握手命令  - 安仲辉
                byte cmdHandShake = CMD_HANDSHAKE;
                SimpleDateFormat sdf =   new SimpleDateFormat("yyyyMMddHHmmss");
                String timeStr = sdf.format(Calendar.getInstance().getTime());
                byte[] timeByte = ProtocolUtil.HexString2Buf(timeStr);
                //System.out.println(ProtocolUtil.bytes2HexString(ProtocolUtil.HexString2Buf(timeStr)));

                //拼接数据，时间后追加泵的编号 - 安仲辉
                byte[] paramHandShake = ProtocolUtil.append(ProtocolUtil.HexString2Buf(timeStr), (byte)FarmConstant.TYPE);
                byte[] dataHandShake = new ProtocolUtil().packData(cmdHandShake, paramHandShake);
                try {
                    BlueToothHelper.sendData(dataHandShake);
                } catch (BleNoConnectedException e) {
                    e.printStackTrace();
                }

                count++;
            }
        };
        timer1.schedule(task,0,1000);
    }


       //设置下拉菜单
        private void setPopupMenu(final String version1, final String version2){
            popupMenu = new PopupMenu(this,findViewById(R.id.img_setting));
            MenuInflater inflater = popupMenu.getMenuInflater();
            //添加单击事件
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()){
                        //升级取消
                        /*case R.id.menu_update:  // 设备升级

                            //先判断蓝牙是否已连接
                            if (!BlueToothHelper.mConnected) {
                                showDialogFirst("请先连接蓝牙设备");
                                return false;
                            }

                            *//**
                             *  显示进度条弹窗动画
                             *//*
                            //创建dialog对象
                            final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                            //设置样式
                            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            //显示dialog
                            dialog.show();
                            //启动分线程，加载数据，并显示进度，加载完成，移除dialog
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        byte cmdUpdate = FarmConstant.CMD_UPDATE;     //获取升级命令
                                        byte[] fileData = readFile();
                                        int length = DataDivider.packCount(fileData);
                                        dialog.setMax(length);
                                        dialog.setTitle("版本升级");
                                        byte len = (byte)length;
                                        //对升级文件进行分包，每128字节为一包
                                        byte[][] byte1 = new byte[128][length];
                                        int i =0;
                                        List<byte[]> file = DataDivider.divide(fileData);
                                        Iterator<byte[]> iterator = file.iterator();
                                        while (iterator.hasNext()) {
                                            byte1[i] = iterator.next();
                                            i++;
                                        }
                                        byte ii;

                                        //同步代码块
                                        synchronized(Lock.lock){
                                            for(ii=0x01; ii<=len; ii++) {

                                                //发送完一包后锁住线程
                                                if(!Lock.isRun){
                                                    try {
                                                        System.out.println("线程休眠");
                                                        Lock.lock.wait();
                                                    } catch (InterruptedException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }

                                                byte[] byte2 = {0x00, ii, 0x00,len};
                                                //合并数组
                                                byte[] bytes = new byte[byte1[ii].length + byte2.length];
                                                System.arraycopy(byte2,0,bytes,0,byte2.length);
                                                System.arraycopy(byte1,0,bytes,byte2.length,byte1.length);
                                                //打包要发送的命令
                                                byte[] data = new ProtocolUtil().packData(cmdUpdate, bytes);
                                                BlueToothHelper.sendData(data);

                                                //发送完一包后设置锁定
                                                Lock.isRun = false;

                                                dialog.setProgress(dialog.getProgress()+1);
                                            }
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    //移除dialog
                                    dialog.dismiss();
                                }
                            }).start();

                            break;*/
                        case R.id.menu_version: //获取版本号
                            //先判断蓝牙是否已连接
                            if (!BlueToothHelper.mConnected) {
                                showDialogFirst("请先连接蓝牙设备");
                                return false;
                            }
                            byte cmdVersion = CMD_VERSION;   //获取版本号命令

                            byte[] param = {};
                            byte[] data = ProtocolUtil.packData(cmdVersion, param);
                            try {
                                BlueToothHelper.sendData(data);
                            } catch (BleNoConnectedException e) {
                                e.printStackTrace();
                            }
                            showDialogFirst(version1,version2);
                            break;
                    }
                    return false;
                }
            });

            inflater.inflate(R.menu.popupmenu,popupMenu.getMenu());

            popupMenu.show();
        }


        //将要升级的文件读取成byte[]类型并返回
        private byte[] readFile() throws Exception {

            int tempchar;
            File file = new File(
                    "G:\\AAAAAA\\bleprinter\\card-slider-simple-example\\src\\main\\java\\com\\cat\\bleprinter\\updateFile",
                    "test.bin");
            FileInputStream fis = new FileInputStream(file);
            int length;    //文件长度
            int i = 0;
            // 一次读一个字节，先判断文件的长度
            while((tempchar = fis.read()) != -1){
                i++;

            }
            length = i;
            byte[] data = new byte[length];

            //对文件进行打包
            while((tempchar = fis.read()) != -1){
                i=0;
                data[i++] = (byte)tempchar;
            }
            fis.close();
            return data;
        }


}