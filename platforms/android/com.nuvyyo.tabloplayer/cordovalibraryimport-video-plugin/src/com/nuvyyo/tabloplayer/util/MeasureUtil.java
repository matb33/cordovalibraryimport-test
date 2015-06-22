package com.nuvyyo.tabloplayer.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by mike on 15/06/15.
 */
public class MeasureUtil {
    public static int dipsToPixels(DisplayMetrics metrics, int dips) {
        float logicalDensity = metrics.density;
        return (int) Math.ceil(dips * logicalDensity);
    }

    public static int dipsToPixels(WindowManager windowManager, int dips) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return dipsToPixels(metrics, dips);
    }
}
