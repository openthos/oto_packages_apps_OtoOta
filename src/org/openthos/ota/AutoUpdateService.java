package org.openthos.ota;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import org.openthos.ota.utils.OtaReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Notification.DEFAULT_ALL;
import static android.app.Notification.VISIBILITY_PUBLIC;

public class AutoUpdateService extends Service {
    private static final int TIME = 36000;
    private File mOtaFile = null;
    private ArrayList<String> alUpDate;
    private final static int VERSION_LINE = 0;
    private final static int VALUE_COLUMN = 1;

    private String mSuffix = "user/";
    private String mBasePath = "";

    @Override
    public void onCreate() {
        super.onCreate();
        checkBate();
        mBasePath = getSharedPreferences("OTA", Context.MODE_PRIVATE).getString("DownloadUrl", "");
        if (TextUtils.isEmpty(mBasePath)) {
            mBasePath = "https://mirrors.tuna.tsinghua.edu.cn/openthos/OTA/";
            getSharedPreferences("OTA", Context.MODE_PRIVATE).edit()
                    .putString("DownloadUrl", mBasePath).commit();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String downloadPath = getDonwloadPath() + "/oto_ota.ver";
                mOtaFile = new File(downloadPath);
                if (mOtaFile.exists()) {
                    mOtaFile.delete();
                }
                downLoadnewVersion(getDownloadUrl("oto_ota.ver"), downloadPath);
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, TIME);
        return Service.START_STICKY_COMPATIBILITY;
    }

    private void showNotification(String version) {
        Bitmap btm = BitmapFactory.decodeResource(getResources(),
                R.drawable.otaoto);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.disconvery_new_version))
                .setContentText(getString(R.string.disconvery_new_version) + version)
                .setTicker(getString(R.string.disconvery_new_version))
                .setLargeIcon(btm)
                .setSmallIcon(R.drawable.otaoto)
                .setNumber(0)
                .setAutoCancel(true)
                .setDefaults(DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setVisibility(VISIBILITY_PUBLIC)
                .setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager
                 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

    private String getDonwloadPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getPath()
                    + File.separator + "System_Os";
        } else {
            return getFilesDir().getAbsolutePath();
        }
    }

    private void downLoadnewVersion(final String url, final String path) {
        new HttpUtils().download(url, path, false, false, new RequestCallBack<File>() {
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                File f = responseInfo.result;
                if (f.length() > 0) {
                    checkNewVersion();
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    private String getCurrentVersion() {
        String path = "/system/version";
        if (new File(path).exists()) {
            String[] data = OtaReader.getFileDes(new File(path)).split("\n");
            return data[VERSION_LINE].split(":")[VALUE_COLUMN];
        }
        return String.valueOf(0);
    }

    private void checkNewVersion() {
        if (mOtaFile.exists()) {
            alUpDate = OtaReader.getArraylist(mOtaFile);
            String str = alUpDate.get(VERSION_LINE);
            String[] strings = str.split("=");
            String version = strings[VALUE_COLUMN];
            String currentVersion = getCurrentVersion();
            try {
                String currentversion = currentVersion.replace(".", "");
                String newversion = version.replace(".", "");
                int cv = Integer.parseInt(currentversion);
                int nv = Integer.parseInt(newversion);
                if (nv > cv) {
                    showNotification(version);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getDownloadUrl(String url) {
        return mBasePath + mSuffix + url;
    }

    private boolean checkBate() {
        try {
            Process pro = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "HOME=/system/gnupg/home gpg --list-keys"});
            BufferedReader in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                if (line.contains("Openthos Test")) {
                    mSuffix = "dev/";
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
