package com.gunlocator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gunlocator.locator.Locator;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private TextView txvLevel;
    private RenderScript renderScript;
    private ToggleButton tbtLocator;
    private int dataLength;
    private Allocation allocation;
    private Locator locator;

    private ScriptC_BandPassFilter bandPassFilter;

    private short[] sources = {3, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
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
        tbtLocator = (ToggleButton) findViewById(R.id.tbtLocator);
        tbtLocator.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        Log.w(TAG, "MainActivity.start Record");
                        locator = new Locator();
                        locator.setHandler(handler);
                        locator.start();
                    } else {
                        Log.w(TAG, "MainActivity.stop Record");
                        if (locator != null) {
                            locator.interrupt();
                        }
                    }
                }
        );

        Button buttonRS = (Button) findViewById(R.id.buttonRS);
        buttonRS.setOnClickListener(v -> {
            Allocation data = Allocation.createSized(renderScript, Element.I16(renderScript), sources.length, Allocation.USAGE_SCRIPT);
            Allocation out = Allocation.createSized(renderScript, Element.I16(renderScript), sources.length, Allocation.USAGE_SCRIPT);
            data.copy1DRangeFrom(0, sources.length, sources);

            bandPassFilter.bind_data(data);
            bandPassFilter.bind_out(out);
            bandPassFilter.forEach_root(data, out);

            short[] outs = new short[sources.length];
            out.copyTo(outs);

            Log.w(TAG, "Arrays.toString(sources) = " + Arrays.toString(sources));
            Log.w(TAG, "Arrays.toString(outs) =    " + Arrays.toString(outs));
        });

        renderScript = RenderScript.create(this);
        bandPassFilter = new ScriptC_BandPassFilter(renderScript);
    }

    public void startRecord(View view) {
        Log.w(TAG, "MainActivity.startRecord");


        locator = new Locator();
        locator.setHandler(handler);
        locator.start();
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
