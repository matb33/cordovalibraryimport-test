package com.nuvyyo.tabloplayer.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

/**
 * Created by mike on 10/06/15.
 */
public class DrawBorderTransformation implements Transformation {

    @Override
    public Bitmap transform(final Bitmap source) {
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Draw source to output
        Paint copyPaint = new Paint();
        copyPaint.setAntiAlias(true);
        canvas.drawBitmap(source, 0.f, 0.f, copyPaint);

        Paint paint = new Paint();
        paint.setColor(Color.argb((int) (255 * .6f), 255, 255, 255));
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2.0f);
        paint.setStyle(Paint.Style.STROKE);

        // Top Border
        canvas.drawLine(0, 0, source.getWidth(), 0, paint);

        // Right Border
        canvas.drawLine(source.getWidth(), 0, source.getWidth(), source.getHeight(), paint);

        // Bottom Border
        canvas.drawLine(0, source.getHeight(), source.getWidth(), source.getHeight(), paint);

        // Left Border
        canvas.drawLine(0, 0, 0, source.getHeight(), paint);

        // Recycle input source.
        if( source != output )
            source.recycle();

        return output;
    }

    @Override
    public String key() {
        return "tablo-draw-borders";
    }
}
