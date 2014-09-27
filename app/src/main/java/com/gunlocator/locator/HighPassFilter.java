package com.gunlocator.locator;

/**
 * Created bdst skdstrdstldstuk on 9/27/14.
 */
public class HighPassFilter {

    static final double[] a = {1, -3.86969897027222, 5.61751902231049, -3.62562575742273, 0.877811503648146};
    static final double[] b = {0.936915953353349, -3.7476638134134, 5.62149572012009, -3.7476638134134, 0.936915953353349};
    static final int bufferSize = 512;


    public static double[] filtering(short[] src) {
        double[] dst = new double[bufferSize];

        // Filter first 9 samples
        dst[0] = b[0] * src[0];
        dst[1] = b[0] * src[1] + b[1] * src[0] - a[1] * dst[0];
        dst[2] = b[0] * src[2] + b[1] * src[1] + b[2] * src[0] - a[1] * dst[1] - a[2] * dst[0];
        dst[3] = b[0] * src[3] + b[1] * src[2] + b[2] * src[1] + b[3] * src[0] - a[1] * dst[2] - a[2] * dst[1] - a[3] * dst[0];

        for (int i = 4; i < bufferSize; i++) {
            dst[i] = b[0] * src[i] + b[1] * src[i - 1] + b[2] * src[i - 2] + b[3] * src[i - 3] + b[4] * src[i - 4] - a[1] * dst[i - 1] - a[2] * dst[i - 2] - a[3] * dst[i - 3] - a[4] * dst[i - 4];
        }


        return dst;
    }
}
