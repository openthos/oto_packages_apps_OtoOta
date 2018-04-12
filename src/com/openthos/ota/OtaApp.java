package com.openthos.ota;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class OtaApp extends Application {
    public Service mService;
    public static final String SP_NAME = "SP_NAME";
    public static final String TIPS = "TIPS";
    public static final String AUTO = "AUTO";


    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void startAutoUpdate(Context context, boolean isStart) {
        Intent intent = new Intent();
        intent.setClass(context, AutoUpdateService.class);
        if (isStart) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }
}
