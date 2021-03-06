package com.gunlocator;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gunlocator.gps.LocationHelper;
import com.gunlocator.locator.Locator;
import com.gunlocator.network.QuoteOfTheMomentClient;
import com.gunlocator.network.QuoteOfTheMomentServer;

public class MainActivity extends ActionBarActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int LOCAL_MESSAGE = 0;
    public static final int REMOTE_MESSAGE = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOCAL_MESSAGE: {
                    Bundle data = msg.getData();
                    double cfar = data.getDouble(Locator.CFAR);
                    txvLevel.setText(Double.toString(cfar).substring(0, 6));
                    double balance = data.getDouble(Locator.BALANCE);
                    txvBalance.setText(Double.toString(balance).substring(0, 6));
                    long delay = data.getLong(Locator.DELAY);
//            txvDelay.setText(Long.toString((System.nanoTime() - delay) / 1000000));
                    txvDelay.setText(Long.toString(delay / 1000000));
                    break;
                }
                case REMOTE_MESSAGE: {
                    txvBalance.setText((String) msg.obj);
                }
            }
        }
    };
    private TextView txvLevel;
    private TextView txvBalance;
    private TextView txvDelay;
    private ToggleButton tbtLocator;
    private Button btnGetTime;
    private Locator locator;
    private LocationHelper locationHelper;
    private QuoteOfTheMomentClient quoteOfTheMomentClient;
    private QuoteOfTheMomentServer quoteOfTheMomentServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lockMulticast();
        setContentView(R.layout.activity_main);

        txvLevel = (TextView) findViewById(R.id.txvLevel);
        txvBalance = (TextView) findViewById(R.id.txvBalance);
        txvDelay = (TextView) findViewById(R.id.txvDelay);

        tbtLocator = (ToggleButton) findViewById(R.id.tbtLocator);
        tbtLocator.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        Log.w(TAG, "MainActivity.start Record");
                        locator = new Locator();
                        locator.setHandler(handler);
                        locator.start();

                        locationHelper.start();
                    } else {
                        Log.w(TAG, "MainActivity.stop Record");
                        if (locator != null) {
                            locator.interrupt();
                        }
                    }
                }
        );

        btnGetTime = (Button) findViewById(R.id.btnGetTime);
        btnGetTime.setOnClickListener(v -> {

            quoteOfTheMomentServer = QuoteOfTheMomentServer.getInstance(handler);

            quoteOfTheMomentClient = new QuoteOfTheMomentClient(1234);
            quoteOfTheMomentClient.start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationHelper = new LocationHelper(this);
        locationHelper.setOnGPSTimeChange(gpsTime -> {
            Log.w(TAG, "gpsTime = " + gpsTime);
        });

        locationHelper.start();

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (locationHelper != null) {
            locationHelper.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void lockMulticast() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("Log_Tag");
            lock.acquire();
        }
    }
}
