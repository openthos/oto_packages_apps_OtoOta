package com.openthos.ota;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import com.openthos.utis.MyReader;
import com.openthos.utis.Md5;

public class MainActivity extends Activity{
    private File mOtaFile = null;
    private File mReleaseNoteFile = null;
    private File mDownloadFile = null;
    private File mMd5File = null;
    private ProgressBar mProgressBar;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private final static String CURRENT_VERSION = "1.8.7";
    private ArrayList<String> alUpDate;
    private TextView mError;
    private TextView mPrtv;
    private RelativeLayout mUpdate;
    private RelativeLayout mShowHaveUpdate;
    private RelativeLayout mCurrentVersion;
    private RelativeLayout mUpdateNow;
    private Button mUpdateNowButton;
    private Button mUpdate_introduce;
    private final static int TIMER_CLOSING_PAGE_INTERVAL = 5000; // 5 second.
    private final static int TIMER_CHECK_VERSION_INTERVAL = 5000; // 5second.

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSharedPreferences = OtaApp.getContext().getSharedPreferences("update",
                             OtaApp.getContext().MODE_WORLD_READABLE |
                             OtaApp.getContext().MODE_WORLD_WRITEABLE |
                             OtaApp.getContext().MODE_APPEND);
        mEditor = mSharedPreferences.edit();
        mEditor.putBoolean("default", true);
        mEditor.apply();
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        window.setAttributes(layoutParams);
        float density = getResources().getDisplayMetrics().density;
        initView();
        setListen();
        initData();
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
        mError = (TextView) findViewById(R.id.error);
        mPrtv = (TextView) findViewById(R.id.prtv);
        mUpdate_introduce = (Button) findViewById(R.id.update_introduce);
        mUpdateNow = (RelativeLayout) findViewById(R.id.updateNow);
        mUpdateNowButton = (Button) findViewById(R.id.updateNowButton);
        mShowHaveUpdate = (RelativeLayout) findViewById(R.id.showHaveUpdate);
        mCurrentVersion = (RelativeLayout) findViewById(R.id.currentVersion);

    }

    //downLoadnewVersieon(getDownloadUrl(update),gtDonwloadPath() + "/update.txt");
    //http://192.168.0.180/openthos/oto_ota.ver
    private void downLoadnewVersion(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.configTimeout(TIMER_CLOSING_PAGE_INTERVAL);
        httpUtils.configSoTimeout(TIMER_CHECK_VERSION_INTERVAL);
        httpUtils.download(url, path, true, true, new RequestCallBack<File>() {
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mOtaFile.exists()) {
                    alUpDate = MyReader.getArraylist(mOtaFile);
                    String str = alUpDate.get(0);
                    //Version=1.8.８
                    String[] strings = str.split("=");
                    String version = strings[1];
                    if (!version.equals(CURRENT_VERSION)) {
                        mCurrentVersion.setVisibility(View.GONE);
                        mUpdate.setVisibility(View.VISIBLE);
                        mUpdateNow.setVisibility(View.VISIBLE);
                    }else {
                        mCurrentVersion.setVisibility(View.GONE);
                        mUpdate.setVisibility(View.GONE);
                        mUpdateNow.setVisibility(View.GONE);
                        mError.setText(getResources().getString(R.string.errorNet));
                    }
                }
            }
            @Override
            public void onFailure(HttpException e, String s) {
                mCurrentVersion.setVisibility(View.GONE);
                mError.setVisibility(View.VISIBLE);
                mError.setText(getResources().getString(R.string.errorNet));
            }
        });
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
        String  mReleaseNoteFileName = alUpDate.get(1).split("=")[1];
        String  mReleaseNoteFilePath = getDonwloadPath() + "/" + mReleaseNoteFileName;
        mReleaseNoteFile = new File(mReleaseNoteFilePath);
        if (mReleaseNoteFile.exists()) {
            mReleaseNoteFile.delete();
        }
        downLoadDescription(getDownloadUrl(mReleaseNoteFileName), mReleaseNoteFilePath);
    }

    private void downLoadDescription(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.configTimeout(TIMER_CLOSING_PAGE_INTERVAL);
        httpUtils.configSoTimeout(TIMER_CHECK_VERSION_INTERVAL);
        httpUtils.download(url, path, true, false, new RequestCallBack<File>() {
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mReleaseNoteFile.exists()) {
                    String str = MyReader.getFileDes(mReleaseNoteFile);
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getResources().getString(R.string.update_introduce))
                        .setMessage(str)
                        .setPositiveButton(getResources().getString(R.string.confirm),
                         new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                             }
                         }).show();
                }
            }
            @Override
            public void onFailure(HttpException e, String s) {
                mError.setVisibility(View.VISIBLE);
                mError.setText(getResources().getString(R.string.errorNet));
            }
        });
    }

    private void updateVersion() {
        String mMd5FileName = alUpDate.get(2).split("=")[1] + ".md5";
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
    }

    private void downLoadMd5(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.download(url, path, true, false, new RequestCallBack<File>() {
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mMd5File.exists()) {
                    String mDownloadFileName = alUpDate.get(2).split("=")[1];
                    String mDownloadFilePath = getDonwloadPath() + "/" + mDownloadFileName;
                    mDownloadFile = new File(mDownloadFilePath);
                    if (mDownloadFile.exists()) {
                        mDownloadFile.delete();
                    }
                    downLoadUpdateFile(getDownloadUrl(mDownloadFileName), mDownloadFilePath);
                    mError.setVisibility(View.VISIBLE);
                    mError.setText("Download failed");
                }
            }
            @Override
            public void onFailure(HttpException e, String s) {
                mError.setVisibility(View.VISIBLE);
                mError.setText(getResources().getString(R.string.errorNet));
            }
        });
    }

    private void downLoadUpdateFile(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.download(url, path, true, false, new RequestCallBack<File>() {
            @Override
            public void onStart() {
                super.onStart();
            }
            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
                mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                mShowHaveUpdate.setVisibility(View.VISIBLE);
                mProgressBar.setMax((int) total);
                mProgressBar.setProgress((int) current);
                int percent = ((int) (current / total) * 100);
                mPrtv.setText("loading...");
            }
            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mDownloadFile.exists()) {
                    String fileMD5 = Md5.getFileMD5(mDownloadFile);
                    ArrayList<String> arr = MyReader.getArraylist(mMd5File);
                    String[] checkMD5Info = arr.get(0).split("=");
                    String checkMD5 = checkMD5Info[1].trim();
                    if (fileMD5.equals(checkMD5)) {
                        //data/media/0/System_Os/update
                        String filePath =  getDonwloadPath() + "/update";
                        File upFile = new File(filePath);
                        if (!upFile.exists()) {
                            try {
                                upFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        MyReader.writeFile(upFile, "upadte.zip");
                        mShowHaveUpdate.setVisibility(View.GONE);
                        showMyDialog(MainActivity.this);
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mDownloadFile.delete();
                                mMd5File.delete();
                                mReleaseNoteFile.delete();
                                mOtaFile.delete();
                            }
                        });
                        mShowHaveUpdate.setVisibility(View.GONE);
                        Timer timer = new Timer();
                        mOtaFile.delete();
                        mDownloadFile.delete();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                            }
                        };
                        timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
                    }
                } else {
                    mError.setVisibility(View.VISIBLE);
                    mError.setText(getResources().getString(R.string.errorNet));
                }
            }
            @Override
            public void onFailure(HttpException e, String s) {
            }
        });
    }

    //http://192.168.0.180/openthos/oto_ota.ver
    private String getDownloadUrl(String url) {
        String basePath = "http://192.168.0.180/openthos/";
        String path = basePath + url;
        return path;
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
        new AlertDialog.Builder(context)
            .setTitle(getResources().getString(R.string.downloadsucessdate))
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
                            mError.setVisibility(View.VISIBLE);
                            mError.setText(getResources().getString(R.string.System_update));
                        }
                    }).show();
    }

    public void getFinish() {
        finish();
    }
}
