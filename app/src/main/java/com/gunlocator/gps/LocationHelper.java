package com.gunlocator.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Created by skyrylyuk on 10/2/14.
 */
public class LocationHelper {
    public static final String TAG = LocationHelper.class.getSimpleName();

    public static volatile long gpsTimeDelta = 0;

    private final LocationManager manager;
    private final SimpleLocationListener locationListener;
    private OnGPSTimeChange onGPSTimeChange;

    public LocationHelper(Context context) {
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new SimpleLocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (onGPSTimeChange != null) {
                    onGPSTimeChange.onChange(gpsTimeDelta = System.currentTimeMillis() - location.getTime());
                }
            }
        };
    }

    public void start() {
        long minTime = TimeUnit.SECONDS.toMillis(5);
        Log.w(TAG, "minTime = " + minTime);

        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 5, locationListener);
//        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TimeUnit.MINUTES.toMillis(5), 5, locationListener);
    }

    public void stop() {
        manager.removeUpdates(locationListener);
    }

    public void setOnGPSTimeChange(OnGPSTimeChange onGPSTimeChange) {
        this.onGPSTimeChange = onGPSTimeChange;
    }

    public interface OnGPSTimeChange {
        public void onChange(long gpsTime);
    }
}
