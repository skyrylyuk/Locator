package com.gunlocator;

import android.app.Application;
import android.content.pm.ApplicationInfo;

/**
 * Created by skyrylyuk on 9/26/14.
 */
public class LocatorApp extends Application {

    public static boolean idDebuggable;

    public LocatorApp() {
        idDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static boolean isDebug() {
        return idDebuggable;
    }
}
