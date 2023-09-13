package com.exam_screen;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    enum STATUS {IDLE, RUNNING, FAIL};
    public final int ALARM_ID = 1;

    public DevicePolicyManager devicePolicyManager;
    public ComponentName componentName;
    public PowerManager mPowerManager;
    public EditText delayEditText;
    public EditText checkEditText;
    public Button testButton;
    public TextView offCount;
    public TextView onCount;
    public String scriptPath;
    public boolean testing;
    public int screenOnCount = 0;
    public int screenOffCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 앱 실행시, 초기설정 */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

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

        /* wakeup power manager 가져오기 */
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        /* 앱화면 및 이벤트 설정 */
        offCount = findViewById(R.id.off_count);
        onCount = findViewById(R.id.on_count);
        delayEditText = findViewById(R.id.delay);
        checkEditText = findViewById(R.id.check_delay);
        testButton = findViewById(R.id.button);
        setButton(STATUS.IDLE);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testing) {
                    setButton(STATUS.IDLE);
                } else {
                    setButton(STATUS.RUNNING);
                    screenOnCount = 0;
                    screenOffCount = 0;
                    screenTest();
                }
            }
        });
    }

    public void setButton(STATUS status) {
        if (status == STATUS.IDLE) {
            testButton.setBackgroundColor(Color.BLUE);
            testButton.setTextColor(Color.WHITE);
            testButton.setText("START TEST");
            testing = false;
        } else if (status == STATUS.RUNNING) {
            testButton.setBackgroundColor(Color.YELLOW);
            testButton.setTextColor(Color.BLACK);
            testButton.setText("RUNNING TEST");
            testing = true;
        } else if (status == STATUS.FAIL) {
            testButton.setBackgroundColor(Color.RED);
            testButton.setTextColor(Color.WHITE);
            testButton.setText("FAIL TEST");
            testing = false;
        }
    }

    public void screenTest() {
        // 화면 끄기
        devicePolicyManager.lockNow();
        // 화면 켜기
        scheduleAlarm(this, Integer.parseInt(delayEditText.getText().toString()));
    }

    public void scheduleAlarm(Context context, long delayMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, intent, 0);

        long triggerAtMillis = SystemClock.elapsedRealtime() + delayMillis;
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
    }

    public BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOnCount++;
                
                new Handler().postDelayed(new Runnable() {  // 화면 켜지고 제테스트 판단
                    @Override
                    public void run() {
                        if (!testing) {
                            Toast.makeText(context, "테스트를 중지하였습니다.", Toast.LENGTH_SHORT).show();
                        } else if (screenOnCount == screenOffCount) {
                            screenTest();
                        } else {
                            setButton(STATUS.FAIL);
                        }
                    }
                }, Integer.parseInt(checkEditText.getText().toString()));

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                screenOffCount++;
            }
            onCount.setText(String.valueOf(screenOnCount));
            offCount.setText(String.valueOf(screenOffCount));
        }
    };
}