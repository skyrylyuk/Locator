package com.gunlocator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gunlocator.locator.Locator;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    short[] results = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private TextView txvLevel;
    //    private RenderScript renderScript;
    private Button btnStop;
    private int dataLength;
    private Allocation allocation;
    private Locator locator;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            txvLevel.setText("" + msg.arg1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txvLevel = (TextView) findViewById(R.id.txvLevel);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(v -> {
            Toast.makeText(this, "Lambda", Toast.LENGTH_LONG).show();
        });

//        renderScript = RenderScript.create(this);
    }

/*
    public void stopRecord(View view) {
        Log.w(TAG, "MainActivity.stopRecord");
        locator.interrupt();
    }
*/

    public void startRecord(View view) {
        Log.w(TAG, "MainActivity.startRecord");


        locator = new Locator();
        locator.setHandler(handler);
        locator.start();

/*
        new Thread() {

            @Override
            public void run() {
                super.run();

                audioReceiver = new AudioReceiver(Rates.MEDIUM);

                dataLength = audioReceiver.getBuffSize();
                allocation = Allocation.createSized(renderScript, Element.I16(renderScript), dataLength, Allocation.USAGE_SCRIPT);

                audioReceiver.start();
            }

        }.start();*/

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


}
