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

    private double calculatePower(int row, int start, int finish) {
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

            Log.w(TAG, "LocatorHandler.handleMessage");

            if (isFirstRow) {
                isFirstRow = false;
                return;
            }


            int rowNumber = msg.arg1;
            long time = (long) msg.obj;
            Log.w(TAG, "time = " + (System.currentTimeMillis() - time));


            double left = 0, center = 0, right = 0;

            for (int offset = 1; offset < CHUNK_COUNT; offset++) {
                if (offset == 1) {
                    left = calculatePower(rowNumber, 0, CHUNK_SIZE);
                    center = calculatePower(rowNumber, CHUNK_SIZE * 2, CHUNK_SIZE * 3);
                    right = calculatePower(rowNumber, CHUNK_SIZE * 4, CHUNK_SIZE * 5);
                } else {
                    if (rowNumber != BUFF_LAST_ROW) {
                        left = calculatePower(rowNumber, CHUNK_SIZE * offset, CHUNK_SIZE * (offset + 1));
                        if (offset <= 3) {
                            center = calculatePower(rowNumber, CHUNK_SIZE * (offset + 1), CHUNK_SIZE * (offset + 2));
                        } else {
                            center = calculatePower(rowNumber + 1, CHUNK_SIZE * (offset - 4), CHUNK_SIZE * (offset - 3));
                        }
                        right = calculatePower(rowNumber + 1, CHUNK_SIZE * (offset - 2), CHUNK_SIZE * (offset -1));
                    } else {
                        left = calculatePower(rowNumber, CHUNK_SIZE * offset, CHUNK_SIZE * (offset + 1));
                        if (offset <= 3) {
                            center = calculatePower(rowNumber, CHUNK_SIZE * (offset + 1), CHUNK_SIZE * (offset + 2));
                        } else {
                            center = calculatePower(0, CHUNK_SIZE * (offset - 4), CHUNK_SIZE * (offset - 3));
                        }
                        right = calculatePower(0, CHUNK_SIZE * (offset - 2), CHUNK_SIZE * (offset -1));
                    }
                }

            }

            double medians = (left + right) / 2;

            double delta = center - medians;

            if (delta > 1000000) {
                handler.sendMessage(handler.obtainMessage(NOTIFICATION_UI, (int) (delta / 1000000), 0));
//                Log.w(TAG, "Shut detect [ delta " + delta + "]========================================================================");
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

                if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    System.err.println("read() returned ERROR_INVALID_OPERATION");
                    return;
                }

                if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                    System.err.println("read() returned ERROR_BAD_VALUE");
                    return;
                }

                // send Message count
                locatorHandler.sendMessage(locatorHandler.obtainMessage(NOTIFICATION_LOCATOR, count, 0, System.currentTimeMillis()));
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

