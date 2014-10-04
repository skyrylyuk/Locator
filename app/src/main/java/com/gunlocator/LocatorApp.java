package com.gunlocator;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Created by skyrylyuk on 9/26/14.
 */
public class LocatorApp extends Application {

    public static final String ttt = "dfdfd";
    public static LocatorApp instance;
    private static Context applicationContext;

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
