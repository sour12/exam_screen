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
    public EditText delayEditText;
    public EditText checkEditText;
    public Button testButton;
    public TextView totalGap;
    public TextView triggerGap;
    public boolean testing;
    public static long offTime;
    public static long onTime;
    public static long totalTime;

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

        /* 앱화면 및 이벤트 설정 */
        triggerGap = findViewById(R.id.trigger_gap);
        totalGap = findViewById(R.id.total_gap);
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
        offTime = System.currentTimeMillis();
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
                totalTime = System.currentTimeMillis();

                if (!testing) {
                    Toast.makeText(context, "테스트를 중지하였습니다.", Toast.LENGTH_SHORT).show();
                } else if ((totalTime - offTime) <= Integer.parseInt(checkEditText.getText().toString())) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            screenTest();
                        }
                    }, 2000);
                } else {
                    setButton(STATUS.FAIL);
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // empty
            }
            triggerGap.setText(String.valueOf(onTime - offTime));
            totalGap.setText(String.valueOf(totalTime - offTime));
        }
    };
}