package com.gunlocator.locator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by skyrylyuk on 9/23/14.
 */
public class AudioReceiver extends Thread {

    public static final String TAG = AudioReceiver.class.getSimpleName();

    //    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final int BUFF_COUNT = 32;
    private static final int CHUNK_SIZE = 512;
    private static final int BUFFER_SIZE = CHUNK_SIZE * 5;

    private Handler handler;

    private static int sampleRateInHz;

    private int buffSize;


    public AudioReceiver(Rates rate) {
        sampleRateInHz = rate.value;
        buffSize = getBuffer(rate);

        Looper.prepare();

        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                int rowId = msg.what;
/*

            short cs = 0;
            for (int i = 0; i < dataLength; i++) {
                cs += samples[i];
            }

            System.arraycopy(results, 0, results, 1, 4);
            results[0] = cs;
            Log.w(TAG, "Arrays.toString(samples) = " + Arrays.toString(results));
*/

            }
        };

        Log.w(TAG, "buffSize = " + buffSize);

        Log.w(TAG, "BUFFER_SIZE = " + BUFFER_SIZE);
    }

    @Override
    public void run() {
        super.run();

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        short[][] buffers = new short[BUFF_COUNT][buffSize >> 1];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                CHANNEL, ENCODING,
                BUFFER_SIZE);

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
            int samplesRead = record.read(buffers[count], 0, buffers[count].length);

            if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                System.err.println("read() returned ERROR_INVALID_OPERATION");
                return;
            }

            if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                System.err.println("read() returned ERROR_BAD_VALUE");
                return;
            }

            sendMsg(count);
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

    private void sendMsg(int rowNumber) {
        handler.sendMessage(handler.obtainMessage(rowNumber));
    }

    private int getBuffer(Rates rate) {

        int size = AudioRecord.getMinBufferSize(rate.value, CHANNEL, ENCODING);

        if (size == AudioRecord.ERROR) {
            System.err.println("getMinBufferSize returned ERROR");
            return -1;
        }

        if (size == AudioRecord.ERROR_BAD_VALUE) {
            System.err.println("getMinBufferSize returned ERROR_BAD_VALUE");
            return -1;
        }

        return size;
    }

    public int getBuffSize() {
        return buffSize >> 1;
    }


}
