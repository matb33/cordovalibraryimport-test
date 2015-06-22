package com.nuvyyo.tabloplayer.bif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

/**
 * Created by mike on 11/06/15.
 */
public class SnapshotZoomTransformation implements Transformation {
    private static final String KEY_BASE = "tablo-snapshot-zoom";

    private SnapshotGrid mGrid;
    private String mUrl;
    private int mX;
    private int mY;

    /**
     *
     * @param grid {@link SnapshotGrid} which describes the snapshots.
     * @param url The url to the particular grid we're operating against.
     * @param x The x-index of the frame to zoom.
     * @param y The y-index of the frame to zoom.
     */
    public SnapshotZoomTransformation(SnapshotGrid grid, String url, int x, int y) {
        mGrid = grid;
        mUrl = url;
        mX = x;
        mY = y;
    }

    @Override
    public Bitmap transform(final Bitmap source) {
        final int outputWidth   = mGrid.getWidth();
        final int outputHeight  = mGrid.getHeight();

        final int sourceOffsetX = mX * -mGrid.getWidth();
        final int sourceOffsetY = mY * -mGrid.getHeight();

        Bitmap output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Draw source to output
        Paint copyPaint = new Paint();
        copyPaint.setAntiAlias(true);
        canvas.drawBitmap(source, sourceOffsetX, sourceOffsetY, copyPaint);

        // Recycle input source.
        if( source != output )
            source.recycle();

        return output;
    }

    @Override
    public String key() {
        return KEY_BASE + String.format("%s-%d-%d", mUrl, mX, mY);
    }
}