package com.nuvyyo.tabloplayer;

import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.exoplayer.audio.AudioCapabilities;

import java.util.List;

import rx.Observable;

/**
 * Created by mike on 01/06/15.
 */
public interface Player {
    public static final class VideoSize {
        public int width;
        public int height;
        public float pixelAspectRatio;

        public VideoSize(int w, int h, float par) {
            width = w;
            height = h;
            pixelAspectRatio = par;
        }
    }

    public static final class ClosedCaptionCue {
        /**
         * Used by some methods to indicate that no value is set.
         */
        public static final int UNSET_VALUE = -1;

        public final CharSequence text;

        public final int line;
        public final int position;
        public final Layout.Alignment alignment;
        public final int size;

        public ClosedCaptionCue() {
            this(null);
        }

        public ClosedCaptionCue(CharSequence text) {
            this(text, UNSET_VALUE, UNSET_VALUE, null, UNSET_VALUE);
        }

        public ClosedCaptionCue(CharSequence text, int line, int position, Layout.Alignment alignment, int size) {
            this.text = text;
            this.line = line;
            this.position = position;
            this.alignment = alignment;
            this.size = size;
        }
    }

    public void initialise(Context context, SurfaceHolder surfaceHolder, DisplayMetrics metrics, AudioCapabilities audioCapabilities);

    public void onSurfaceCreated(SurfaceHolder holder);
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height);
    public void onSurfaceDestroyed(SurfaceHolder holder);

    public Observable<Long> getPositionObservable();
    public Observable<Long> getDurationObservable();
    public Observable<PlayerState> getStateObservable();
    public Observable<VideoSize> getVideoSizeObservable();
    public Observable<List<ClosedCaptionCue>> getClosedCaptionObservable();

    public void setClosedCaptionsEnabled(boolean enabled);
    public boolean isClosedCaptionsEnabled();
    public Observable<Boolean> getClosedCaptionsEnabledObservable();

    public PlayerState getState();

    /**
     * Whether the player is currently attempting to play video. If the
     * player is paused for a re-buffer, this still returns <code>true</code>.
     *
     * @return
     */
    public boolean isPlaying();

    public long getPosition();
    public long getDuration();
    public float getBufferedPercent();

    public Observable<String> openUrl(final String url);
    public Observable<String> openUrl(final String url, final boolean isLive);

    public void close();
    public void destroy();

    public void start();
    public void pause();
    public boolean isPaused();

    public void togglePlayback();
    public void seekTo(final long position);

    public boolean isLiveStreaming();
}
