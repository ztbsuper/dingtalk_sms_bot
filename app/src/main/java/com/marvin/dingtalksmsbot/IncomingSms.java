package com.marvin.dingtalksmsbot;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Marvin on 2017/6/17.
 */
public class IncomingSms extends BroadcastReceiver {
    private static final String TAG = "IncomingSms";

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    private static final String DINGTALK_URL = "https://oapi.dingtalk.com/robot/send?access_token=";


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage msg;
        if (null != bundle) {
            Object[] smsObj = (Object[]) bundle.get("pdus");
            for (Object object : smsObj) {
                msg = SmsMessage.createFromPdu((byte[]) object);
                sendDingtalkMessage(msg, context);
            }


        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void sendDingtalkMessage(SmsMessage smsMessage, Context context) {
// 如果没有设置,这里保底
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String dingtalkNick = preferences.getString("dingtalk_nick", telephonyManager.getLine1Number());
        if(Strings.isNullOrEmpty(dingtalkNick)){
            dingtalkNick = telephonyManager.getLine1Number();
        }
        String accesstoken = preferences.getString("dingtalk_accesstoken", null);
        String smsBodyFilter = preferences.getString("sms_body_filter", null);
        Log.i(TAG, "nick:" + dingtalkNick);
        Log.i(TAG, "accesstoken:" + accesstoken);
        Log.i(TAG, "smsBodyFilter:" + smsBodyFilter);

        // token没设置,退出
        if (Strings.isNullOrEmpty(accesstoken)) {
            Log.w(TAG, "no access token, ignore ");
            return;
        }

        String[] keywords = smsBodyFilter.split(",");
        String messageBody = smsMessage.getMessageBody();
        boolean shouldSend = false;
        for (String keyword : keywords) {
            if (messageBody.contains(keyword)) {
                shouldSend = true;
            }
        }
        // 没有关键词, 退出
        if (!shouldSend) {
            Log.w(TAG,"关键词不匹配, 退出");
            return;
        }
        Date date = new Date(smsMessage.getTimestampMillis());

        String content = String.format("[%s]于[%s]发来短信, 发件人[%s], 内容:[%s]", dingtalkNick,date.toString(), smsMessage.getOriginatingAddress(), smsMessage.getMessageBody());

        // 开始发钉钉消息
        Map<String, Object> dingtalkMessage = Maps.newHashMap();

        dingtalkMessage.put("msgtype", "text");
        Map<String, String> contentMap = Maps.newHashMap();
        contentMap.put("content", content);
        dingtalkMessage.put("text", contentMap);

        RequestBody requestBody = RequestBody.create(JSON, JSONObject.toJSONString(dingtalkMessage));
        Request request = new Request.Builder()
                .url(DINGTALK_URL + accesstoken)
                .post(requestBody)
                .build();
        new Thread(new HttpSender(OK_HTTP_CLIENT,request)).start();
    }


    public static class HttpSender implements Runnable{
        private OkHttpClient client;
        private Request request;

        public HttpSender(OkHttpClient client, Request request) {
            this.client = client;
            this.request = request;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            try (Response response = client.newCall(request).execute()) {
                Log.i(TAG, response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

}
