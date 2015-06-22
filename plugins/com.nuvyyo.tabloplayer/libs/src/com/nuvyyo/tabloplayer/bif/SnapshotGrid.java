package com.nuvyyo.tabloplayer.bif;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by mike on 11/06/15.
 */
public class SnapshotGrid {
    @SerializedName(value="rows")
    private int mRows;

    @SerializedName(value="cols")
    private int mColumns;

    @SerializedName(value="width")
    private int mWidth;

    @SerializedName(value="height")
    private int mHeight;

    @SerializedName(value="grids")
    private int mGrids;

    @SerializedName(value="urlPattern")
    private String mUrlPattern;

    @SerializedName(value="step")
    private int[] mStepSizes;

    public long getApproximateDurationSeconds() {
        long maxStepSize = mStepSizes[ mStepSizes.length - 1 ];
        long duration = mGrids * (mRows * mColumns) * maxStepSize;

        // Correct for step sizes
        for(int stepSize : mStepSizes) {
            duration -= (maxStepSize - stepSize);
        }

        return duration;
    }

    public int getRows() {
        return mRows;
    }

    public int getColumns() {
        return mColumns;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getGrids() {
        return mGrids;
    }

    public String getUrlPattern() {
        return mUrlPattern;
    }

    public int[] getStepSizes() {
        return mStepSizes;
    }

    public SnapshotIndexPath indexPathForTimestamp(long timestampMillis) {
        long duration      = TimeUnit.MILLISECONDS.toSeconds(timestampMillis);
        int snapsPerSprite = getRows() * getColumns();

        int spriteIdx = -1;
        int snapCol   = -1;
        int snapRow   = -1;

        for(int i = 0, idx = 0; i <= duration; i += getIntervalSecondsForIndex(idx), ++idx) {
            spriteIdx = (int)Math.floor( idx / snapsPerSprite );
            int snapIdx   = idx % snapsPerSprite;

            snapCol = idx % getColumns();
            snapRow = (int)Math.floor(snapIdx / getColumns());
        }

        return new SnapshotIndexPath(spriteIdx, snapCol, snapRow);
    }

    public long timestampForIndexPath(SnapshotIndexPath index) {
        long durationSeconds = getApproximateDurationSeconds();
        int snapsPerSprite   = getRows() * getColumns();

        int spriteIdx = -1;
        int snapCol   = -1;
        int snapRow   = -1;

        for(int i = 0, idx = 0; i <= durationSeconds; i += getIntervalSecondsForIndex(idx), ++idx) {
            spriteIdx = (int)Math.floor( idx / snapsPerSprite );
            int snapIdx   = idx % snapsPerSprite;

            snapCol = idx % getColumns();
            snapRow = (int)Math.floor(snapIdx / getColumns());

            if( spriteIdx == index.gridIndex && snapCol == index.x && snapRow == index.y )
                return TimeUnit.SECONDS.toMillis(i);
        }

        return -1L;
    }

    private int getIntervalSecondsForIndex(int i) {
        int[] stepSizes = getStepSizes();
        return stepSizes[ Math.min(i, stepSizes.length - 1) ];
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == null || !(obj instanceof SnapshotGrid) )
            return false;

        SnapshotGrid other = (SnapshotGrid)obj;

        boolean equal = false;

        equal  = mColumns == other.mColumns;
        equal &= mRows == other.mRows;
        equal &= mUrlPattern.equals(other.mUrlPattern);
        equal &= mWidth == other.mWidth;
        equal &= mHeight == other.mHeight;
        equal &=  mGrids == other.mGrids;

        return equal;
    }

    public static final class SnapshotIndexPath {
        public static final SnapshotIndexPath INVALID = new SnapshotIndexPath(-1, -1, -1);

        public int gridIndex;
        public int x;
        public int y;

        public SnapshotIndexPath(int gridIndex, int x, int y) {
            this.gridIndex = gridIndex;
            this.x = x;
            this.y = y;
        }

        public boolean isValid() {
            return gridIndex >= 0 && x >=0 && y >= 0;
        }

        public String toString() {
            return String.format("{%d, %d, %d}", gridIndex, x, y);
        }
    }
}
