package com.cat.bleprinter.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cat.bleprinter.constant.FarmConstant;
import com.cat.bleprinter.dao.BlueToothDao;
import com.cat.bleprinter.entity.BlueTooth;
import com.cat.bleprinter.exception.BleNoConnectedException;
import com.cat.bleprinter.util.ProtocolUtil;
import com.quintic.libqpp.QppApi;
import com.quintic.libqpp.iQppCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Created by FMY on 2017/6/24 0024.
 */
public class BlueToothHelper  {

    private static final String TAG = BlueToothHelper.class.getSimpleName();
    private static BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothGatt mBluetoothGatt = null;
    private final String deviceAddr;
    private Context mcontext;
    private BluetoothManager mBluetoothManager = null;
    private BlueToothDao blueToothDao = new BlueToothDao();
    /// qpp start
    protected static String uuidQppService = "0000fee9-0000-1000-8000-00805f9b34fb";
    protected static String uuidQppCharWrite = "d44bc439-abfd-45a2-b575-925416129600";
//    protected static String uuidQppService = "0000ffe0-0000-1000-8000-00805f9b34fb";
//    protected static String uuidQppCharWrite = "0000ffe1-0000-1000-8000-00805f9b34fb";


    /**
     * 连接状态
     */
    public static boolean mConnected = false;

    public BlueToothHelper(Context context, String deviceaddr, BluetoothManager bluetoothManager) {
        this.mcontext = context;
        this.mBluetoothManager = bluetoothManager;
        this.deviceAddr = deviceaddr;
    }

    /**
     * 暴露出的一个方法
     */
    public void startConnect() {
        boolean isSuccess = initialize();
        if (!isSuccess) {
            return;
        }

        connect(deviceAddr);


        QppApi.setCallback(new iQppCallback() {
            //接受函数
            @Override
            public void onQppReceiveData(BluetoothGatt mBluetoothGatt, String qppUUIDForNotifyChar, byte[] qppData) {

                int[] data = new int[qppData.length];

                for (int i = 0; i < qppData.length; i++) {
                    data[i] = qppData[i] & 0xFF;
                }

                //进行异或校验
                if(!ProtocolUtil.checkReceiveData(data)){
                    Log.e(TAG, "异或校验失败：" + ProtocolUtil.ints2HexString(data));
                    return;
                }

                Log.e(TAG, "收到蓝牙数据：" + ProtocolUtil.ints2HexString(data));
                //log.info("收到蓝牙数据"+ ProtocolUtil.ints2HexString(data));

                //进行广播
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mcontext);
                Intent intent;

                String blueToothResult = "蓝牙数据："+ProtocolUtil.ints2HexString(data);

                if (data[0] == 0xAA && data[data.length - 1] == 0xCC) {
                    //完整的数据,取命令字
                    switch (data[2]){            //判断命令字类型
                        case 0x00:               //通用应答
                            byte[] result = new byte[2];         //要返回的命令结果
                            result[0] = (byte) data[4];          //要回应的命令的命令字
                            result[1] = (byte) data[5];          //回应的结果 成功|失败

                            //如果是握手的通用应答
                            if(result[0] == FarmConstant.CMD_HANDSHAKE){
                                intent = new Intent(FarmConstant.HAND);
                                intent.putExtra("RESULT",result[1]);
                                broadcastManager.sendBroadcast(intent);
                                break;
                            }

                            //如果是其他的通用应答，使用下面的
                            intent = new Intent(FarmConstant.TY_RESPONSE_ACTION_NAME);
                            intent.putExtra("RESULT",result);
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
                        case 0x02:               //建压完成命令
                            System.out.println("建压完成");
                            intent = new Intent(FarmConstant.JY_RESPONSE_ACTION_NAME);
                            String[] jyData = new String[1];
                            jyData[0] = "建压完成";
                            intent.putExtra("responseData",jyData);
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
                        case 0x03:               //自检结果上报命令
                            intent = new Intent(FarmConstant.ZJ_RESPONSE_ACTION_NAME);
                            //检测设备状态
                            String[] responseData = new String[8];
                            //用于存放正常设备返回的
                            int[] value = new int[3];
                            if (data[4]==FarmConstant.NORMAL){
                                Log.e(TAG,"设备状态正常");
                                responseData[0] = "正常";
                            } else {
                                Log.e(TAG,"设备状态异常");
                                responseData[0] = "异常";
                            }

                            //检测喷嘴状态
                            if (data[5]==FarmConstant.NORMAL){
                                Log.e(TAG,"喷嘴状态正常");
                                responseData[1] = "正常";

                                //如果正常解析值,值0是喷嘴阻值
                                value[0] = ProtocolUtil.shortbyte2int(new byte[]{(byte) data[6], (byte) data[7]});

                            }else if (data[5]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"喷嘴短路");
                                responseData[1] = "短路";
                            }else if (data[5]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"喷嘴断路");
                                responseData[1] = "断路";
                            }else if (data[5]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"喷嘴未知");
                                responseData[1] = "未知";
                            }

                            //检测泵电机供电
                            if (data[8]==FarmConstant.NORMAL){
                                Log.e(TAG,"泵电机状态正常");
                                responseData[2] = "正常";
                            }else if (data[8]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"泵电机短路");
                                responseData[2] = "短路";
                            }else if (data[8]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"泵电机断路");
                                responseData[2] = "断路";
                            }else if (data[8]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"泵电机未知");
                                responseData[2] = "未知";
                            }

                            //加热
                            if (data[9]==FarmConstant.NORMAL){
                                Log.e(TAG,"加热状态正常");
                                responseData[3] = "正常";

                                //如果正常解析值,值0是加热阻值
                                value[1] = ProtocolUtil.shortbyte2int(new byte[]{(byte) data[10], (byte) data[11]});

                            }else if (data[9]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"加热状态短路");
                                responseData[3] = "短路";
                            }else if (data[9]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"加热状态断路");
                                responseData[3] = "断路";
                            }else if (data[9]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"加热状态未知");
                                responseData[3] = "未知";
                            }

                            //压力传感器电源
                            if (data[12]==FarmConstant.NORMAL){
                                Log.e(TAG,"压力传感器状态正常");
                                responseData[4] = "正常";
                            }else if (data[12]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"压力传感器状态短路");
                                responseData[4] = "短路";
                            }else if (data[12]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"压力传感器状态断路");
                                responseData[4] = "断路";
                            }else if (data[12]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"压力传感器状态未知");
                                responseData[4] = "未知";
                            }

                            //检测电磁阀状态
                            if (data[13]==FarmConstant.NORMAL){
                                Log.e(TAG,"电磁阀状态正常");
                                responseData[5] = "正常";

                                value[2] = ProtocolUtil.shortbyte2int(new byte[]{(byte) data[14], (byte) data[15]});

                            }else if (data[13]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"电磁阀状态短路");
                                responseData[5] = "短路";
                            }else if (data[13]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"电磁阀状态断路");
                                responseData[5] = "断路";
                            }else if (data[13]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"电磁阀状态未知");
                                responseData[5] = "未知";
                            }

                            //检测气压传感器电源
                            if (data[16]==FarmConstant.NORMAL){
                                Log.e(TAG,"气压传感器状态正常");
                                responseData[6] = "正常";
                            }else if (data[16]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"气压传感器状态短路");
                                responseData[6] = "短路";
                            }else if (data[16]==FarmConstant.UNCIRCUIT) {
                                Log.e(TAG,"气压传感器状态断路");
                                responseData[6] = "断路";
                            }else if (data[16]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"气压传感器状态未知");
                                responseData[6] = "未知";
                            }

                            //检测CAN总线
                            if (data[17]==FarmConstant.NORMAL){
                                Log.e(TAG,"CAN状态正常");
                                responseData[7] = "正常";
                            }else if (data[17]==FarmConstant.ABNORMAL){
                                Log.e(TAG,"CAN总线状态故障");
                                responseData[7] = "异常";
                            }else if (data[17]==FarmConstant.UNKNOWN) {
                                Log.e(TAG,"CAN总线未知");
                                responseData[7] = "未知";
                            }

                            System.out.println(Arrays.toString(responseData));
                            System.out.println(Arrays.toString(value));
                            //将字节数组传到intent中
                            intent.putExtra("responseData",responseData);
                            intent.putExtra("VALUE",value);
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
                        case 0x04:           //故障通知
                            intent = new Intent(FarmConstant.GZ_RESPONSE_ACTION_NAME);
                            String[] gzData = new String[1];
                            if(data[4] == 0x00){
                                gzData[0] = "未发生过流";
                                intent.putExtra("responseData",gzData);
                            }else if(data[4] == 0x01){
                                gzData[0] = "发生了过流";
                                intent.putExtra("responseData",gzData);
                            }
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
                        case 0x05:           //状态上报
                            //状态上报,无符号两个字节的修改 - 安仲辉
                            int[] reData = new int[1+(qppData.length - 6)/2];
                            reData[0] = data[2];
                            int j = 1;
                            for (int i = 4; i < qppData.length - 2; i+=2) {
                                byte[] u16Byte = {qppData[i],qppData[i+1]};
                                reData[j] = ProtocolUtil.shortbyte2int(u16Byte);
                                j++;
                            }
                            System.out.println("实时状态的参数信息："+ Arrays.toString(reData));
                            intent = new Intent(FarmConstant.ZT_RESPONSE_ACTION_NAME);
                            intent.putExtra("responseData",reData);
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
                        case 0x06:           //返回版本信息
                            intent = new Intent(FarmConstant.VERSION_RESPONSE_ACTION_NAME);
//                            int[] newData = new int[20];
//                            newData[0] = data[4];
//                            newData[1] = data[5];
                            String HWVersion = ProtocolUtil.bytes2HexString(new byte[]{(byte) data[4]});
                            String SWVersion = ProtocolUtil.bytes2HexString(new byte[]{(byte) data[5]});
                            intent.putExtra("HWVersion",HWVersion);
                            intent.putExtra("SWVersion",SWVersion);
                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
                            broadcastManager.sendBroadcast(intent);
                            break;
//                        case 0x0E:              //返回主界面BACK_MAIN
//                            intent = new Intent(FarmConstant.BACK_MAIN);
//                            intent.putExtra("BLUETOOTH_RESULT",blueToothResult);
//                            broadcastManager.sendBroadcast(intent);
//                            break;
                    }
                }
            }
        });
    }

    /**
     * 初始化
     *
     * @return
     */
    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)

        {
            Log.e(TAG, "创建BluetoothAdapter失败.");
            return false;
        }

        return true;
    }

    /**
     * 发送数据的方法
     *
     * @param dat
     */
    public static void sendData(final byte[] dat) throws BleNoConnectedException {
        if (mBluetoothGatt == null) {
            Log.e(TAG,"蓝牙尚未连接*********************************");
            throw new BleNoConnectedException();
        }
        new Thread() {
            @Override
            public void run() {

                if (!QppApi.qppSendData(mBluetoothGatt, dat))//qppDataSend))
                {
                    return;
                }
            }
        }.start();

    }


    /**
     * 回调函数
     *
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange : " + status + "  newState : " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "蓝牙状态变为已连接.");
                mBluetoothGatt.discoverServices();
                mConnected = true;

                //存储
                blueToothDao.deleteAll();
                blueToothDao.save(new BlueTooth("current", deviceAddr));

                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mcontext);
                Intent intent = new Intent(FarmConstant.SATE_ACTION_NAME);
                intent.putExtra("state", "已连接设备：" + deviceAddr);
                broadcastManager.sendBroadcast(intent);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // TODO: 2017/6/24 0024 蓝牙状态改变为未连接的时候 需要干的事情
                Log.e(TAG, "蓝牙状态变为未连接.");

                mConnected = false;
                close();

                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mcontext);
                Intent intent = new Intent(FarmConstant.SATE_ACTION_NAME);
                intent.putExtra("state", "已连接设备：" + deviceAddr);
                broadcastManager.sendBroadcast(intent);

            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (QppApi.qppEnable(mBluetoothGatt, uuidQppService, uuidQppCharWrite)) {
                // TODO: 2017/6/24 0024 支持当前设备进行通讯
            } else {
                List<BluetoothGattService> lBluetoothGattServices;
                lBluetoothGattServices = gatt.getServices();
                if (lBluetoothGattServices == null) {
                    // TODO: 2017/6/24 0024 不支持与当前设备进行通讯
                    Log.e(TAG, "不支持与当前设备进行通讯");
                } else {
//		    		if(lBluetoothGattServices.size()>0)
//		    			setQppData(new String());
                    //setConnectState(lBluetoothGattServices.get(0).getUuid().clockSequence());
                }
            }

        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            QppApi.updateValueForNotification(gatt, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //super.onDescriptorWrite(gatt, descriptor, status);
            Log.w(TAG, "onDescriptorWrite");
            QppApi.setQppNextNotify(gatt, true);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
            /*This is a workaround,20140819,xiesc: it paused with unknown reason on android 4.4.3
             */
                Log.e(TAG, "发送成功!!!!");

            } else {
                Log.e(TAG, "发送失败!!!!");
            }
        }
    };


    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w("Qn Dbg", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        //设置自动连接参数为 false.
        mBluetoothGatt = device.connectGatt(mcontext, true, mGattCallback);

        Log.d(TAG, "尝试创建一个新连接. Gatt: " + mBluetoothGatt);
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("Qn Dbg", "BluetoothAdapter 没有初始化");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
