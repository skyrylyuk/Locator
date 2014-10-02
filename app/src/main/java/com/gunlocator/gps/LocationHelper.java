package com.gunlocator.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

/**
 * Created by skyrylyuk on 10/2/14.
 */
public class LocationHelper {
    public static final String TAG = LocationHelper.class.getSimpleName();
    private final LocationManager manager;
    private final SimpleLocationListener locationListener;
    private OnGPSTimeChange onGPSTimeChange;

    public LocationHelper(Context context) {
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new SimpleLocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (onGPSTimeChange != null) {
                    onGPSTimeChange.onChange(location.getTime());
                }
            }
        };

    }

    public void start() {
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, locationListener);
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
