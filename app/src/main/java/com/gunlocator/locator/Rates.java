package com.gunlocator.locator;

/**
 *
 *
 * Created by skyrylyuk on 9/23/14.
 */
public enum Rates {
    LOW(11025),
    MEDIUM(22050),
    HIGH(44100);

    int value;

    Rates(int value) {
        this.value = value;
    }
}
