package com.openthos.utis;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OtaNetUtils {
    public static boolean isConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mConnectivityManager != null) {
                NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
                if (mNetworkInfo != null) {
                    return mNetworkInfo.isAvailable();
                }
            }
        }
        return false;
    }

    public static String getNetStr(Context context, String path) {
        if (isConnected(context)) {
            InputStream is = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(path);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();
                int code = conn.getResponseCode();

                if (code == HttpURLConnection.HTTP_OK) {
                    is = conn.getInputStream();
                    int len = -1;
                    StringBuffer buffer = new StringBuffer();
                    byte[] bytes = new byte[1024];
                    while ((len = is.read(bytes)) != -1) {
                        buffer.append(new String(bytes, 0, len));
                    }
                    return new String(buffer.toString().getBytes("UTF-8"));
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } else {
            return null;
        }
    }
}