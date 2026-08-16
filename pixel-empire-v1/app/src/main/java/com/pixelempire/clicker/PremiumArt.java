package com.pixelempire.clicker;

import android.graphics.Bitmap;

/** Premium art facade. The embedded world bitmap remains dynamic-UI safe. */
public final class PremiumArt {
    private PremiumArt() {}
    public static Bitmap world() { return ReleaseArt.world(); }
}
