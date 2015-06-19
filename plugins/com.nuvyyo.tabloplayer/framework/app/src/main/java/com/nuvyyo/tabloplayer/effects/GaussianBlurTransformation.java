package com.nuvyyo.tabloplayer.effects;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso.Transformation;

/**
 * Created by mike on 10/06/15.
 */
public class GaussianBlurTransformation implements Transformation {

    private final RenderScript _renderScript;
    private final ScriptIntrinsicBlur _scriptIntrinsicBlur;

    /**
     * Creates a new GaussianBlurTransformation.
     *
     * Uses a default blur radius of 25f.
     *
     * @param context The context to create the RenderScript instance from.
     */
    public GaussianBlurTransformation(final Context context) {
        this(context, 25f);
    }

    /**
     * Creates a new GaussianBlurTransformation.
     *
     * @param context The context to create the RenderScript instance from.
     * @param radius The blur radius.
     */
    @TargetApi(17)
    public GaussianBlurTransformation(final Context context, final float radius) {
        _renderScript = RenderScript.create(context);
        _scriptIntrinsicBlur = ScriptIntrinsicBlur.create(_renderScript, Element.U8_4(_renderScript));
        _scriptIntrinsicBlur.setRadius(radius);
    }

    @Override
    @TargetApi(17)
    public Bitmap transform(final Bitmap source) {
        final Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

        final Allocation allocationInput = Allocation.createFromBitmap(_renderScript, source);
        final Allocation allocationOutput = Allocation.createFromBitmap(_renderScript, result);

        // perform the transformation
        _scriptIntrinsicBlur.setInput(allocationInput);
        _scriptIntrinsicBlur.forEach(allocationOutput);

        // copy the final bitmap created by the output Allocation to the outBitmap
        allocationOutput.copyTo(result);

        source.recycle();
        _scriptIntrinsicBlur.destroy();

        return result;
    }

    // allow picasso to cache the result of this trnasformation
    @Override public String key() {
        return "gauss-blur";
    }
}
