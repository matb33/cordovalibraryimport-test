package com.nuvyyo.tabloplayer.bif;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.exoplayer.util.Assertions;
import com.nuvyyo.tabloplayer.DateTimeUtil;
import com.nuvyyo.tabloplayer.R;
import com.nuvyyo.tabloplayer.bif.SnapshotGrid.SnapshotIndexPath;
import com.nuvyyo.tabloplayer.util.MeasureUtil;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mike on 11/06/15.
 */
public class SnapshotView extends LinearLayout {
    private static final String TAG = "SnapshotView";

    private static final int LAYOUT_ID = R.layout.view_snapshot;
    private static final float SNAPSHOT_SCALE = 1.0f;

    private ExecutorService mExecutor;
    private SnapshotGrid mGrid;
    private Picasso mPicasso;

    private ViewGroup mRoot;
    private TextView mTimestampLabel;

    private SnapshotIndexPath mSnapshotIndex;
    private volatile int mRequestedGridIndex;

    private volatile Bitmap mBitmap;
    private DisplayMetrics mDisplayMetrics;

    public SnapshotView(Context context) {
        this(context, null);
    }

    public SnapshotView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        mSnapshotIndex = SnapshotIndexPath.INVALID;
        mRequestedGridIndex = -1;

        mRoot = (ViewGroup)inflater.inflate(LAYOUT_ID, this, true);
        mTimestampLabel = (TextView)findViewById(R.id.lblTimestamp);

        mExecutor = Executors.newFixedThreadPool(1);
        mPicasso = new Picasso.Builder(getContext())
                        .build();
    }

    private void initDisplayMetrics() {
        Activity activity = (Activity)getContext();
        Assertions.checkState( activity != null );

        mDisplayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseGridBitmap();
        super.onDetachedFromWindow();
    }

    private void releaseGridBitmap() {
        if( mBitmap != null ) {
            Log.d(TAG, "releaseGridBitmap(): Releasing grid bitmap");
            mBitmap = null;
        }
    }

    public void setTimestampMs(long timestampMs) {
        if( !hasSnapshots() )
            return;

        // Get index path
        final SnapshotIndexPath index = mGrid.indexPathForTimestamp(timestampMs);

        // Download bitmap if required.
        if( mSnapshotIndex.gridIndex != index.gridIndex || mBitmap == null ) {
            releaseGridBitmap();

            final String url = getSnapshotUrlForGridIndex(index.gridIndex);
            mPicasso.load(url).fetch(new Callback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "onSuccess(): Downloaded.");
                    mExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap nonFinalBitmap = null;
                            try {
                                nonFinalBitmap = mPicasso.load(url).get();
                            }catch(Exception e) {
                                e.printStackTrace();

                            }

                            final Bitmap bitmap = nonFinalBitmap;
                            getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    mBitmap = bitmap;
                                    invalidate();
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError() {
                    Log.d(TAG, "imageDownload.onError(): Failed to download "+url);
                }
            });
        }

        // Update Label
        mTimestampLabel.setText(DateTimeUtil.DURATION_FORMATTER_NUMERIC_SHORT.format(timestampMs));

        // Update index
        mSnapshotIndex = index;

        // Force re-draw
        invalidate();
    }

    public boolean hasSnapshots() {
        return mGrid != null;
    }

    public void setSnapshotGrid(SnapshotGrid grid) {
        Assertions.checkArgument(grid != null);

        // Check if the new grid is the same.
        if( mGrid != null && mGrid.equals(grid) )
            return;

        mGrid = grid;

        // Trigger preload
       for(int i = 0; i < mGrid.getGrids(); ++i)
            mPicasso.load(getSnapshotUrlForGridIndex(i))
                    .fetch();
    }

    public long getCurrentTimestampMs() {
        if( mGrid == null || mSnapshotIndex == null )
            return -1;

        return mGrid.timestampForIndexPath(mSnapshotIndex);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if( mGrid != null ) {
            if( mDisplayMetrics == null )
                initDisplayMetrics();

            int width  = (int)(mGrid.getWidth() * SNAPSHOT_SCALE);
            int height = (int)(mGrid.getHeight() * SNAPSHOT_SCALE);

            width  = MeasureUtil.dipsToPixels(mDisplayMetrics, width);
            height = MeasureUtil.dipsToPixels(mDisplayMetrics, height);

            widthMeasureSpec  = MeasureSpec.makeMeasureSpec(width,  MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if( mGrid == null || !mSnapshotIndex.isValid() || mBitmap == null ) {
            super.onDraw(canvas);
            return;
        }

        Bitmap slice  = null;
        Bitmap scaled = null;

        try {
            final int outputWidth  = MeasureUtil.dipsToPixels(mDisplayMetrics, (int)(mGrid.getWidth() * SNAPSHOT_SCALE));
            final int outputHeight = MeasureUtil.dipsToPixels(mDisplayMetrics, (int)(mGrid.getHeight() * SNAPSHOT_SCALE));

            final int sourceOffsetX = mSnapshotIndex.x * -mGrid.getWidth();
            final int sourceOffsetY = mSnapshotIndex.y * -mGrid.getHeight();

            // Slice the single snapshot we need out of the grid
            slice = Bitmap.createBitmap(mGrid.getWidth(), mGrid.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas sliceCanvas = new Canvas(slice);
            sliceCanvas.drawBitmap(mBitmap, sourceOffsetX, sourceOffsetY, null);

            // Scale the slice up to the size we need.
            scaled = Bitmap.createScaledBitmap(slice, outputWidth, outputHeight, true);

            // Draw the scaled slice to the canvas.
            canvas.drawBitmap(scaled, 0, 0, null);
        }catch(OutOfMemoryError e) {
            Log.w(TAG, "onDraw(): " + e.getMessage());
        }

        super.onDraw(canvas);
    }

    private String getSnapshotUrlForGridIndex(int gridIndex) {

        return mGrid.getUrlPattern().replace("{{n}}", String.valueOf(gridIndex));
    }
}
