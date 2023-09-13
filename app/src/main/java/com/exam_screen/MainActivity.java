package com.exam_screen;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;
    private PowerManager mPowerManager;

    private EditText delayEditText;
    private Button testButton;
    private TextView offCount;
    private TextView onCount;
    private int screenOnCount = 0;
    private int screenOffCount = 0;
    private boolean testing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 앱 실행시, 초기설정 */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* 화면 Off&On 권한 가져오기 */
        devicePolicyManager = (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(getApplicationContext(), MyDeviceAdminReceiver.class);
        if(!devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
        }

        /* On/Off 리시버 등록 */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, intentFilter);

        /* Testing Status */
        testing = false;

        /* wakeup power manager 가져오기 */
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        /* 앱화면 및 이벤트 설정 */
        offCount = findViewById(R.id.off_count);
        onCount = findViewById(R.id.on_count);
        delayEditText = findViewById(R.id.delay);
        testButton = findViewById(R.id.button);
        testButton.setText("START TEST");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testing) {
                    testing = false;
                } else {
                    testButton.setText("RUNNING TEST");
                    testing = true;
                    screenOnCount = 0;
                    screenOffCount = 0;
                    screenTest();
                }
            }
        });
    }

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOnCount++;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                screenOffCount++;
            }
            onCount.setText(String.valueOf(screenOnCount));
            offCount.setText(String.valueOf(screenOffCount));
        }
    };

    private void screenTest() {
        devicePolicyManager.lockNow();  // 화면 끄기
        wakeUpScreen();                 // 화면 켜기

        // 화면 켜지고 Broadcast 받는시점이 살작 뒤? (1500ms)에 있음...
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!testing) {
                    testButton.setText("START TEST");
                    Toast.makeText(MainActivity.this, "Test를 중지하였습니다.", Toast.LENGTH_SHORT).show();
                } else if (screenOnCount == screenOffCount) {
                    screenTest();
                } else {
                    testButton.setText("FAIL TEST");
                    Toast.makeText(MainActivity.this, "On/Off Count가 다릅니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }, Integer.parseInt(delayEditText.getText().toString()) + 1500);
    }

    private void wakeUpScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PowerManager.WakeLock wakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp:WakeLock");
                wakeLock.acquire();
                wakeLock.release();
            }
        }, Integer.parseInt(delayEditText.getText().toString()));  // eg) EditText에서 5000ms 가져옴
    }
}