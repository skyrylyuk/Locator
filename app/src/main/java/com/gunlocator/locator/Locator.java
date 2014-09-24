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

    private static final int BUFF_COUNT = 32;
    private static final int CHUNK_SIZE = 512;
    private static final int BUFFER_SIZE = CHUNK_SIZE * 5;

    private static short[][] buffers = new short[BUFF_COUNT][BUFFER_SIZE];

    private Handler handler;
    private AudioReceiverThread audioReceiverThread;

    public Locator() {

    }

    @Override
    public void run() {
        super.run();

        audioReceiverThread = new AudioReceiverThread();
        audioReceiverThread.start();

        Looper.prepare();
        handler = new LocatorHandler();
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

    private class LocatorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

                int rowNumber = msg.what;


                double left = calculatePower(rowNumber, 0, CHUNK_SIZE);
                double center = calculatePower(rowNumber, CHUNK_SIZE * 2, CHUNK_SIZE * 3);
                double right = calculatePower(rowNumber, CHUNK_SIZE * 4, CHUNK_SIZE * 5);

                double medians = (left + right) / 2;
//                Log.w(TAG, "left = " + left + " center = " + center + " right = " + right + " medians = " + medians);
//                Log.w(TAG, " center = " + center + " medians = " + medians);

                double delta = center - medians;

                        if (delta > 100000) {
                    Log.w(TAG, "Shut detect [ delta " + delta + "]========================================================================");
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
                handler.sendMessage(handler.obtainMessage(count));
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

