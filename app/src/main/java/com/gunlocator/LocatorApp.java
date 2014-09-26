package com.gunlocator;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Created by skyrylyuk on 9/26/14.
 */
public class LocatorApp extends Application {

    private static Context applicationContext;

    private static LocatorApp instance;

    public static LocatorApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public boolean isDebug() {
        return (instance.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
