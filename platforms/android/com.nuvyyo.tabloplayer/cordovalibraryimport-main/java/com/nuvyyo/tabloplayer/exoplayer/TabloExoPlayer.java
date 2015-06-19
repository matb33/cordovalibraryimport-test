package com.nuvyyo.tabloplayer.exoplayer;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.nuvyyo.tabloplayer.HttpUtil;
import com.nuvyyo.tabloplayer.Player;
import com.nuvyyo.tabloplayer.PlayerState;
import com.nuvyyo.tabloplayer.exoplayer.hls.HLSRendererBuilder;
import com.nuvyyo.tabloplayer.exoplayer.renderer.RendererBuilderCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * Created by mike on 01/06/15.
 */
public class TabloExoPlayer implements Player, ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener, RendererBuilderCallback, HlsSampleSource.EventListener, TextRenderer {
    private static final String TAG = "TabloExoPlayer";

    public static final int TRACK_RENDERER_COUNT = 3;

    public static final int TRACK_TYPE_VIDEO = 0;
    public static final int TRACK_TYPE_AUDIO = 1;
    public static final int TRACK_TYPE_TEXT  = 2;

    public static final int TRACK_TYPE_DISABLED = -1;

    private Handler mHandler;
    private Context   mContext;
    private ExoPlayer mExoPlayer;
    private SurfaceHolder mSurfaceHolder;
    private AudioCapabilities mAudioCapabilities;

    private MultiTrackChunkSource[] mMultiTrackSources;
    private TrackRenderer mVideoTrackRenderer;
    private TrackRenderer mAudioTrackRenderer;
    private TrackRenderer mTextTrackRenderer;

    private int[] mSelectedTracks;

    private HLSRendererBuilder mRendererBuilder;
    private boolean mRendererReady;
    private boolean mCaptionsEnabled;
    private volatile boolean mIsLive;

    private PlayerState mState;
    private BehaviorSubject<PlayerState> mStateSubject;
    private BehaviorSubject<VideoSize> mVideoSizeSubject;
    private BehaviorSubject<Long> mPositionSubject;
    private BehaviorSubject<Long> mDurationSubject;
    private BehaviorSubject<Boolean> mClosedCaptionsEnabledSubject;
    private PublishSubject<List<ClosedCaptionCue>> mClosedCaptionSubject;

    private Observable<Long> mIntervalObservable;
    private Subscription mIntervalSubscription;

    // ---------------------------------------------------------------------------------------------
    // Interface: Player

    public TabloExoPlayer() {

    }

    @Override
    public void initialise(Context context, SurfaceHolder surfaceHolder, DisplayMetrics metrics, AudioCapabilities audioCapabilities) {
        assert( context != null );
        assert( surfaceHolder != null );

        if( mExoPlayer != null ) {
            throw new IllegalStateException("Player already initialised. Have you forgotten to call destroy() ?");
        }

        mHandler = new Handler(Looper.getMainLooper());

        mState             = PlayerState.IDLE;
        mContext           = context;
        mSurfaceHolder     = surfaceHolder;
        mAudioCapabilities = audioCapabilities;

        mStateSubject    = BehaviorSubject.create(mState);
        mVideoSizeSubject= BehaviorSubject.create(new VideoSize(metrics.widthPixels, metrics.heightPixels, 1.0f));
        mPositionSubject = BehaviorSubject.create(0L);
        mDurationSubject = BehaviorSubject.create(0L);
        mClosedCaptionSubject = PublishSubject.create();
        mClosedCaptionsEnabledSubject = BehaviorSubject.create(false);

        mIntervalObservable = Observable.interval(250L, TimeUnit.MILLISECONDS);
        mIntervalSubscription = mIntervalObservable.subscribe(new Action1<Long>() {
            @Override
            public void call(Long sequenceNumber) {
                // Interval is used to perform play position
                // and duration updates.
                onUpdateInterval();
            }
        });

        mExoPlayer = ExoPlayer.Factory.newInstance(TRACK_RENDERER_COUNT, 1000, 5000);
        mExoPlayer.addListener(this);
    }

    @Override
    public void onSurfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        pushSurfaceAndVideoTrack(false);
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
        pushSurfaceAndVideoTrack(false);
    }

    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
        destroy();
    }

    @Override
    public Observable<Long> getPositionObservable() {
        return mPositionSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Long> getDurationObservable() {
        return mDurationSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<PlayerState> getStateObservable() {
        return mStateSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<VideoSize> getVideoSizeObservable() {
        return mVideoSizeSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<ClosedCaptionCue>> getClosedCaptionObservable() {
        return mClosedCaptionSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public PlayerState getState() {
        return mState;
    }

    @Override
    public boolean isPlaying() {
        return mState == PlayerState.STARTED || mState == PlayerState.BUFFERING;
    }

    @Override
    public long getPosition() {
        return mExoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mExoPlayer.getDuration();
    }

    @Override
    public float getBufferedPercent() {
        return (float)mExoPlayer.getBufferedPosition() / getDuration();
    }

    @Override
    public Observable<String> openUrl(final String url) {
        return openUrl(url, false);
    }

    @Override
    public Observable<String> openUrl(final String url, final boolean isLive) {
        Log.v(TAG, "openUrl(): url = "+url+", live = "+isLive);

        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                // Stop the player and unload the current media.
                releaseMediaTracks();

                // If we're currently opening something,
                // cancel it.
                cancelOpenOperation();

                // Create a renderer builder.
                mRendererBuilder = new HLSRendererBuilder(mContext, HttpUtil.getUserAgent(mContext), url, url, mAudioCapabilities);

                // Wrap the renderer builder callback with an inline one so that
                // we can easily notify the subscriber of the result.
                mRendererBuilder.buildRenderers(TabloExoPlayer.this, new RendererBuilderCallback() {
                    @Override
                    public void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers) {
                        TabloExoPlayer.this.onRenderers(trackNames, multiTrackSources, renderers);
                        Log.v(TAG, "onRenderers(): url = "+url);

                        mIsLive = isLive;
                        subscriber.onNext(url);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onRenderersError(Exception e) {
                        TabloExoPlayer.this.onRenderersError(e);
                        subscriber.onError(e);
                    }
                });
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void close() {
        cancelOpenOperation();
        releaseMediaTracks();
    }

    @Override
    public void destroy() {
        close();

        detachIntervalObservable();
        mStateSubject.onCompleted();
        mVideoSizeSubject.onCompleted();
        mPositionSubject.onCompleted();
        mDurationSubject.onCompleted();
        mClosedCaptionSubject.onCompleted();
        mClosedCaptionsEnabledSubject.onCompleted();

        mExoPlayer.release();

        mContext = null;
        mSurfaceHolder = null;
    }

    @Override
    public void start() {
        mExoPlayer.setPlayWhenReady(true);
        setPlayerState(PlayerState.STARTED);
    }

    @Override
    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
        setPlayerState(PlayerState.PAUSED);
    }

    @Override
    public boolean isPaused() {
        return !mExoPlayer.getPlayWhenReady();
    }

    @Override
    public void togglePlayback() {
        if( isPaused() )
            start();
        else
            pause();
    }

    @Override
    public void seekTo(long position) {
        mExoPlayer.seekTo(position);
    }

    @Override
    public boolean isLiveStreaming() {
        return mIsLive;
    }

    // ---------------------------------------------------------------------------------------------
    // Interface: ExoPlayer.Listener

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch(playbackState) {
            case ExoPlayer.STATE_IDLE:
                break;

            case ExoPlayer.STATE_PREPARING:
                setPlayerState(PlayerState.PREPARING);
                break;

            case ExoPlayer.STATE_BUFFERING:
                // Guarantee that we we will pass from
                // preparing -> prepared.
                if( mState == PlayerState.PREPARING )
                    setPlayerState(PlayerState.PREPARED);

                // Now set the state to buffering.
                setPlayerState(PlayerState.BUFFERING);
                break;

            case ExoPlayer.STATE_READY:
                if( playWhenReady )
                    setPlayerState(PlayerState.STARTED);
                else
                    setPlayerState(PlayerState.PAUSED);
                break;

            case ExoPlayer.STATE_ENDED:
                setPlayerState(PlayerState.END);
                break;

            default:
                Log.w(TAG, "onPlayerStateChanged(): Unknown exoplayer state: "+playbackState);
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        Log.v(TAG, "onPlayWhenReadyCommitted(): -");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        error.printStackTrace();
    }

    // ---------------------------------------------------------------------------------------------
    // Interface: RendererBuilderCallback

    @Override
    public void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers) {
        if( trackNames != null ) {
            // TODO: handle track names.
        }

        if( multiTrackSources != null ) {
            // TODO: handle multi-track sources.
        }

        // Normalize the results.
        if( trackNames == null ) {
            trackNames = new String[TabloExoPlayer.TRACK_RENDERER_COUNT][];
        }

        if( multiTrackSources == null ) {
            multiTrackSources = new MultiTrackChunkSource[TabloExoPlayer.TRACK_RENDERER_COUNT];
        }

        for(int i = 0; i < TabloExoPlayer.TRACK_RENDERER_COUNT; ++i) {
            if( renderers[i] == null ) {
                renderers[i] = new DummyTrackRenderer();
            } else if( trackNames[i] == null ) {
                int trackCount = multiTrackSources[i] != null ? multiTrackSources[i].getTrackCount() : 1;
                trackNames[i]  = new String[trackCount];
            }
        }

        mSelectedTracks = new int[TRACK_RENDERER_COUNT];

        mVideoTrackRenderer = renderers[TabloExoPlayer.TRACK_TYPE_VIDEO];
        mAudioTrackRenderer = renderers[TabloExoPlayer.TRACK_TYPE_AUDIO];
        mTextTrackRenderer  = renderers[TabloExoPlayer.TRACK_TYPE_TEXT];
        mMultiTrackSources  = multiTrackSources;

        mRendererReady = true;
        pushSurfaceAndVideoTrack(false);
        pushTrackSelection(TRACK_TYPE_AUDIO, true);
        pushTrackSelection(TRACK_TYPE_TEXT,  true);
        mExoPlayer.prepare(renderers);
    }

    @Override
    public void onRenderersError(Exception e) {
        e.printStackTrace();
    }

    //----------------------------------------------------------------------------------------------
    // Interface: MediaCodecVideoTrackRenderer.EventListener

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        Log.v(TAG, "onDroppedFrames(): count=" + count + ", elapsed=" + elapsed);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        Log.v(TAG, "onVideoSizeChanged(): width=" + width + ", height=" + height + ", PAR=" + pixelWidthHeightRatio);
        mVideoSizeSubject.onNext(new VideoSize(width, height, pixelWidthHeightRatio));
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        Log.v(TAG, "onDrawnToSurface(): surface="+surface.toString() + ", isValid="+surface.isValid());
    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        Log.v(TAG, "onDecoderInitializationError(): " + e.toString());
    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        Log.v(TAG, "onCryptoError(): " + e.toString());
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
        Log.v(TAG, "onDecoderInitialized(): " + decoderName);
    }

    // ---------------------------------------------------------------------------------------------
    // Internal

    private void detachIntervalObservable() {
        if( mIntervalSubscription != null ) {
            mIntervalSubscription.unsubscribe();
            mIntervalSubscription = null;
        }
    }

    private void onUpdateInterval() {
        mPositionSubject.onNext( getPosition() );
        mDurationSubject.onNext( getDuration() );
    }

    private void setPlayerState(PlayerState state) {
        if( state == mState )
            return;

        Log.i(TAG, mState.name() + " -> " + state.name());

        mState = state;
        mStateSubject.onNext(mState);
    }

    private void releaseMediaTracks() {
        pause();
        mExoPlayer.stop();

        mSelectedTracks = null;
        mMultiTrackSources = null;
        mVideoTrackRenderer = null;
        mAudioTrackRenderer = null;

        mIsLive = false;
        mRendererReady = false;
        setPlayerState(PlayerState.STOPPED);
        setPlayerState(PlayerState.END);
    }

    private void cancelOpenOperation() {
        if( mRendererBuilder != null ) {
            mRendererBuilder.cancel();
            mRendererBuilder = null;
        }
    }

    private void pushSurfaceAndVideoTrack(boolean blockForSurfacePush) {
        if( !mRendererReady ) {
            Log.w(TAG, "pushSurfaceAndVideoTrack(): Renderer is not ready or built.");
            return;
        }

        Surface videoSurface = mSurfaceHolder.getSurface();
        if( blockForSurfacePush )
            mExoPlayer.blockingSendMessage(mVideoTrackRenderer,
                    MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, videoSurface);
        else
            mExoPlayer.sendMessage(mVideoTrackRenderer,
                    MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, videoSurface);

        pushTrackSelection(TabloExoPlayer.TRACK_TYPE_VIDEO, videoSurface != null && videoSurface.isValid());
    }

    private void pushTrackSelection(int type, boolean allowRendererEnable) {
        if( !mRendererReady ) {
            Log.w(TAG, "pushTrackSelection(): Renderer is not ready or built.");
            return;
        }

        int trackIndex = mSelectedTracks[type];
        if( trackIndex == TabloExoPlayer.TRACK_TYPE_DISABLED ) {
            mExoPlayer.setRendererEnabled(type, false);

        } else if( mMultiTrackSources[type] == null ) {
            mExoPlayer.setRendererEnabled(type, allowRendererEnable);

        } else {
            boolean playWhenReady = mExoPlayer.getPlayWhenReady();
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.setRendererEnabled(type, false);
            mExoPlayer.sendMessage(mMultiTrackSources[type], MultiTrackChunkSource.MSG_SELECT_TRACK, trackIndex);
            mExoPlayer.setRendererEnabled(type, allowRendererEnable);
            mExoPlayer.setPlayWhenReady(playWhenReady);
        }
    }

    @Override
    public Observable<Boolean> getClosedCaptionsEnabledObservable() {
        return mClosedCaptionsEnabledSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void setClosedCaptionsEnabled(boolean enabled) {
        if( enabled != isClosedCaptionsEnabled() ) {
            mCaptionsEnabled = enabled;
            mClosedCaptionsEnabledSubject.onNext(enabled);
        }
    }

    @Override
    public boolean isClosedCaptionsEnabled() {
        return mCaptionsEnabled;
    }

    // ---------------------------------------------------------------------------------------------
    // Interface: HlsSampleSource.EventListener

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format, int mediaStartTimeMs, int mediaEndTimeMs) {
        Log.v(TAG, String.format("onLoadStarted(): length = %d, mediaStartTimeMs = %d, mediaEndTimeMs = %d", length, mediaStartTimeMs, mediaEndTimeMs));
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format, int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        Log.v(TAG, String.format("onLoadCompleted(): bytesLoaded = %d, mediaStartTimeMs = %d, mediaEndTimeMs = %d, elapsedRealtimeMs = %d, loadDurationMs = %d",
                bytesLoaded, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs));
    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        Log.v(TAG, String.format("onLoadCanceled(): bytesLoaded = %d", bytesLoaded));
    }

    @Override
    public void onLoadError(int sourceId, IOException e) {
        Log.v(TAG, String.format("onLoadError(): exception = %s", e.toString()));
        setPlayerState(PlayerState.ERROR);
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs, int mediaEndTimeMs) {
        Log.v(TAG, String.format("onUpstreamDiscarded(): mediaStartTimeMs = %d, mediaEndTimeMs = %d",
                mediaStartTimeMs, mediaEndTimeMs));
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, int mediaTimeMs) {
        Log.v(TAG, String.format("onDownstreamFormatChanged(): mediaTimeMs = %d", mediaTimeMs));
    }

    @Override
    public void onCues(List<Cue> cues) {
        if( cues == null )
            return;

        List<ClosedCaptionCue> closedCaptionCues = new ArrayList<>();
        for(Cue cue : cues) {
            closedCaptionCues.add(playerCueFromExoCue(cue));
        }

        mClosedCaptionSubject.onNext(closedCaptionCues);
    }

    private ClosedCaptionCue playerCueFromExoCue(Cue cue) {
        return new ClosedCaptionCue(cue.text, cue.line, cue.position, cue.alignment, cue.size);
    }
}
