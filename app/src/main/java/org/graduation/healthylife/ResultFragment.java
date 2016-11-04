package org.graduation.healthylife;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.graduation.R;
import org.graduation.collector.ContactCollector;
import org.graduation.database.DatabaseManager;
import org.graduation.database.SharedPreferenceManager;

import java.util.ArrayList;

public class ResultFragment extends Fragment
{//upload ftp and the final view

    int cnt;
    Cursor cursor;
    public ArrayList< Integer> stepList;
    public ArrayList< Float> volumeList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.content_result, container, false);
        TextView tv= (TextView) view.findViewById(R.id.textView3);
        cnt= SharedPreferenceManager.getManager().getInt("emotionCnt",0);
        Log.d("resultFragment","emotion"+cnt);

        stepList=new ArrayList<Integer>();
        volumeList=new ArrayList<Float>();

        if(cnt<60) tv.setText("您已经提交了"+cnt+"次，还有"+(60-cnt)+"次可以领取奖励");
        else tv.setText("您已经提交了足够的次数。\n您的ID是"+SharedPreferenceManager.getManager().getString("phoneID", null)
                +"。\n联系张啸(tobexiao1@gmail.com)确认上传成功后即可领取奖励。");

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(cnt>=57)
                {
                    new ContactCollector().collect();
                    readDB();
                    saveDailyStep();
                    saveDailyVolume();

                }
                else
                {//Log.e("contact","-1");
                    //new ContactCollector().collect();
                    readDB();
                    saveDailyStep();
                    saveDailyVolume();
                }
                new FtpUploader().upload();
            }
        }).start();
        return view;
    }


    public void saveDailyStep()
    {
        if(stepList.size()==0) return ;

        DatabaseManager manager = DatabaseManager.getDatabaseManager();
        manager.saveDailyStep(stepList.get(stepList.size()-1));
    }

    public void saveDailyVolume()
    {
        if(volumeList.size()==0) return;

        DatabaseManager manager = DatabaseManager.getDatabaseManager();
        double average=0;
        for(int i=0;i<volumeList.size();++i)
        {
            average+=volumeList.get(i);
        }
        average=average/(double)volumeList.size();
        manager.saveDailyVolume((float) average);

        Log.e("vol","saved");
    }
    public void readDB()
    {
        cursor=DatabaseManager.getDatabaseManager().queryAudio();
        //private static final String CREATE_AUDIO = "create table audio ("
        //        + "start_time integer,"
        //        + "volume real)";

        volumeList=new ArrayList<Float>();
        //volumeList.clear();
        while(cursor.moveToNext())
        {
            volumeList.add(cursor.getFloat(1));
        }

        cursor=DatabaseManager.getDatabaseManager().queryAcceleration();
        //private static final String CREATE_ACC = "create table acceleration ("
        //        + "start_time integer,"
        //        + "steps integer,"
        //        + "x_axis float,"
        //        + "y_axis float,"
        //        + "z_axis float)";

        stepList=new ArrayList<Integer>();
        //stepList.clear();
        while(cursor.moveToNext())
        {
            stepList.add(cursor.getInt(1));
        }
    }

}
