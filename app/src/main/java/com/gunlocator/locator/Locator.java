package com.gunlocator.locator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Locator extends Thread {

    public static final String TAG = Locator.class.getSimpleName();
    public static final int NOTIFICATION_LOCATOR = 555;
    public static final int NOTIFICATION_UI = 556;
    public static final int DELTA = 180;
    private static final int BUFF_COUNT = 32;
    private static final int BUFF_LAST_ROW = BUFF_COUNT - 1;
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_COUNT = 5;
    private static final int BUFFER_SIZE = CHUNK_SIZE * CHUNK_COUNT;
    private static short[][] buffers = new short[BUFF_COUNT][BUFFER_SIZE];
    private SimpleDateFormat format = new SimpleDateFormat("HH_mm");
    private boolean isDebug = false; //LocatorApp.getInstance().isDebug();
    private Handler handler;

    private Handler locatorHandler;

    private AudioReceiverThread audioReceiverThread;

    private boolean isFirstRow = true;
    private FileWriter writer;

    public Locator() {
        if (isDebug) {
            getFileWriter();
        }
    }

    @Override
    public void run() {
        super.run();

        audioReceiverThread = new AudioReceiverThread();
        audioReceiverThread.start();

        Looper.prepare();
        locatorHandler = new LocatorHandler();
        Looper.loop();
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

    @Override
    public void interrupt() {
        audioReceiverThread.interrupt();

        try {
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "java.io.IOException ", e);
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
                char c = (char) i;
                writer.append(c);
                writer.append('\n');
            }

            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "java.io.IOException ", e);
        }
    }

    private class LocatorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (isFirstRow) {
                Log.w(TAG, "isFirstRow");
                isFirstRow = false;
                return;
            }

            int rowNumber = msg.arg1;

            if (isDebug) {
                writeToCSV(buffers[rowNumber]);
            }

            long time = (long) msg.obj;

            int nextRowNumber = rowNumber;
            rowNumber = rowNumber == 0 ? BUFF_LAST_ROW : rowNumber - 1;

            for (int offset = 0; offset < CHUNK_COUNT; offset++) {

                double left = calculatePower(rowNumber, offset);

                double center = offset < 3 ? calculatePower(rowNumber, offset + 2) : calculatePower(nextRowNumber, offset - 3);

                double right = offset == 0 ? calculatePower(rowNumber, 4) : calculatePower(nextRowNumber, offset - 1);

                double delta = (center / ((left + right) / 2));


                if (delta > DELTA) {
                    Log.w(TAG, "delta = " + delta);
                    Log.w(TAG, "left = " + left);
                    Log.w(TAG, "center = " + center);
                    Log.w(TAG, "right = " + right);
                    Log.w(TAG, "offset = " + offset);
                    Log.w(TAG, "rowNumber = " + rowNumber);
//                    handler.sendMessage(handler.obtainMessage(NOTIFICATION_UI, (int) (delta / 1000000), 0));

//                    int v = (int) (delta);

//                    Log.w(TAG, "Shut detect at " + formatter.format(new Date(time)) + " - delta " + v);
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
                long timeStamp = System.currentTimeMillis();

                int samplesRead = record.read(buffers[count], 0, BUFFER_SIZE);

                if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    System.err.println("read() returned ERROR_INVALID_OPERATION");
                    return;
                }

                if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                    System.err.println("read() returned ERROR_BAD_VALUE");
                    return;
                }

                // send Message count
                locatorHandler.sendMessage(locatorHandler.obtainMessage(NOTIFICATION_LOCATOR, count, 0, timeStamp));
                count = (count + 1) % BUFF_COUNT;
            }
            try {
                try {
                    record.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            } finally {
                // освобождаем ресурсы
                record.release();
                record = null;
            }

        }

    }
}

