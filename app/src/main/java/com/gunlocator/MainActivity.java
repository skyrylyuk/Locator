package com.gunlocator;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gunlocator.locator.AudioReceiver;
import com.gunlocator.locator.Locator;
import com.gunlocator.locator.Rates;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private AudioReceiver audioReceiver;
    private RenderScript renderScript;

    private int dataLength;
    private Allocation allocation;

    short[] results = {1,1,1,1,1};
    private Locator locator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        renderScript = RenderScript.create(this);
    }

    public void startRecord(View view) {
        Log.w(TAG, "MainActivity.startRecord");


        locator = new Locator();
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

    public void stopRecord(View view) {
        Log.w(TAG, "MainActivity.stopRecord");
        locator.interrupt();
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
