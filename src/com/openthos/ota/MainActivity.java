package com.openthos.ota;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.openthos.utis.OtaReader;

public class MainActivity extends Activity {
    private File mOtaFile = null;
    private File mReleaseNoteFile = null;
    private File mDownloadFile = null;
    private File mUpdateFile = null;
    private ProgressBar mProgressBar;
    private ProgressDialog progressDialog;
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
    private Button mUpdateManualButton;

    private final static int OTAFILE = 0;
    private final static int RELEASENOTEFILE = 1;
    private final static int ERRORNET = 3;
    private final static int ERRORNOTICE = 4;
    private final static int VERSION_LINE = 0;
    private final static int RELEASENOTE_LINE = 1;
    private final static int MD5FILENAME_LINE = 2;
    private final static int VALUE_COLUMN = 1;
    private String mSuffix = "user/";
    private String mBasePath = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (checkBate()) {
            ((TextView) findViewById(R.id.shownewversion)).setText(
                    getString(R.string.openthos_new_version_develop));
            ((TextView) findViewById(R.id.openthos)).setText(
                    getString(R.string.otui_develop));
        }
        mBasePath = getSharedPreferences("OTA", Context.MODE_PRIVATE).getString("DownloadUrl", "");
        if (TextUtils.isEmpty(mBasePath)) {
            mBasePath = "https://mirrors.tuna.tsinghua.edu.cn/openthos/OTA/";
            getSharedPreferences("OTA", Context.MODE_PRIVATE).edit().putString("DownloadUrl", mBasePath).commit();
        }
        initView();
        setListen();
        initData();
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

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case OTAFILE:
                    downLoadOtaFile();
                    break;
                case RELEASENOTEFILE:
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
                    }
                    break;
                case ERRORNET:
                    mErrorlayout.setVisibility(View.VISIBLE);
                    mError.setText(getResources().getString(R.string.errorNet));
                    mCurrentVersion.setVisibility(View.VISIBLE);
                    getCurrentVersion();
                    mcurrent.setText(CURRENT_VERSION);
                    break;
                case ERRORNOTICE:
                    mUpdateNow.setVisibility(View.GONE);
                    mUpdateIntroduce.setVisibility(View.GONE);
                    mCurrentVersion.setVisibility(View.VISIBLE);
                    getCurrentVersion();
                    mcurrent.setText(CURRENT_VERSION);
                    mErrorlayout.setVisibility(View.VISIBLE);
                    mError.setText(getResources().getString(R.string.errornotice));
                    break;
            }
            return false;
        }
    });

    private boolean checkFile(File f) {
        mUpdateFile = new File(getDonwloadPath(), "update.zip");
        String result = "";
        try {
            Process pro = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "HOME=/system/gnupg/home gpg -o "
                            + mUpdateFile.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getPath(), "/sdcard")
                            + " -d " + f.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getPath(), "/sdcard")
                            + " ; echo $?"});
            BufferedReader in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                result = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            int code = Integer.parseInt(result);
            if (code != 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        if (mUpdateFile.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private String getCurrentVersion() {
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
        if (mOtaFile.exists()) {
            mOtaFile.delete();
        }
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
        mUpdateManualButton = (Button) findViewById(R.id.update_manual);

        mShowHaveUpdate = (RelativeLayout) findViewById(R.id.showHaveUpdate);
        mCurrentVersion = (RelativeLayout) findViewById(R.id.currentVersion);
        mErrorlayout = (RelativeLayout) findViewById(R.id.errorlayout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void downLoadnewVersion(final String url, final String path) {
        new HttpUtils().download(url, path, false, false, new RequestCallBack<File>() {
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                File f = responseInfo.result;
                if (f.length() > 0) {
                    Message message = mHandler.obtainMessage();
                    message.what = OTAFILE;
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                } else {
                    mHandler.sendEmptyMessage(ERRORNET);
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                mHandler.sendEmptyMessage(ERRORNET);
            }
        });
    }

    private void downLoadOtaFile() {
        if (mOtaFile.exists()) {
            alUpDate = OtaReader.getArraylist(mOtaFile);
            String str = alUpDate.get(VERSION_LINE);
            String[] strings = str.split("=");
            String version = strings[VALUE_COLUMN];
            getCurrentVersion();
            try {
                String currentversion = CURRENT_VERSION.replace(".", "");
                String newversion = version.replace(".", "");
                int cv = Integer.parseInt(currentversion);
                int nv = Integer.parseInt(newversion);
                if (nv > cv) {
                    mCurrentVersion.setVisibility(View.GONE);
                    mUpdate.setVisibility(View.VISIBLE);
                    mnewversion.setText(version);
                    mUpdateNow.setVisibility(View.VISIBLE);
                    mUpdateIntroduce.setVisibility(View.VISIBLE);
                } else {
                    Message message = mHandler.obtainMessage();
                    mHandler.sendEmptyMessage(ERRORNOTICE);
                }
            } catch (Exception ex) {
            }
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
        mUpdateManualButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.openthos.filemanager",
                        "com.openthos.filemanager.PickerActivity"));
                startActivityForResult(intent, 000);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            String filePath = data.getData().getEncodedPath();
            File f = new File(filePath);
            if (!f.exists()) {
                Toast.makeText(this, getString(R.string.check_error), Toast.LENGTH_SHORT).show();
                return;
            } else if (filePath.endsWith(".gpg")) {
                checkUpgradeGpgFile(f);
            } else {
                Toast.makeText(this, getString(R.string.check_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUpgradeGpgFile(final File f) {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(getString(R.string.verify_package));
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (checkFile(f)) {
                    String filePath = getDonwloadPath() + "/update";
                    File upFile = new File(filePath);
                    if (!upFile.exists()) {
                        try {
                            upFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    String updatename = mUpdateFile.getName();
                    OtaReader.writeFile(upFile, updatename);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            mShowHaveUpdate.setVisibility(View.GONE);
                            showMyDialog();
                        }
                    });
                } else {
                    try {
                        Runtime.getRuntime().exec(new String[]{"su", "-c", "rm -r /sdcard/System_Os"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, getString(R.string.verify_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }.start();
    }

    private void checkUpgradeZipFile(final File f) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.verify_package));
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread() {
            int isOK = 0;

            @Override
            public void run() {
                super.run();
                try {
                    ZipInputStream in = new ZipInputStream(new FileInputStream(f));
                    ZipEntry entry = in.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().equals("kernel")) {
                            isOK++;
                        } else if (entry.getName().equals("ramdisk.img")) {
                            isOK++;
                        } else if (entry.getName().equals("system.sfs")) {
                            isOK++;
                        } else if (entry.getName().equals("update.list")) {
                            isOK++;
                        } else if (entry.getName().equals("version")) {
                            isOK++;
                        }
                        entry = in.getNextEntry();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (isOK == 5) {
                            choiceToUpgrade(f);
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.verify_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }.start();
    }

    private void choiceToUpgrade(final File f) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(getString(R.string.confrim_manual));
        dialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                progressDialog.setMessage(getString(R.string.init_upgrade));
                progressDialog.show();
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Process pro = Runtime.getRuntime().exec(
                                    new String[]{"cp", f.getAbsolutePath(), getDonwloadPath()});
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(pro.getInputStream()));
                            while (in.readLine() != null) {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        OtaReader.writeFile(new File(getDonwloadPath() + "/update"), f.getName());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                showMyDialogBySelf();
                            }
                        });
                    }
                }.start();
            }
        });
        dialog.setCancelable(false);
        dialog.create().show();
    }

    private void updateDescription() {
        String mReleaseNoteFileName = alUpDate.get(RELEASENOTE_LINE).split("=")[VALUE_COLUMN];
        String mReleaseNoteFilePath = getDonwloadPath() + "/" + mReleaseNoteFileName;
        mReleaseNoteFile = new File(mReleaseNoteFilePath);
        downLoadDescription(getDownloadUrl(mReleaseNoteFileName), mReleaseNoteFilePath);
    }

    private void downLoadDescription(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        HttpHandler<File> http = httpUtils.download(url, path, false, false, new RequestCallBack<File>() {
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                File f = responseInfo.result;
                if (f.length() > 0) {
                    Message message = mHandler.obtainMessage();
                    message.what = RELEASENOTEFILE;
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                mHandler.sendEmptyMessage(ERRORNET);
            }
        });
    }

    private void updateVersion() {
        String mDownloadFileName = alUpDate.get(MD5FILENAME_LINE).split("=")[VALUE_COLUMN];
        String mDownloadFilePath = getDonwloadPath() + "/" + mDownloadFileName;
        Log.i("wwww", mDownloadFilePath);
        Log.i("wwww", mDownloadFileName);

        mDownloadFile = new File(mDownloadFilePath);
        downLoadUpdateFile(getDownloadUrl(mDownloadFileName), mDownloadFilePath);
        mCurrentVersion.setVisibility(View.GONE);
        mShowHaveUpdate.setVisibility(View.VISIBLE);
        mUpdate.setVisibility(View.GONE);
        mUpdateNow.setVisibility(View.GONE);
        mUpdateIntroduce.setVisibility(View.GONE);
    }

    private void downLoadUpdateFile(final String url, final String path) {
        new HttpUtils().download(url, path, true, false, new RequestCallBack<File>() {
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
                checkUpgradeGpgFile(mDownloadFile);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                mShowHaveUpdate.setVisibility(View.GONE);
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getDownloadUrl(String url) {
        return mBasePath + mSuffix + url;
    }

    @SuppressLint("WrongConstant")
    private String getDonwloadPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getPath()
                    + File.separator + "System_Os";
        } else {
            return getFilesDir().getAbsolutePath();
        }
    }

    public void showMyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
    }

    public void showMyDialogBySelf() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
    }
}
