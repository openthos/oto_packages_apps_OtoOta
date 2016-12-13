package com.openthos.ota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.database.Cursor;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.openthos.utis.OtaNetUtils;
import com.openthos.utis.OtaReader;
import com.openthos.utis.OtaMd5;

public class MainActivity extends Activity {
    private File mOtaFile = null;
    private File mReleaseNoteFile = null;
    private File mDownloadFile = null;
    private File mMd5File = null;
    private ProgressBar mProgressBar;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private String CURRENT_VERSION = null;
    private ArrayList<String> alUpDate;
    private TextView mError;
    private TextView mcurrent;
    private TextView mnewversion;
    private RelativeLayout mUpdate;
    private RelativeLayout mShowHaveUpdate;
    private RelativeLayout mCurrentVersion;
    private RelativeLayout mUpdateNow;
    private RelativeLayout mUpdateIntroduce;
    private RelativeLayout mErrorlayout;
    private Button mUpdateNowButton;
    private Button mUpdate_introduce;
    private HttpHandler<File> http1;
    private String mNetStr;
    private final static int TIMER_CLOSING_PAGE_INTERVAL = 3000; // 3 second.
    private final static int TIMER_CHECK_VERSION_INTERVAL = 3000; // 3second.
    private final static int OTAFILE = 0;
    private final static int RELEASENOTEFILE = 1;
    private final static int MD5FILE = 2;
    private final static int VERSION_LINE = 0;
    private final static int RELEASENOTE_LINE = 1;
    private final static int MD5FILENAME_LINE = 2;
    private final static int VALUE_COLUMN = 1;
    private final static double DIALOG_WIDTH_FACTOR = 0.5;
    private final static double DIALOG_HEIGHT_FACTOR = 0.6;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSharedPreferences = getSharedPreferences("update", MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        mEditor.putBoolean("default", true);
        mEditor.commit();
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        window.setAttributes(layoutParams);
        float density = getResources().getDisplayMetrics().density;
        initView();
        setListen();
        initData();
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String path = "";
            Bundle data = msg.getData();
            if (data != null) {
                path = data.getString("path");
            }
            switch (msg.what) {
                case OTAFILE:
                    if (mNetStr != null) {
                        if (OtaReader.writeFile(path, mNetStr, true)) {
                            downLoadOtaFile();
                        } else {
                            mOtaFile.delete();
                            mErrorlayout.setVisibility(View.VISIBLE);
                            mError.setText(getResources().
                            getString(R.string.errorNet));
                            mCurrentVersion.setVisibility(View.VISIBLE);
                            CurrentVersion();
                            mcurrent.setText(CURRENT_VERSION);
                        }
                    } else {
                        mOtaFile.delete();
                        mErrorlayout.setVisibility(View.VISIBLE);
                        mError.setText(getResources().
                        getString(R.string.errorNet));
                        mCurrentVersion.setVisibility(View.VISIBLE);
                        CurrentVersion();
                        mcurrent.setText(CURRENT_VERSION);
                    }
                    break;
                case RELEASENOTEFILE:
                    if (mNetStr != null) {
                        if (OtaReader.writeFile(path, mNetStr, true)) {
                            if (mReleaseNoteFile.exists()) {
                                String str = OtaReader.getFileDes(mReleaseNoteFile);
                                AlertDialog.Builder builder = new AlertDialog.
                                                   Builder(MainActivity.this);
                                builder.setTitle(getResources()
                                       .getString(R.string.update_introduce))
                                       .setMessage(str)
                                       .setPositiveButton(getResources()
                                       .getString(R.string.confirm),
                                           new DialogInterface.OnClickListener() {
                                           @Override
                                           public void onClick(DialogInterface dialog, int which) {
                                           }
                                           });
                                AlertDialog ad = builder.create();
                                ad.setCanceledOnTouchOutside(false);
                                ad.setCancelable(false);
                                ad.show();
                                WindowManager windowManager = getWindowManager();
                                Display display = windowManager.getDefaultDisplay();
                                WindowManager.LayoutParams params = ad.getWindow().getAttributes();
                                params.width = (int) (display.getWidth() * DIALOG_WIDTH_FACTOR);
                                params.height = (int) (display.getHeight() * DIALOG_HEIGHT_FACTOR);
                                params.gravity = Gravity.CENTER;
                                ad.getWindow().setGravity(Gravity.CENTER_HORIZONTAL |
                                                            Gravity.CENTER_VERTICAL);
                                ad.onWindowAttributesChanged(params);
                                ad.getWindow().setAttributes(params);
                            }
                        } else {
                            mReleaseNoteFile.delete();
                            Toast.makeText(MainActivity.this, R.string.error,
                                           Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mReleaseNoteFile.delete();
                        Toast.makeText(MainActivity.this, R.string.error,
                                       Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MD5FILE:
                    if (mNetStr != null) {
                        if (OtaReader.writeFile(path, mNetStr, true)) {
                            downLoadMd5File();
                        }
                    } else {
                        mDownloadFile.delete();
                    }
                    break;
                default:
                     break;
            }
            return false;
        }
    });

    private String CurrentVersion() {
        String path = "/system/version";
        if (new File(path).exists()) {
            String[] data = OtaReader.getFileDes(new File(path)).split("\n");
            return CURRENT_VERSION = data[VERSION_LINE].split(":")[VALUE_COLUMN];
        }
        return CURRENT_VERSION;
    }

    private void initData() {
        String downloadPath = getDonwloadPath() + "/oto_ota.ver";
        mOtaFile = new File(downloadPath);
        downLoadnewVersion(getDownloadUrl("oto_ota.ver"), downloadPath);
    }

    private void initView() {
        mUpdate = (RelativeLayout) findViewById(R.id.update);
        mcurrent = (TextView) findViewById(R.id.current_version);
        mnewversion = (TextView) findViewById(R.id.newversion);
        mError = (TextView) findViewById(R.id.error);
        mUpdate_introduce = (Button) findViewById(R.id.update_introduce);
        mUpdateNow = (RelativeLayout) findViewById(R.id.updateNow);
        mUpdateIntroduce = (RelativeLayout) findViewById(R.id.updateIntroduce);
        mUpdateNowButton = (Button) findViewById(R.id.updateNowButton);
        mShowHaveUpdate = (RelativeLayout) findViewById(R.id.showHaveUpdate);
        mCurrentVersion = (RelativeLayout) findViewById(R.id.currentVersion);
        mErrorlayout = (RelativeLayout) findViewById(R.id.errorlayout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void downLoadnewVersion(final String url, final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mNetStr = OtaNetUtils.getNetStr(MainActivity.this, url);
                if (mNetStr != null) {
                    Message message = mHandler.obtainMessage();
                    message.what = OTAFILE;
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                } else {
                    mErrorlayout.setVisibility(View.VISIBLE);
                    mError.setText(getResources().getString(R.string.errorNet));
                    mCurrentVersion.setVisibility(View.VISIBLE);
                    CurrentVersion();
                    mcurrent.setText(CURRENT_VERSION);
                }
            }
        }).start();
    }

    private void downLoadOtaFile() {
        if (mOtaFile.exists()) {
            alUpDate = OtaReader.getArraylist(mOtaFile);
            String str = alUpDate.get(VERSION_LINE);
            String[] strings = str.split("=");
            String version = strings[VALUE_COLUMN];
            if (!version.equals("") && !version.equals(CURRENT_VERSION)) {
                mCurrentVersion.setVisibility(View.GONE);
                mUpdate.setVisibility(View.VISIBLE);
                mnewversion.setText(version);
                mUpdateNow.setVisibility(View.VISIBLE);
                mUpdateIntroduce.setVisibility(View.VISIBLE);
            } else {
                mUpdate.setVisibility(View.GONE);
                mUpdateNow.setVisibility(View.GONE);
                mUpdateIntroduce.setVisibility(View.GONE);
                mCurrentVersion.setVisibility(View.VISIBLE);
                CurrentVersion();
                mcurrent.setText(CURRENT_VERSION);
                mErrorlayout.setVisibility(View.VISIBLE);
                mError.setText(getResources().getString(R.string.errornotice));
            }
        } else {
            mErrorlayout.setVisibility(View.VISIBLE);
            mError.setText(getResources().getString(R.string.errornotice));
            mCurrentVersion.setVisibility(View.VISIBLE);
            CurrentVersion();
            mcurrent.setText(CURRENT_VERSION);
        }
    }

    private void setListen() {
        mUpdate_introduce.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDescription();
            }
        });
        mUpdateNowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateVersion();
            }
        });
    }

    private void updateDescription() {
        String mReleaseNoteFileName = alUpDate.get(RELEASENOTE_LINE).split("=")[VALUE_COLUMN];
        String mReleaseNoteFilePath = getDonwloadPath() + "/" + mReleaseNoteFileName;
        mReleaseNoteFile = new File(mReleaseNoteFilePath);
        downLoadDescription(getDownloadUrl(mReleaseNoteFileName), mReleaseNoteFilePath);
    }

    private void downLoadDescription(final String url, final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mNetStr = OtaNetUtils.getNetStr(MainActivity.this, url);
                if (mNetStr != null) {
                    Message message = mHandler.obtainMessage();
                    message.what = RELEASENOTEFILE;
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                }
            }
        }).start();
    }

    private void updateVersion() {
        String mMd5FileName = alUpDate.get(MD5FILENAME_LINE).split("=")[VALUE_COLUMN] + ".md5";
        String mMd5FilePath = getDonwloadPath() + "/" + mMd5FileName;
        mMd5File = new File(mMd5FilePath);
        if (mMd5File.exists()) {
            mMd5File.delete();
        }
        downLoadMd5(getDownloadUrl(mMd5FileName), mMd5FilePath);
        mCurrentVersion.setVisibility(View.GONE);
        mShowHaveUpdate.setVisibility(View.VISIBLE);
        mUpdate.setVisibility(View.GONE);
        mUpdateNow.setVisibility(View.GONE);
        mUpdateIntroduce.setVisibility(View.GONE);
    }

    private void downLoadMd5(final String url, final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mNetStr = OtaNetUtils.getNetStr(MainActivity.this, url);
                if (mNetStr != null) {
                    Message message = mHandler.obtainMessage();
                    message.what = MD5FILE;
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                }
            }
        }).start();
    }

    private void downLoadMd5File() {
        if (mMd5File.exists()) {
            String mDownloadFileName = alUpDate.get(MD5FILENAME_LINE).split("=")[VALUE_COLUMN];
            String mDownloadFilePath = getDonwloadPath() + "/" + mDownloadFileName;
            mDownloadFile = new File(mDownloadFilePath);
            downLoadUpdateFile(getDownloadUrl(mDownloadFileName), mDownloadFilePath);
        }
    }

    private void downLoadUpdateFile(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        http1 = httpUtils.download(url, path, true, false, new RequestCallBack<File>() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
                mShowHaveUpdate.setVisibility(View.VISIBLE);
                int percent = ((int) (100 * current / total));
                mProgressBar.setProgress(percent);
            }

            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mDownloadFile.exists()) {
                    String fileMD5 = OtaMd5.getFileMD5(mDownloadFile);
                    ArrayList<String> arr = OtaReader.getArraylist(mMd5File);
                    String[] checkMD5Info = arr.get(VERSION_LINE).split("=");
                    String checkMD5 = checkMD5Info[VALUE_COLUMN].trim();
                    if (fileMD5.equals(checkMD5)) {
                        //data/media/0/System_Os/update
                        String filePath = getDonwloadPath() + "/update";
                        File upFile = new File(filePath);
                        if (!upFile.exists()) {
                            try {
                                upFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        String updatename = mDownloadFile.getName();
                        OtaReader.writeFile(upFile, updatename);
                        mShowHaveUpdate.setVisibility(View.GONE);
                        showMyDialog(MainActivity.this);
                    } else {
                        mShowHaveUpdate.setVisibility(View.GONE);
                        Timer timer = new Timer();
                        mOtaFile.delete();
                        mMd5File.delete();
                        mDownloadFile.delete();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                            }
                        };
                        timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                if (mDownloadFile.exists()) {
                    mDownloadFile.delete();
                }
                downLoadUpdateFile(url, path);
                Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //http://192.168.0.180/openthos/oto_ota.ver
    private String getDownloadUrl(String url) {
        String basePath = "";
        Uri uriQuery = Uri.parse("content://com.otosoft.tools.myprovider/upgradeUrl");
        if (uriQuery != null) {
            Cursor cursor = getContentResolver().query(uriQuery, null, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                basePath = cursor.getString(cursor.getColumnIndex("upgradeUrl"));
                cursor.close();
            }
        }
        if (TextUtils.isEmpty(basePath)) {
            basePath = "http://dev.openthos.org/openthos/";
        }
        return basePath + url;
    }

    private String getDonwloadPath() {
        mSharedPreferences = OtaApp.getContext().getSharedPreferences("update",
                             OtaApp.getContext().MODE_WORLD_WRITEABLE |
                             OtaApp.getContext().MODE_APPEND |
                             OtaApp.getContext().MODE_WORLD_READABLE);
        Boolean check = mSharedPreferences.getBoolean("default", false);
        if (check) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return Environment.getExternalStorageDirectory().getPath()
                        + File.separator + "System_Os";
            } else {
                return getFilesDir().getAbsolutePath();
            }
        } else {
            String path = mSharedPreferences.getString("path", "");
            return path;
        }
    }

    public void showMyDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.downloadsucessdate))
                .setMessage(getResources().getString(R.string.downloadsucess))
                .setPositiveButton(getResources().getString(R.string.install),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PowerManager powerManager = (PowerManager) MainActivity.this.
                                getApplicationContext().getSystemService(Context.POWER_SERVICE);
                                powerManager.reboot(null);
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.cancle),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mErrorlayout.setVisibility(View.VISIBLE);
                                mError.setText(getResources().getString(R.string.System_update));
                            }
                        });
        AlertDialog ad = builder.create();
        ad.setCanceledOnTouchOutside(false);
        ad.setCancelable(false);
        ad.show();
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams params = ad.getWindow().getAttributes();
        params.width = (int) (display.getWidth() * DIALOG_WIDTH_FACTOR);
        params.height = (int) (display.getHeight() * DIALOG_HEIGHT_FACTOR);
        params.gravity = Gravity.CENTER;
        ad.getWindow().setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        ad.getWindow().setAttributes(params);
    }

    public void getFinish() {
        finish();
    }
}
