package com.gunlocator.locator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gunlocator.LocatorApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;

public class Locator extends Thread {
    public static final String TAG = Locator.class.getSimpleName();

    public static final String CFAR = "CFAR";
    public static final String BALANCE = "BALANCE";
    public static final String DELAY = "DELAY";

    public static final int NOTIFICATION_UI = 556;
    public static final int DELTA = 18;
    private static final int BUFF_COUNT = 32;
    private static final int BUFF_LAST_ROW = BUFF_COUNT - 1;
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_COUNT = 5;
    private static final int BUFFER_SIZE = CHUNK_SIZE * CHUNK_COUNT;
    private static short[][] buffers = new short[BUFF_COUNT][BUFFER_SIZE];
    private SimpleDateFormat format = new SimpleDateFormat("HH_mm");
    //    public SimpleDateFormat formatWide = new SimpleDateFormat("HH_mm_ss.SSS");
    private boolean isDebug = false; //LocatorApp.getInstance().isDebug();
    private Handler handler;

    private SynchronousQueue<CycleBufferNotifier> queue = new SynchronousQueue<>();

    private AudioReceiverThread audioReceiverThread;

//    LocatorApp.

    private boolean isFirstRow = true;
    private FileWriter writer;

    public Locator() {
        if (isDebug) {
            getFileWriter();
        }

        audioReceiverThread = new AudioReceiverThread();
    }

    public Locator(boolean isDebug) {
        if (isDebug) {
            getFileWriter();
        }

        audioReceiverThread = new AudioReceiverThreadMock();
    }

    @Override
    public void run() {
        super.run();

        audioReceiverThread.start();

        while (!isInterrupted()) {

            try {
                handleBuffer(queue.take());
            } catch (InterruptedException e) {
                interrupt();
            }

        }

    }

    private double calculatePower(int row, int chunkNumber) {

        int start = chunkNumber * CHUNK_SIZE;
        int finish = start + CHUNK_SIZE;

        double result = 0;

        short[] buffer = buffers[row];

        for (int i = start; i < finish; i++) {
            result += Math.pow(buffer[i], 2);
        }

        return result;
    }

    private double calculatePower(double[] buffer) {
        double result = 0;

        for (int i = 0; i < CHUNK_SIZE; i++) {
            result += Math.pow(buffer[i], 2);
        }

        return result;
    }

    @Override
    public void interrupt() {
        audioReceiverThread.interrupt();

        if (isDebug) {
            try {
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "java.io.IOException ", e);
            }
        }

        super.interrupt();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private void getFileWriter() {
        String storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + "locator" + format.format(new Date()) + ".csv";
        try {
            writer = new FileWriter(storage_path);
        } catch (IOException e) {
            Log.e("phase6", "java.io.IOException ", e);
        }
    }

    private void writeToCSV(short[] buffer) {
        try {
            for (short i : buffer) {
                writer.append(Short.toString(i));
                writer.append('\n');
            }

            writer.flush();
        } catch (IOException e) {
//            Log.e(TAG, "java.io.IOException ", e);
        }
    }


    private void handleBuffer(CycleBufferNotifier msg) {
        long time = msg.time;
        long delay = System.nanoTime() - time;

        if (isFirstRow) {
            isFirstRow = false;
            return;
        }
        int rowNumber = msg.row;

        if (isDebug) {
            writeToCSV(buffers[rowNumber]);
        }


        int nextRowNumber = rowNumber;
        rowNumber = rowNumber == 0 ? BUFF_LAST_ROW : rowNumber - 1;

        for (int offset = 0; offset < CHUNK_COUNT; offset++) {

            double left = calculatePower(rowNumber, offset);

            double center = offset < 3 ? calculatePower(rowNumber, offset + 2) : calculatePower(nextRowNumber, offset - 3);

            double right = offset == 0 ? calculatePower(rowNumber, 4) : calculatePower(nextRowNumber, offset - 1);

            double cfar = (center / ((left + right) / 2));


            if (cfar > DELTA) {
/*
                    Log.w(TAG, "cfar = " + cfar);
                    Log.w(TAG, "left = " + left);
                    Log.w(TAG, "center = " + center);
                    Log.w(TAG, "right = " + right);
                    Log.w(TAG, "offset = " + offset);
                    Log.w(TAG, "rowNumber = " + rowNumber);
*/

                int start;
                int finish;
                if (offset < 3) {
                    start = (offset + 2) * CHUNK_SIZE;
                    finish = start + CHUNK_SIZE;
                } else {
                    start = (offset - 3) * CHUNK_SIZE;
                    finish = start + CHUNK_SIZE;
                }
                double lowEnergy = calculatePower(BandPassFilter.filtering(Arrays.copyOfRange(buffers[rowNumber], start, finish)));
                double highEnergy = calculatePower(HighPassFilter.filtering(Arrays.copyOfRange(buffers[rowNumber], start, finish)));

                double balanse = lowEnergy / highEnergy;

                if (balanse > 1) {
//                        Log.w(TAG, "DETECT " + balanse + " at " + formatWide.format(new Date(time)));

                    Message message = handler.obtainMessage(NOTIFICATION_UI);
                    Bundle data = new Bundle();
                    data.putDouble(CFAR, cfar);
                    data.putDouble(BALANCE, balanse);

                    data.putLong(DELAY, delay);
                    message.setData(data);

                    handler.sendMessage(message);
                } else {
                    Log.w(TAG, "delay = " + (delay));
                }
            }
        }
    }

    private class AudioReceiverThread extends Thread {

        private final int RATE = Rates.MEDIUM.value;
        private final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
        private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;


        @Override
        public void run() {
            super.run();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RATE, CHANNEL, ENCODING,
                    BUFFER_SIZE * 2);

            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                System.err.println("getState() != STATE_INITIALIZED");
                return;
            }

            try {
                record.startRecording();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }

            int count = 0;

            while (!isInterrupted()) {

                int samplesRead = record.read(buffers[count], 0, BUFFER_SIZE);
                long timeStamp = System.nanoTime();

                if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    System.err.println("read() returned ERROR_INVALID_OPERATION");
                    return;
                }

                if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                    System.err.println("read() returned ERROR_BAD_VALUE");
                    return;
                }

                // send Message count
                try {
                    queue.put(CycleBufferNotifier.getInstance(count, timeStamp));
                } catch (InterruptedException e) {
                    Locator.this.interrupt();
                }
                count = (count + 1) % BUFF_COUNT;
            }
            try {
                try {
                    record.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            } finally {
                record.release();
                record = null;
            }

        }
    }

    private class AudioReceiverThreadMock extends AudioReceiverThread {

        @Override
        public void run() {
//            super.run();

            try {
                InputStream stream = LocatorApp.getInstance().getAssets().open("shock.1.csv");

                BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String str = null;

                in.readLine();

                for (int i = 0; i < BUFFER_SIZE && (str = in.readLine()) != null; i++) {
                    short v = Short.parseShort(str);
                    buffers[0][i] = v;
                }

                in.close();

                try {
                    queue.put(CycleBufferNotifier.getInstance(0, System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    Locator.this.interrupt();
                }

                System.arraycopy(buffers[0], 0, buffers[1], 0, BUFFER_SIZE);

                try {
                    queue.put(CycleBufferNotifier.getInstance(1, System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    Locator.this.interrupt();
                }


            } catch (IOException e) {
                Log.e(TAG, "java.io.IOException ", e);
            }

        }
    }
}

