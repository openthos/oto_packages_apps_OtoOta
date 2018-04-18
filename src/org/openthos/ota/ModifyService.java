package org.openthos.ota;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.openthos.ModifyPath;

/**
 * Created by root on 12/22/17.
 */

public class ModifyService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ModifyPath.Stub() {
            @Override
            public void changePath(String aString) throws RemoteException {
                getSharedPreferences("OTA", Context.MODE_PRIVATE).edit().putString("DownloadUrl", aString).commit();
            }
        };
    }
}
