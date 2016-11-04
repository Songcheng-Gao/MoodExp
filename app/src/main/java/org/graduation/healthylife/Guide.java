package org.graduation.healthylife;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import org.graduation.R;
import org.graduation.appintro.AppIntro2;
import org.graduation.slide.FirstSlide;
import org.graduation.slide.FourthSlide;
import org.graduation.slide.SecondSlide;
import org.graduation.slide.ThirdSlide;

public class Guide extends AppIntro2 {

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    public static boolean isSubmit=false;

    @Override
    public void init(Bundle savedInstanceState)
    {
        // TODO Auto-generated method stub

        preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = preferences.edit();

        String isFinish = preferences.getString("finish", "...");
        Log.e("test", isFinish);
        if (isFinish.length() != 0 && isFinish.compareTo("true") == 0)
        {
            Intent intent = new Intent();
            intent.setClass(Guide.this, MainActivity.class);
            Guide.this.startActivity(intent);
        }
        else
        {
            addSlide(new FirstSlide(), getApplicationContext());
            addSlide(new SecondSlide(), getApplicationContext());
            addSlide(new ThirdSlide(), getApplicationContext());
            //addSlide(new FourthSlide(), getApplicationContext());
        }
    }


    @Override
    public void onDonePressed()
    {
        // TODO Auto-generated method stub

        if(isSubmit==false)
        {
            Toast.makeText(MainApplication.getContext(), "请点击确定", Toast.LENGTH_SHORT).show();
            return ;
        }

        System.out.println("IntroActivity onDonePressed");

        editor.putString("finish","true");
        editor.commit();

        Intent intent=new Intent();
        intent.setClass(Guide.this, MainActivity.class);
        Guide.this.startActivity(intent);

    }

}