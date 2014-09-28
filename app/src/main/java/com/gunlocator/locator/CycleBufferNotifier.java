package com.gunlocator.locator;

/**
 * Created by skyrylyuk on 9/28/14.
 */
public class CycleBufferNotifier {

    private static CycleBufferNotifier instance;
    int row;
    long time;

    public static CycleBufferNotifier getInstance(int rowValue, long timestamp) {
        if (instance == null) {
            instance = new CycleBufferNotifier();
        }
        instance.row = rowValue;
        instance.time = timestamp;
        return instance;
    }
}
