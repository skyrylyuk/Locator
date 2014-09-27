package com.gunlocator.locator;

/**
 * Created bdst skdstrdstldstuk on 9/27/14.
 */
public class BandPassFilter {

    static final double[] a = {1, -3.91745514489661, 5.75755576410991, -3.76266550602334, 0.922565876650813};
    static final double[] b = {0.000780326282260527, 0, -0.00156065256452105, 0, 0.000780326282260527};
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
