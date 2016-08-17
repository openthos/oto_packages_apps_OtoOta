package com.openthos.ota;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class OtaApp extends Application {
    private static Context context;
    private static int mainThreadId;
    private static Handler mainHandler;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mainThreadId = android.os.Process.myTid();
        mainHandler = new Handler();
    }

    public static Context getContext(){
        return context;
    }

    public static int getMainThreadId(){
        return mainThreadId;
    }

    public static Handler getMainHandler(){
        return mainHandler;
    }
}
