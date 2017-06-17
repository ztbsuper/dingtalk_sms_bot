package com.marvin.dingtalksmsbot;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by Marvin on 2017/6/17.
 */
public class BackgroundService extends IntentService {
    private IncomingSms incomingSms;
    private IntentFilter intentFilter;
    private static final String TAG = "BackgroundService";

    public BackgroundService() {
        super("dingtalk-sms-bot");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG,"background handle intent");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"background service starting");
        incomingSms = new IncomingSms();
        intentFilter= new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(incomingSms,intentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(incomingSms);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }
}
