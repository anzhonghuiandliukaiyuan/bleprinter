package com.cat.bleprinter.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.StyleRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.cat.bleprinter.R;
import com.cat.bleprinter.constant.FarmConstant;
import com.cat.bleprinter.exception.BleNoConnectedException;
import com.cat.bleprinter.helper.BlueToothHelper;
import com.cat.bleprinter.util.ProtocolUtil;
import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.CardSnapHelper;
import com.ramotion.cardslider.examples.simple.cards.SliderAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static com.cat.bleprinter.constant.FarmConstant.CMD_HANDSHAKE;

public class MenuActivity extends AppCompatActivity {

    private final int[] pics = {R.drawable.pp11, R.drawable.pp22, R.drawable.pp33, R.drawable.pp44, R.drawable.pp55};
    private final String[] countries = {"博世2.2(24V)", "博世6.5(24V)", "天纳克1.5", "天纳克6.0", "无锡凯龙"};
    private final byte[] type = {(byte) 0xFF, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    private final SliderAdapter sliderAdapter = new SliderAdapter(pics, 5, new OnCardClickListener());

    private CardSliderLayoutManager layoutManger;
    private RecyclerView recyclerView;

    private Button btnSearch;
    private TextView country1TextView;
    private TextView country2TextView;
    private int countryOffset1;
    private int countryOffset2;
    private long countryAnimDuration;
    private int currentPosition;

    //蓝牙管理类
    private BluetoothManager bluetoothManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //注册广播
        registerBoradcastReceiver();
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_menu);

        initRecyclerView();
        initCountryText();
        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    private void registerBoradcastReceiver() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(MenuActivity.this);
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(FarmConstant.HAND);
        //注册广播
        broadcastManager.registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    private void initRecyclerView() {
        btnSearch = (Button) findViewById(R.id.btnSearch);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setAdapter(sliderAdapter);
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onActiveCardChange();
                }
            }
        });

        //蓝牙搜索
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });

        layoutManger = (CardSliderLayoutManager) recyclerView.getLayoutManager();

        new CardSnapHelper().attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initCountryText() {
        countryAnimDuration = getResources().getInteger(R.integer.labels_animation_duration);
        countryOffset1 = getResources().getDimensionPixelSize(R.dimen.left_offset);
        countryOffset2 = getResources().getDimensionPixelSize(R.dimen.card_width);
        country1TextView = (TextView) findViewById(R.id.tv_country_1);
        country2TextView = (TextView) findViewById(R.id.tv_country_2);

        country1TextView.setX(countryOffset1);
        country2TextView.setX(countryOffset2);
        country1TextView.setText(countries[0]);
        country2TextView.setAlpha(0f);

        country1TextView.setTypeface(Typeface.createFromAsset(getAssets(), "open-sans-extrabold.ttf"));
        country2TextView.setTypeface(Typeface.createFromAsset(getAssets(), "open-sans-extrabold.ttf"));
    }

    private void setCountryText(String text, boolean left2right) {
        TextView invisibleText;
        TextView visibleText;
        if (country1TextView.getAlpha() > country2TextView.getAlpha()) {
            visibleText = country1TextView;
            invisibleText = country2TextView;
        } else {
            visibleText = country2TextView;
            invisibleText = country1TextView;
        }

        int vOffset;
        if (left2right) {
            invisibleText.setX(0);
            vOffset = countryOffset2;
        } else {
            invisibleText.setX(countryOffset2);
            vOffset = 0;
        }

        invisibleText.setText(text);

        ObjectAnimator iAlpha = ObjectAnimator.ofFloat(invisibleText, "alpha", 1f);
        ObjectAnimator vAlpha = ObjectAnimator.ofFloat(visibleText, "alpha", 0f);
        ObjectAnimator iX = ObjectAnimator.ofFloat(invisibleText, "x", countryOffset1);
        ObjectAnimator vX = ObjectAnimator.ofFloat(visibleText, "x", vOffset);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(iAlpha, vAlpha, iX, vX);
        animSet.setDuration(countryAnimDuration);
        animSet.start();
    }

    private void onActiveCardChange() {
        int pos = layoutManger.getActiveCardPosition();
        if (pos == RecyclerView.NO_POSITION || pos == currentPosition) {
            return;
        }

        onActiveCardChange(pos);
    }

    private void onActiveCardChange(int pos) {
        int animH[] = new int[]{R.anim.slide_in_right, R.anim.slide_out_left};
        int animV[] = new int[]{R.anim.slide_in_top, R.anim.slide_out_bottom};

        boolean left2right = pos < currentPosition;
        if (left2right) {
            animH[0] = R.anim.slide_in_left;
            animH[1] = R.anim.slide_out_right;

            animV[0] = R.anim.slide_in_bottom;
            animV[1] = R.anim.slide_out_top;
        }

        setCountryText(countries[pos % countries.length], left2right);

        currentPosition = pos;
    }

    private class TextViewFactory implements ViewSwitcher.ViewFactory {

        @StyleRes
        final int styleId;
        final boolean center;

        TextViewFactory(@StyleRes int styleId, boolean center) {
            this.styleId = styleId;
            this.center = center;
        }

        @SuppressWarnings("deprecation")
        @Override
        public View makeView() {
            final TextView textView = new TextView(MenuActivity.this);

            if (center) {
                textView.setGravity(Gravity.CENTER);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                textView.setTextAppearance(MenuActivity.this, styleId);
            } else {
                textView.setTextAppearance(styleId);
            }

            return textView;
        }

    }

    private class ImageViewFactory implements ViewSwitcher.ViewFactory {
        @Override
        public View makeView() {
            final ImageView imageView = new ImageView(MenuActivity.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            final LayoutParams lp = new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(lp);

            return imageView;
        }
    }

    private class OnCardClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {


            final CardSliderLayoutManager lm = (CardSliderLayoutManager) recyclerView.getLayoutManager();

            if (lm.isSmoothScrolling()) {
                return;
            }

            final int activeCardPosition = lm.getActiveCardPosition();
            if (activeCardPosition == RecyclerView.NO_POSITION) {
                return;
            }

            final int clickedPosition = recyclerView.getChildAdapterPosition(view);
            if (clickedPosition == activeCardPosition) {

                if (BlueToothHelper.mConnected) {
                    //调用定时任务方法,启用定时任务前，初始化心跳接收状态
                    FarmConstant.HAND_ANSWER_STATE = 0;
                    startHandTimer(countries[clickedPosition], type[clickedPosition]);

                } else {
                    Toast.makeText(MenuActivity.this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
                }


            } else if (clickedPosition > activeCardPosition) {
                recyclerView.smoothScrollToPosition(clickedPosition);
                onActiveCardChange(clickedPosition);
            }
        }
    }

    //接收广播
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(FarmConstant.HAND)) {
                if (BlueToothHelper.mConnected) {
                    System.out.println("收到握手广播了");
                    byte result = intent.getByteExtra("RESULT", (byte) 0xFF);
                    if (result == 0x00) {
                        Log.i("TGA", "握手成功，允许建立操作");
                        FarmConstant.HAND_ANSWER_STATE = 1;
                    }else if(result == 0x01){
                        Log.i("TGA", "握手失败");
                        FarmConstant.HAND_ANSWER_STATE = 2;
                    }
                }
            }
        }

    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }


    //启动握手定时任务 发送三次
    private void startHandTimer(final String deviceName, final byte type) {
        final Timer timer1 = new Timer();
        TimerTask task = new TimerTask() {
            int count = 1;

            @Override
            public void run() {
                //发送三次, 如果三次内没有反馈，取消；
                if (count == 3 && FarmConstant.HAND_ANSWER_STATE == 0) {
                    //showToast("抱歉，设备长时间没有应答");
                    Message msg = new Message();
                    msg.obj = "抱歉，设备长时间没有应答";
                    handler.sendMessage(msg);
                    timer1.cancel();

                } else if (FarmConstant.HAND_ANSWER_STATE == 2) { //收到握手错误应答
                    //showToast("抱歉，设备应答失败");
                    Message msg = new Message();
                    msg.obj = "抱歉，设备应答失败";
                    handler.sendMessage(msg);
                    timer1.cancel();

                } else if (FarmConstant.HAND_ANSWER_STATE == 1) { //收到握手正确应答
                    timer1.cancel();
                    System.out.println("进入");
                    Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                    intent.putExtra("设备型号", deviceName);
                    intent.putExtra("TYPE", type);
                    startActivity(intent);
                }


                //发送握手命令  - 安仲辉
                byte cmdHandShake = CMD_HANDSHAKE;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String timeStr = sdf.format(Calendar.getInstance().getTime());
                byte[] timeByte = ProtocolUtil.HexString2Buf(timeStr);
                //System.out.println(ProtocolUtil.bytes2HexString(ProtocolUtil.HexString2Buf(timeStr)));

                //拼接数据，时间后追加泵的编号 - 安仲辉
                byte[] paramHandShake = ProtocolUtil.append(ProtocolUtil.HexString2Buf(timeStr), type);
                byte[] dataHandShake = new ProtocolUtil().packData(cmdHandShake, paramHandShake);
                try {
                    BlueToothHelper.sendData(dataHandShake);
                } catch (BleNoConnectedException e) {
                    e.printStackTrace();
                }

                count++;
            }
        };
        timer1.schedule(task, 0, 1000);
    }

    protected void showToast(String msg) {
        Toast.makeText(MenuActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    //创建handler等待接受消息
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            showToast((String) msg.obj);
        }
    };
}
