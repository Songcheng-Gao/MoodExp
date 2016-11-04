package org.graduation.healthylife;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.graduation.R;
import org.graduation.database.SharedPreferenceManager;
import org.graduation.service.FeedbackAlarmReceiver;
import org.graduation.service.GatherAlarmReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity
{
    private static AlarmManager alarmManager;
    private static PendingIntent gatherPendingIntent;
    private static AlarmManager alarmManager2;
    private static PendingIntent feedbackPendingIntent;
    TextView introTv;
    TextView submitTv;
    TextView queryTv;

    public static SharedPreferences shared;
    public static SharedPreferences.Editor editor;

    public static Handler recvHandler=new Handler();
    public static int msgGpsRequest=0;
    public static int msgWlanRequest=1;

    private long exitTime = 0;

    NotificationManager nm;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction()
                .add(R.id.layout_mainpage, new OptionFragment())
                .commit();//创建情绪选择fragment界面
        introTv=(TextView)findViewById(R.id.introTv);
        submitTv=(TextView)findViewById(R.id.submitTv);
        queryTv=(TextView)findViewById(R.id.queryTv);
        MOnClickListener mOnClickListener=new MOnClickListener();
        introTv.setOnClickListener(mOnClickListener);
        submitTv.setOnClickListener(mOnClickListener);
        queryTv.setOnClickListener(mOnClickListener);

        shared=getSharedPreferences("user", Context.MODE_PRIVATE);
        editor=shared.edit();editor.putString("date","-1");

        nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        recvHandler =new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what==msgGpsRequest)
                {
                    Toast.makeText(MainApplication.getContext(), "请激活GPS", Toast.LENGTH_SHORT).show();
                    Intent intent1=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pi= PendingIntent.getActivity(MainActivity.this, 0, intent1,0);

                    Notification notify=new Notification.Builder( getApplicationContext() )
                            .setAutoCancel(true)
                            .setTicker("请打开GPS开关")

                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.icon))
                            .setSmallIcon(R.mipmap.icon)
                            .setContentTitle( "请打开GPS开关"  )
                            .setContentText("GPS数据是实验数据重要的一部分，请打开GPS开关")
                            .setDefaults(Notification.DEFAULT_SOUND )//| Notification.DEFAULT_VIBRATE)
                            .setWhen(System.currentTimeMillis())
                            .setContentIntent(pi)
                            .build();

                    int id= msgGpsRequest ;
                    nm.notify(id,notify);/**/
                }

                else if(msg.what==msgWlanRequest)
                {
                    Toast.makeText(MainApplication.getContext(), "请激活WLAN", Toast.LENGTH_SHORT).show();
                    Intent intent1=new Intent(Settings.ACTION_WIFI_SETTINGS)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pi= PendingIntent.getActivity(MainActivity.this, 0, intent1,0);

                    Notification notify=new Notification.Builder( getApplicationContext() )
                            .setAutoCancel(true)
                            .setTicker("请打开WLAN开关")

                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.icon))
                            .setSmallIcon(R.mipmap.icon)
                            .setContentTitle( "请打开WLAN开关"  )
                            .setContentText("WLAN数据是实验数据重要的一部分，请打开WLAN开关")
                            .setDefaults(Notification.DEFAULT_SOUND )//| Notification.DEFAULT_VIBRATE)
                            .setWhen(System.currentTimeMillis())
                            .setContentIntent(pi)
                            .build();

                    int id= msgWlanRequest ;
                    nm.notify(id,notify);/**/
                }


            }//public void handleMessage(Message msg)

        };

        checkPermission();//开启权限
        prepareServices();//开始服务

        if(!hasOpened())
        {
            handleFirstOpen();//确认第一次开启
        }
    }
    class MOnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.introTv:
                    MainActivity.this.getFragmentManager().beginTransaction().
                            replace(R.id.layout_mainpage, new IntroFragment()).commit();//更换fragment
                    break;
                case R.id.submitTv:
                    MainActivity.this.getFragmentManager().beginTransaction().
                            replace(R.id.layout_mainpage,new OptionFragment()).commit();
                    break;
                case R.id.queryTv:
                {
                    Intent intent=new Intent();
                    intent.setClass(MainActivity.this, ChartActivity.class);
                    MainActivity.this.startActivity(intent);
                }
                    //MainActivity.this.getFragmentManager().beginTransaction().
                            //replace(R.id.layout_mainpage, new QueryFragment()).commit();
                    break;
                default:
                    break;
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
    boolean hasOpened()
    {
        SharedPreferenceManager sm=SharedPreferenceManager.getManager();
        return sm.getBoolean("opened", false);
    }

    void handleFirstOpen()
    {
        SharedPreferenceManager sm=SharedPreferenceManager.getManager();
        sm.put("opened",true);
        sm.put("phoneID",""+(long)(Math.random()*Long.MAX_VALUE));//手机序列号随机 有可能重复
    }

    private void prepareServices() {
        alarmManager= (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        gatherPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, GatherAlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                5 * 60 * 1000,//5 min
                gatherPendingIntent);
        alarmManager.cancel(gatherPendingIntent);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                5 * 60 * 1000,
                gatherPendingIntent);
        alarmManager2 = (AlarmManager)this
                .getSystemService(Context.ALARM_SERVICE);
        feedbackPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, FeedbackAlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar clock = Calendar.getInstance();
        clock.setTimeInMillis(System.currentTimeMillis());
        clock.set(Calendar.HOUR_OF_DAY, 10);
        clock.set(Calendar.MINUTE, 0);
        clock.set(Calendar.SECOND, 0);
        alarmManager2.cancel(feedbackPendingIntent);
        alarmManager2.setRepeating(AlarmManager.RTC_WAKEUP,
                clock.getTimeInMillis(),
                AlarmManager.INTERVAL_HALF_DAY,
                feedbackPendingIntent);
        clock.set(Calendar.HOUR_OF_DAY,16);
        alarmManager2.setRepeating(AlarmManager.RTC_WAKEUP,
                clock.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                feedbackPendingIntent);
        Log.d("Service Preparation", "done.");
    }

    private void checkPermission() {
        // Dynamically request permissions on Android 6.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> requesting = new ArrayList<>();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requesting.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requesting.add(Manifest.permission.RECORD_AUDIO);
            }
            if (requesting.isEmpty()) {
                return;
            }
            ActivityCompat.requestPermissions(this, requesting.toArray(new String[2]), 0);
        }
        // Ask for usage permission on available system
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkUsagePermission()) {
                Toast.makeText(getBaseContext(),
                        "我们的实验需要您打开权限开关,谢谢", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private boolean checkUsagePermission() {
        UsageStatsManager manager = (UsageStatsManager) getSystemService(
                Context.USAGE_STATS_SERVICE);
        Map<String, UsageStats> results = manager.queryAndAggregateUsageStats(
                System.currentTimeMillis() - SystemClock.elapsedRealtime(),
                System.currentTimeMillis());
        return !results.isEmpty();
    }

    private void dumpDatabase() {
        final String SRC_DATABASE = "/data/data/org.graduation.healthylife/"
                + "databases/HealthyLife.db";
        final String DEST_FILE = Environment.getExternalStorageDirectory() + "/HealthyLife.db";
        try {
            InputStream fin = new FileInputStream(new File(SRC_DATABASE));
            OutputStream fout = new FileOutputStream(new File(DEST_FILE));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fin.read(buffer)) > 0) {
                fout.write(buffer, 0, length);
            }
            fout.flush();
            fin.close();
            fout.close();
            Toast.makeText(getBaseContext(), "数据库准备完毕", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "数据库复制失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
        {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
