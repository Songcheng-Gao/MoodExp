package org.graduation.collector;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.provider.Settings;
import android.util.Log;

import org.graduation.database.DatabaseManager;

/**
 * Created by javan on 2016/4/25.
 */
public class AudioCollector implements ICollector {
    private static final String TAG = "AudioRecord";
    AudioRecord mAudioRecord = null;
    private static boolean isGetVoiceRun=false;
    private static boolean collectFlag=false;
    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    private int bufferSize = 0;
    static AudioDemo audio;
    static boolean bIsAudioDemoCreate=false;

    public AudioCollector()
    {
        if(bIsAudioDemoCreate==false) {
            audio = new AudioDemo();
            audio.statAudio();
            bIsAudioDemoCreate = true;
        }
    };


    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]
                    { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[]
                        { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: "
                                + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(
                                rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(
                                    MediaRecorder.AudioSource.DEFAULT,
                                    rate,
                                    channelConfig,
                                    audioFormat,
                                    bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                this.bufferSize = bufferSize;
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + " Exception, keep trying.");
                    }
                }
            }
        }
        return null;
    }


    public void collect()
    {

        Log.e("vol",audio.getVolume()+"");

        DatabaseManager.getDatabaseManager().saveAudio(System.currentTimeMillis(), audio.getVolume());
    }

    @Override
    public void startCollect()
    {
        if (isGetVoiceRun)
        {
            return;
        }
        collectFlag=true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(collectFlag){
                    collect();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    public void stopCollect() {
        collectFlag=false;
    }

    public class AudioDemo
    {

        private static final String TAG = "AudioRecord";
        static final int SAMPLE_RATE_IN_HZ = 8000;
        final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord mAudioRecord;
        boolean isGetVoiceRun;
        Object mLock;

        double volume=20;

        public AudioDemo()
        {
            mLock = new Object();
        }

        public double getVolume()
        {
            //Log.e("test",volume+"");
            if(volume>100) return 100;
            else  return volume;
        }

        public void statAudio()
        {
            if (isGetVoiceRun) {
                Log.e(TAG, "还在录着呢");
                return;
            }
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
            if (mAudioRecord == null) {
                Log.e("sound", "mAudioRecord初始化失败");
            }
            isGetVoiceRun = true;

            new Thread(new Runnable()
            {
                @Override
                public void run() {
                    mAudioRecord.startRecording();
                    short[] buffer = new short[BUFFER_SIZE];
                    while (isGetVoiceRun) {
                        //r是实际读取的数据长度，一般而言r会小于buffersize
                        int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (int i = 0; i < buffer.length; i++) {
                            v += buffer[i] * buffer[i];
                        }
                        // 平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) r;
                        volume = 10 * Math.log10(mean);
                        //Log.d("test", "分贝值:" + volume);
                        // 大概一秒十次
                        synchronized (mLock) {
                            try {
                                mLock.wait(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
            }).start();
        }
    }



}
