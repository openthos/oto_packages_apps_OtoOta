package com.openthos.ota;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.openthos.utis.Md5;

import com.openthos.utis.MyReader;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
public class MainActivity extends Activity {
    private File otaFile = new File(getDonwloadPath() + "/openthos-date.iso");
    private File updatefile = new File(getDonwloadPath() + "/update.txt");
    private File md5File = new File(getDonwloadPath() + "/android-date.md5");
    private File introuceFile = new File(getDonwloadPath() + "/introduce.txt");
    private String update = "update";
    private String md5 = "md5";
    private String openthos = "openthos";
    private String introduce = "introduce";
    private RelativeLayout mShow_no_update;
    private TextView mShowCurrentVersion;
    private RelativeLayout mShowHaveUpdate;
    private TextView mShowNoNet1;
    private ProgressBar mProgressBar;
    private TextView mSize;
    private TextView mSpeed;
    private TextView mTime;
    private TextView mInstall;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    private static ImageView mExit2;
    private int cancle=0;

    private final static int TIMER_CLOSING_PAGE_INTERVAL = 1000; // 1 second.
    private final static int TIMER_CHECK_VERSION_INTERVAL = 100; // 0.1 second.
    private final static int SWITCH_B_TO_MB_NUMBER = 1000000; // B to MB.
    private final static int TIMER_SWITCH_MILLISECOND_TO_SECOND = 1000; // 1 second.
    private final static int SWITCH_B_TO_KB = 1000; // B to KB.
    private final static double DOUBLE_SWITCH_B_TO_KB = 1000.0D; //double B to KB.
    private final static int SWITCH_SECOND_TO_MINUTE = 60; // second to minute.
    private final static int DIALOG_LP_POSITION_Y = 4; // position y.
    private final static float DIALOG_LP_ALPHA = 1.0f; // alpha 1.0.
    private final static int FORMAT_KEEP_TWO_LENGTH_AFTER_POINT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSharedPreferences = OtaApp.getContext().getSharedPreferences("update",
                        OtaApp.getContext().MODE_WORLD_READABLE |
                        OtaApp.getContext().MODE_WORLD_WRITEABLE | OtaApp.getContext().MODE_APPEND);
        editor = mSharedPreferences.edit();
        editor.putBoolean("default", true);
        editor.apply();
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        /*layoutParams.width = 480;
        layoutParams.height = 360;  */
        window.setAttributes(layoutParams);
        float density = getResources().getDisplayMetrics().density;
        System.out.println("midubi"+density);

        initView();
        initDate();
    }

    private void initDate() {
    }

    private void initView() {
        mExit2 = (ImageView) findViewById(R.id.exit);
        mShowCurrentVersion = (TextView) findViewById(R.id.showCurrentVersion);
        mShow_no_update = (RelativeLayout) findViewById(R.id.show_no_update);
        mShowNoNet1 = (TextView) findViewById(R.id.showNoNet1);
        downLoadnewVersion(getDownloadUrl(update),getDonwloadPath() + "/update.txt");
    }

    private void downLoadnewVersion(final String url, final String path) {
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
                mExit2.setClickable(false);
                if (updatefile.exists()) {
                    ArrayList<String> alUpDate = MyReader.getArraylist(updatefile);
                    if (!"1".equals(alUpDate.get(alUpDate.size() - 1))) {
                        Timer timer = new Timer();
                        mShow_no_update.setVisibility(View.VISIBLE);
                        mExit2.setClickable(true);
                        mExit2.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                updatefile.delete();
                            }
                        };
                        timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
                    } else {
                        mExit2.setClickable(false);
                        downLoadMd5(getDownloadUrl(md5),getDonwloadPath() + "/android-date.md5");
                    }
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                mShow_no_update.setVisibility(View.VISIBLE);
                mShowNoNet1.setText(R.string.errorNet);
                mExit2.setClickable(true);
                mExit2.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        updatefile.delete();
                    }
                };
                timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
            }
        });
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
                 downLoadIntrouce(getDownloadUrl(introduce),getDonwloadPath() + "/introduce.txt");
            }

            @Override
            public void onFailure(HttpException e, String s) {
                updatefile.delete();
                md5File.delete();
                mShow_no_update.setVisibility(View.VISIBLE);
                mShowNoNet1.setText(R.string.errorNet);
                    mExit2.setClickable(true);
                    mExit2.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                    }
                };
                timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
            }
        });
    }

    private void downLoadIntrouce(final String url, final String path) {
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
                 mShowHaveUpdate = (RelativeLayout) findViewById(R.id.showHaveUpdate);
                 mShowHaveUpdate.setVisibility(View.VISIBLE);
                 mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                 mSize = (TextView) findViewById(R.id.size);
                 mSpeed = (TextView) findViewById(R.id.speed);
                 mTime = (TextView) findViewById(R.id.time);
                 mInstall = (TextView) findViewById(R.id.install);
                downLoadOTA(getDownloadUrl(openthos),getDonwloadPath() + "/openthos-date.iso");
            }

            @Override
            public void onFailure(HttpException e, String s) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updatefile.delete();
                        md5File.delete();
                        introuceFile.delete();
                    }
                });
                mShow_no_update.setVisibility(View.VISIBLE);
                mShowNoNet1.setText(R.string.errorNet);
                mExit2.setClickable(true);
                mExit2.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                    }
                };
                timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
            }
        });
    }

    private void downLoadOTA(final String url, final String path) {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.download(url, path, true, false, new RequestCallBack<File>() {
            private Long startDate;
            private Long endDate;
            @Override
            public void onStart() {
                super.onStart();
                startDate = System.currentTimeMillis();
            }

            @Override
            public void onLoading(final long total, final long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
                endDate = System.currentTimeMillis();
                final NumberFormat nt=NumberFormat.getInstance();
                nt.setMinimumFractionDigits(FORMAT_KEEP_TWO_LENGTH_AFTER_POINT);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setDrawingCacheBackgroundColor(getResources()
                                                                .getColor(R.color.progressbar));
                mProgressBar.setMax((int) total);
                mProgressBar.setProgress((int) current);
                mSize.setText("(" + total / SWITCH_B_TO_MB_NUMBER + "M)"); // to MB
                Long time = (endDate - startDate) / TIMER_SWITCH_MILLISECOND_TO_SECOND;
                Double progress = (Double) ( current / (DOUBLE_SWITCH_B_TO_KB * time));
                if (current!=0) {
                   Long time1 = (total - current) / SWITCH_B_TO_KB; // to KB
                   mSpeed.setText(nt.format(progress) + "kb/s");
                   String format = nt.format(time1 / ((progress * SWITCH_SECOND_TO_MINUTE)));
                   String fenzhong = String.valueOf(format);
                   mTime.setText(fenzhong + "minutes");
               }
            }

            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (md5File.exists()) {
                    ArrayList<String> md5Al = MyReader.getArraylist(md5File);
                    System.out.println("md5Al" + md5Al.get(0));
                    System.out.println("Md5.getFileMD5(otaFile)" + Md5.getFileMD5(otaFile));

                    String systemdownloadMd5 = Md5.getFileMD5(otaFile);
                    boolean b = (md5Al.get(0)).equals(Md5.getFileMD5(otaFile));

                    if ((md5Al.get(0)).equals(Md5.getFileMD5(otaFile))) {
                        mProgressBar.setVisibility(View.GONE);
                        mTime.setVisibility(View.GONE);
                        mSpeed.setText(R.string.verifySuccess);
                        showMyDialog(MainActivity.this);
                        mExit2.setClickable(true);
                        mExit2.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                  finish();
                            }
                        });
                        mInstall.setVisibility(View.VISIBLE);
                        if (mInstall.VISIBLE==View.VISIBLE) {
                            mInstall.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showMyDialog(MainActivity.this);
                                }
                            });
                        }
                     } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                updatefile.delete();
                                md5File.delete();
                                introuceFile.delete();
                                otaFile.delete();
                            }
                        });
                        mExit2.setClickable(true);
                        mExit2.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                        mTime.setVisibility(View.GONE);
                        mSpeed.setText(R.string.zhengzaijiaoyanxitongwenjian);
                        mProgressBar.setVisibility(View.GONE);
                        mSpeed.setText(R.string.veriffailed);
                        Timer timer = new Timer();
                        //otaFile.delete();
                        //updatefile.delete();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                             }
                        };
                        timer.schedule(task, TIMER_CLOSING_PAGE_INTERVAL);
                    }
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                updatefile.delete();
                md5File.delete();
                introuceFile.delete();
                mExit2.setClickable(true);
                mExit2.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        });
    }

    private String getDownloadUrl(String url) {
        mSharedPreferences = OtaApp.getContext().getSharedPreferences("update",
                             Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE |
                             Context.MODE_APPEND);
         Boolean check = mSharedPreferences.getBoolean("default", false);
         if (check) {
             if (update==url) {
                 update = "http://192.168.0.180/openthos/update.txt";
                 return update;
             } else if (url == md5) {
                 md5 = "http://192.168.0.180/openthos/android-date.md5";
                 return md5;
             } else if(url == openthos) {
                 openthos="http://192.168.0.180/openthos/openthos-date.iso";
                 return openthos;
             } else if(url == introduce) {
                 introduce="http://192.168.0.180/openthos/introduce.txt";
                 return introduce;
             }
         } else {
             String url1 = mSharedPreferences.getString("url", "who");
             if (update==url) {
                 update = url1 + "/update2.txt";
                 return update;
             } else if (url == md5) {
                 md5 = url1 + "/android-date.md5";
                 return md5;
             } else if (url == openthos) {
                 openthos = url1 + "/openthos-date.iso";
                 return openthos;
             } else if (url == introduce) {
                 introduce = url1 + "/introduce.txt";
                 return introduce;
             }
         }

         return null;
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

     public static void  showMyDialog(Context context) {
         final Dialog dialog = new Dialog(context);
         dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
         dialog.setContentView(R.layout.dialog_layout);

         WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
         layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
         layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
         layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
         layoutParams.format = PixelFormat.RGBA_8888;
         layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
         layoutParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

         Window dialogWindow = dialog.getWindow();
         WindowManager.LayoutParams lp = dialogWindow.getAttributes();
         dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);
         if (lp == null) {
             lp= new WindowManager.LayoutParams(-1, DIALOG_LP_POSITION_Y);
             lp.windowAnimations = 0;
             lp.format = PixelFormat.TRANSLUCENT | WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW;
         }
         lp.alpha = DIALOG_LP_ALPHA;
         lp.gravity=Gravity.CENTER;
         TextView mDialogDismiss= (TextView) dialog.findViewById(R.id.dialogDismiss);
         mDialogDismiss.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 dialog.dismiss();
             }
         });
         TextView mDialogInstall= (TextView) dialog.findViewById(R.id.dialogInstall);
         mDialogInstall.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
             }
         });
         dialog.show();
    }

    public void getFinish() {
        finish();
    }
}
