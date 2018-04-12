package com.openthos.ota;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BootConpleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(
                "systemUpgradeInfo", Context.MODE_PRIVATE);
        if (sp.getBoolean("isUpgrade", false)) {
            final String[] oldAppInfos = context.getResources()
                    .getStringArray(R.array.deprecated_system_app_list);
            final List<SystemAppInfo> systemAppInfoList = new ArrayList<SystemAppInfo>();
            PackageManager pm = context.getPackageManager();
            String packageName = null;
            String appName = null;
            Drawable appIcon = null;
            PackageInfo info = null;
            for (int i = 0; i < oldAppInfos.length; i++) {
                try {
                    packageName = oldAppInfos[i];
                    appName = pm.getApplicationLabel(pm.getApplicationInfo(
                            oldAppInfos[i], PackageManager.GET_META_DATA)).toString();
                    appIcon = pm.getApplicationIcon(packageName);
                    info = pm.getPackageInfo(packageName, 0);
                    if (checkAppIsExists(context, packageName) && info != null
                            && ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) >= 1)) {
                        systemAppInfoList.add(
                                new SystemAppInfo(packageName, appName, appIcon, false));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (systemAppInfoList.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        context, AlertDialog.THEME_HOLO_LIGHT);
                builder.setTitle(context.getResources()
                        .getString(R.string.multichoice_dialog_title));
                ListView multiChoiceLv = new ListView(context);
                final MultiChoiceAdapter adapter =
                        new MultiChoiceAdapter(systemAppInfoList, context);
                multiChoiceLv.setAdapter(new MultiChoiceAdapter(systemAppInfoList, context));
                multiChoiceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> p, View view, int position, long id) {
                        systemAppInfoList.get(position).isChecked =
                                !systemAppInfoList.get(position).isChecked;
                        LinearLayout ll = (LinearLayout) view;
                        CheckBox cb = (CheckBox) ll.getChildAt(3);
                        cb.setChecked(!cb.isChecked());
                    }
                });
                builder.setView(multiChoiceLv);
                builder.setPositiveButton(
                        context.getResources().getString(R.string.multichoice_dialog_confirm),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                String packageName = null;
                                SystemAppInfo systemAppInfo = null;
                                CheckBox checkBox = null;
                                for (int i = 0; i < systemAppInfoList.size(); i++) {
                                    systemAppInfo = systemAppInfoList.get(i);
                                    if (systemAppInfoList.get(i).isChecked) {
                                        packageName = systemAppInfo.packageName;
                                        BufferedReader in = null;
                                        try {
                                            Process pro = Runtime.getRuntime().exec(
                                                    new String[]{"su", "-c",
                                                            "pm uninstall --user 0 " + packageName});
                                            in = new BufferedReader(
                                                    new InputStreamReader(pro.getInputStream()));
                                            String line;
                                            while ((line = in.readLine()) != null) {
                                            }
                                            pro.waitFor();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } finally {
                                            if (in != null) {
                                                try {
                                                    in.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });

                builder.setNegativeButton(
                        context.getResources().getString(R.string.multichoice_dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
                sp.edit().putBoolean("isUpgrade", false).commit();
            }
        }
        OtaApp.startAutoUpdate(context,
                OtaApp.getSharedPreferences(context).getBoolean(OtaApp.TIPS, false));
    }

    private boolean checkAppIsExists(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private class SystemAppInfo {
        String packageName;
        String appName;
        Drawable appIcon;
        boolean isChecked;

        public SystemAppInfo(
                String packageName, String appName, Drawable appIcon, boolean isChecked) {
            this.packageName = packageName;
            this.appName = appName;
            this.appIcon = appIcon;
            this.isChecked = isChecked;
        }
    }

    private class MultiChoiceAdapter extends BaseAdapter {
        List<SystemAppInfo> data;
        Context context;

        public MultiChoiceAdapter(List<SystemAppInfo> list, Context context) {
            data = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public LinearLayout getItem(int position) {
            return (LinearLayout) getView(position, null, null);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout ll = null;
            TextView tv = null;
            ImageView iv = null;
            View spaceView = null;
            CheckBox cb = null;
            LinearLayout.LayoutParams params = null;
            if (convertView == null) {
                ll = new LinearLayout(context);
                ll.setGravity(LinearLayout.HORIZONTAL);
                iv = new ImageView(context);
                params = new LinearLayout.LayoutParams(48, 48, 0);
                iv.setLayoutParams(params);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                params.topMargin = 5;
                params.bottomMargin = 5;
                params.leftMargin = 10;
                params.rightMargin = 10;
                iv.setLayoutParams(params);
                tv = new TextView(context);
                tv.setTextColor(Color.BLACK);
                tv.setLayoutParams(params);
                cb = new CheckBox(context);
                cb.setBackgroundColor(Color.BLACK);
                cb.setLayoutParams(params);
                cb.setFocusable(false);
                cb.setClickable(false);
                cb.setEnabled(false);
                spaceView = new View(context);
                params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                spaceView.setLayoutParams(params);
            } else {
                ll = (LinearLayout) convertView;
                iv = (ImageView) ll.getChildAt(0);
                tv = (TextView) ll.getChildAt(1);
                spaceView = ll.getChildAt(2);
                cb = (CheckBox) ll.getChildAt(3);
            }
            tv.setText(data.get(position).appName);
            iv.setImageDrawable(data.get(position).appIcon);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            ll.addView(iv);
            ll.addView(tv);
            ll.addView(spaceView);
            ll.addView(cb);
            convertView = ll;
            return convertView;
        }
    }
}
