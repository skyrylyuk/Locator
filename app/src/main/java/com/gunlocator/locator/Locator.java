package com.gunlocator.locator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class Locator extends Thread {

    public static final String TAG = Locator.class.getSimpleName();

    public static final int NOTIFICATION_LOCATOR = 555;
    public static final int NOTIFICATION_UI = 556;
    public static final int DELTA = 15000;
    private static final int BUFF_COUNT = 32;
    private static final int BUFF_LAST_ROW = BUFF_COUNT - 1;
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_COUNT = 5;
    private static final int BUFFER_SIZE = CHUNK_SIZE * CHUNK_COUNT;
    private static short[][] buffers = new short[BUFF_COUNT][BUFFER_SIZE];

    private Handler handler;

    private Handler locatorHandler;

    private AudioReceiverThread audioReceiverThread;

    private boolean isFirstRow = true;

    public Locator() {

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

        super.interrupt();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private class LocatorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (isFirstRow) {
                isFirstRow = false;
                return;
            }

            int rowNumber = msg.arg1;
            long time = (long) msg.obj;
//            Log.w(TAG, "time = " + (System.nanoTime() - time));

            int nextRowNumber = rowNumber == BUFF_LAST_ROW ? 0 : rowNumber + 1;

            for (int offset = 0; offset < CHUNK_COUNT; offset++) {

                double left = calculatePower(rowNumber, offset);

                double center = offset < 3 ? calculatePower(rowNumber, offset + 2) : calculatePower(nextRowNumber, offset - 3);

                double right = offset == 0 ? calculatePower(rowNumber, 4) : calculatePower(nextRowNumber, offset - 1);

                int delta = (int) (Math.abs(center - (left + right) / 2) / 100000);

                if (delta > DELTA) {
//                    handler.sendMessage(handler.obtainMessage(NOTIFICATION_UI, (int) (delta / 1000000), 0));

                    int v = (int) (delta);

                    Log.w(TAG, "Shut detect [ delta " + v);
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

