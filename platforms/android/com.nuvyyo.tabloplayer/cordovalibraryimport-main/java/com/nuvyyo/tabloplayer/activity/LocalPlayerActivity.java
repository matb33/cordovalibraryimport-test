package com.nuvyyo.tabloplayer.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;

import com.nuvyyo.tabloplayer.Player;
import com.nuvyyo.tabloplayer.PlayerFactory;
import com.nuvyyo.tabloplayer.PlayerState;
import com.nuvyyo.tabloplayer.R;
import com.nuvyyo.tabloplayer.captions.ClosedCaptionView;
import com.nuvyyo.tabloplayer.util.MediaInfoUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;

/**
 * Created by mike on 05/06/15.
 */
public class LocalPlayerActivity extends PlayerActivity implements SurfaceHolder.Callback, AudioCapabilitiesReceiver.Listener {
    private static final String TAG                 = "LocalPlayerActivity";

    private static final String KEY_PLAYLIST_URL    = "PlaylistUrl";
    private static final String KEY_PLAY_POSITION   = "PlayPosition";
    private static final String KEY_IS_PAUSED       = "IsPaused";
    private static final String KEY_MEDIA_INFO      = "MediaInfo";
    private static final String KEY_LIVE_STREAM     = "MediaIsLive";
    private static final String KEY_CAPTIONS_ENABLED= "ClosedCaptionsEnabled";

    // ------------------------------------------------------------------------
    // Player

    private Player mPlayer;
    private long mLastUserIneractionTimeNs;

    private AudioCapabilitiesReceiver mAudioCapabilitiesReceiver;
    private AudioCapabilities mAudioCapabilities;

    /**
     * The position the player was at before pausing. This is
     * used to restore state after onPause().
     */
    private long mPreviousPosition;

    /**
     * Indicates whether the player was paused before
     * the activity was paused. This is used to restore
     * state after onPause().
     */
    private boolean mPlayerWasPaused;

    /**
     * Provided by the calling activity. Indicates if the
     * current media is a live stream.
     */
    private boolean mMediaIsLive;

    /**
     * Provided by the calling activity. Updated when
     * the user toggles captions.
     */
    private boolean mCaptionsEnabled;

    /**
     * If true, the player will seek to the live position
     * on the next duration update.
     */
    private boolean mShouldSeekToLive;

    // -----------------------------------------------
    // Media Button Receiver

    public static final String ACTION_MEDIA_CONTROL = "com.nuvyyo.android.slipstream.exovideoplayer.ACTION_MEDIA_CONTROL";
    public static final String EXTRA_MEDIA_CONTROL  = "media-control";

    public static final String MEDIA_CONTROL_FAST_FORWARD   = "fast forward";
    public static final String MEDIA_CONTROL_REWIND         = "rewind";

    private ComponentName mMediaButtonReceiverName;
    private boolean       mMediaButtonReceiverRegistered;

    private BroadcastReceiver mMediaControlReceiver;

    // -----------------------------------------------
    // Audio Focus Management

    private AudioManager                            mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioFocusChangeListener();
    private boolean                                 mHasAudioFocus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SurfaceView videoSurfaceView = getVideoSurfaceView();
        videoSurfaceView.getHolder().addCallback(this);
        videoSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);

        mAudioCapabilitiesReceiver = new AudioCapabilitiesReceiver(this, this);
        mMediaControlReceiver = new MediaControlReceiver();

        // Pull values out from the previous
        // instance.
        if( savedInstanceState != null ) {
            Bundle mediaInfoBundle = savedInstanceState.getBundle(KEY_MEDIA_INFO);
            long playPosition = savedInstanceState.getLong(KEY_PLAY_POSITION, 0L);
            boolean wasPaused  = savedInstanceState.getBoolean(KEY_IS_PAUSED, false);
            boolean isLive = savedInstanceState.getBoolean(KEY_LIVE_STREAM, false);
            boolean captionsEnabled = savedInstanceState.getBoolean(KEY_CAPTIONS_ENABLED, false);

            // Put values into launch intent for natural handling.
            Intent intent = getIntent();
            intent.putExtra(EXTRA_PLAY_POSITION, playPosition);
            intent.putExtra(EXTRA_PLAYER_PAUSED, wasPaused);
            intent.putExtra(EXTRA_MEDIA_INFO, mediaInfoBundle);
            intent.putExtra(EXTRA_MEDIA_LIVE_STREAM, isLive);
            intent.putExtra(EXTRA_CAPTIONS_ENABLED, captionsEnabled);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        handleLaunchIntent();
        mAudioCapabilitiesReceiver.register();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        requestAudioFocus();
    }

    @Override
    public void onPause(){
        updateLaunchIntent();
        releasePlayer();
        mAudioCapabilitiesReceiver.unregister();
        releaseAudioFocus();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        boolean wasPaused = mPlayerWasPaused;
        boolean captionsEnabled = mCaptionsEnabled;
        if( mPlayer != null ) {
            wasPaused = mPlayer.isPaused();
            captionsEnabled = mPlayer.isClosedCaptionsEnabled();
        }

        savedInstanceState.putLong(KEY_PLAY_POSITION, mPreviousPosition);
        savedInstanceState.putBoolean(KEY_IS_PAUSED, wasPaused);
        savedInstanceState.putBoolean(KEY_LIVE_STREAM, mMediaIsLive);
        savedInstanceState.putBundle(KEY_MEDIA_INFO, MediaInfoUtil.mediaInfoToBundle(getMediaInfo()));
        savedInstanceState.putBoolean(KEY_CAPTIONS_ENABLED, captionsEnabled);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void willShowSystemUi() {
        super.willShowSystemUi();

        updateLastUserInteractionTime();
    }

    @Override
    protected void willHideSystemUi() {
        super.willHideSystemUi();


    }

    @Override
    protected void onClosedCaptionButtonClicked() {
        super.onClosedCaptionButtonClicked();

        setClosedCaptionsEnabled( !isClosedCaptionsEnabled() );
    }

    @Override
    protected boolean isClosedCaptionsEnabled() {
        return mPlayer != null && mPlayer.isClosedCaptionsEnabled();
    }

    @Override
    protected void setClosedCaptionsEnabled(boolean enabled) {
        mPlayer.setClosedCaptionsEnabled(enabled);
        updateClosedCaptionButton(mPlayer.isClosedCaptionsEnabled());
    }

    @Override
    protected void onFullScreenContentContainerClicked() {
        super.onFullScreenContentContainerClicked();

        updateLastUserInteractionTime();
    }

    @Override
    protected void onPlayPauseClicked() {
        super.onPlayPauseClicked();

        updateLastUserInteractionTime();
        mPlayer.togglePlayback();
    }

    @Override
    protected void onSkipBackwardClicked() {
        super.onSkipBackwardClicked();

        updateLastUserInteractionTime();
        quickSkipBackward();
    }

    @Override
    protected void onSkipForwardClicked() {
        super.onSkipForwardClicked();

        updateLastUserInteractionTime();
        quickSkipForward();
    }

    @Override
    protected void onSeekProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onSeekProgressChanged(seekBar, progress, fromUser);

        if( fromUser )
            updateLastUserInteractionTime();
    }

    @Override
    protected void onSeekStartTrackingTouch(SeekBar seekBar) {
        super.onSeekStartTrackingTouch(seekBar);
        startUserSeek();
    }

    @Override
    protected void onSeekStopTrackingTouch(SeekBar seekBar) {
        super.onSeekStopTrackingTouch(seekBar);
        finishUserSeek();
    }

    @Override
    protected boolean isLiveStream() {
        return mPlayer != null && mPlayer.isLiveStreaming();
    }

    private void handleLaunchIntent() {
        Intent intent = getIntent();
        if( intent == null ) {
            Log.e(TAG, "handleLaunchIntent(): intent = null");
            return;
        }

        if (intent.hasExtra(EXTRA_MEDIA_INFO)) {
            Log.v(TAG, "handleLaunchIntent(): url from prod intent");

            MediaInfo info = MediaInfoUtil.bundleToMediaInfo(intent.getBundleExtra(EXTRA_MEDIA_INFO));
            setMediaInfo(info);

            mPreviousPosition = intent.getLongExtra(EXTRA_PLAY_POSITION, 0L);
            mPlayerWasPaused  = intent.getBooleanExtra(EXTRA_PLAYER_PAUSED, false);
            mMediaIsLive      = intent.getBooleanExtra(EXTRA_MEDIA_LIVE_STREAM, false);
            mCaptionsEnabled  = intent.getBooleanExtra(EXTRA_CAPTIONS_ENABLED, false);
        }
    }

    private void updateLaunchIntent() {
        Bundle mediaInfoBundle = MediaInfoUtil.mediaInfoToBundle(getMediaInfo());
        long playPosition = mPlayer.getPosition();
        boolean wasPaused = mPlayer.isPaused();
        boolean isLive = mMediaIsLive;
        boolean captionsEnabled = mPlayer.isClosedCaptionsEnabled();

        // Put values into launch intent for natural handling.
        Intent intent = getIntent();
        intent.putExtra(EXTRA_PLAY_POSITION, playPosition);
        intent.putExtra(EXTRA_PLAYER_PAUSED, wasPaused);
        intent.putExtra(EXTRA_MEDIA_INFO, mediaInfoBundle);
        intent.putExtra(EXTRA_MEDIA_LIVE_STREAM, isLive);
        intent.putExtra(EXTRA_CAPTIONS_ENABLED, captionsEnabled);
    }

    private void initialisePlayer() {
        Assertions.checkState(mPlayer == null);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mPlayer = PlayerFactory.createPlayer(PlayerFactory.PlayerType.EXO_PLAYER);
        mPlayer.initialise(this, getVideoSurfaceView().getHolder(), metrics, mAudioCapabilities);

        mPlayer.getStateObservable().subscribe(new Action1<PlayerState>() {
            @Override
            public void call(PlayerState state) {
                onPlayerStateChanged(state);
            }
        });

        Observable.zip(mPlayer.getPositionObservable(), mPlayer.getDurationObservable(), new Func2<Long, Long, Void>() {
            @Override
            public Void call(Long playPosition, Long playDuration) {
                if( mPlayer == null ) {
                    Log.w(TAG, "initialisePlayer(): zip player is null.");
                    return null;
                }

                // If user is actively manipulating the seek bar,
                // we will do nothing.
                if( isUserSeekInProgress() )
                    return null;

                if (!mPlayer.isPaused() && System.nanoTime() - mLastUserIneractionTimeNs >= TimeUnit.MILLISECONDS.toNanos(HIDE_CONTROLS_DELAY_MS))
                    getUiVisibilityManager().hideSystemUi();

                getSeekBar().setProgress(playPosition.intValue());
                getSeekBar().setMax(playDuration.intValue());

                // Seek to live if required.
                if( mShouldSeekToLive && playDuration > 0L ) {
                    seekToLive();
                    mShouldSeekToLive = false;
                }
                return null;
            }

        }).subscribe();

        mPlayer.getVideoSizeObservable().subscribe(new Action1<Player.VideoSize>() {
            @Override
            public void call(Player.VideoSize videoSize) {
                if( mPlayer == null ) {
                    Log.w(TAG, "initialisePlayer(): videoSizeObservable player is null.");
                    return;
                }

                onVideoSizeChanged(videoSize);
            }
        });

        mPlayer.getClosedCaptionsEnabledObservable().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean captionsEnabled) {
                if( mPlayer == null ) {
                    Log.w(TAG, "initialisePlayer(): closedCaptionsEnabled player is null.");
                    return;
                }

                updateClosedCaptionButton(captionsEnabled);
            }
        });

        mPlayer.getClosedCaptionObservable().subscribe(new Action1<List<Player.ClosedCaptionCue>>() {
            @Override
            public void call(List<Player.ClosedCaptionCue> closedCaptionCues) {
                if( mPlayer == null ) {
                    Log.w(TAG, "initialisePlayer(): closedCaptionsObservable player is null.");
                    return;
                }

                if( !mPlayer.isClosedCaptionsEnabled() ) {
                    return;
                }

                ClosedCaptionView view = getClosedCaptionView();
                view.clearLines();

                for(Player.ClosedCaptionCue cue : closedCaptionCues)
                    view.addLine(cue.text);
            }
        });

        mPlayer.setClosedCaptionsEnabled(mCaptionsEnabled);

        // Seek to live if viewing a live stream.
        mShouldSeekToLive = mMediaIsLive;
    }

    private void releasePlayer() {
        if( mPlayer == null ) {
            Log.w(TAG, "releasePlayer(): Player not initialised.");
            return;
        }

        mPreviousPosition = mPlayer.getPosition();
        mPlayerWasPaused  = mPlayer.isPaused();
        mCaptionsEnabled  = mPlayer.isClosedCaptionsEnabled();
        Log.v(TAG, "releasePlayer(): previous = "+mPreviousPosition);

        mPlayer.destroy();
        mPlayer = null;
    }

    private void preparePlayer() {
        initialisePlayer();
        updateLastUserInteractionTime();
        getUiVisibilityManager().showSystemUi();
    }

    private void updatePlayPauseButton(PlayerState state) {
        switch(state) {
            case PAUSED:
                getPlayPauseButton().setImageResource(R.drawable.button_video_player_play);
                break;

            case IDLE:
            case PREPARED:
            case PREPARING:
            case BUFFERING:
            case STARTED:
            case STOPPED:
            case END:
            case ERROR:
            case PLAYBACK_COMPLETE:
                getPlayPauseButton().setImageResource(R.drawable.button_video_player_pause);
                break;
        }
    }

    private void updateClosedCaptionButton(boolean captionsEnabled) {
        if( captionsEnabled ) {
            getClosedCaptionButton().setTextColor(getResources().getColor(R.color.tablo_blue));
        } else {
            getClosedCaptionButton().setTextColor(getResources().getColor(R.color.white));
            getClosedCaptionView().setVisibility(View.GONE);
        }
    }

    private void onVideoSizeChanged(Player.VideoSize videoSize) {
        // Validate dimensions
        if( videoSize.width <= 0 || videoSize.height <= 0 )
        {
            // We have received invalid video dimensions. This only happens on
            // select android tablets, and typically this method will be called again
            // with valid dimensions prior to video rendering beginning. The following
            // has been introduced as a workaround to prevent divide by 0 errors in this case.
            return;	// Get out to avoid divide by 0 error, and hope the video surface doesn't look awful.
        }

        // Retrieve the screen size
        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        // Get dimensions
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;

        // Set up target dimensions
        int targetVideoWidth = videoSize.width;
        int targetVideoHeight= videoSize.height;

        // Get video surface layout parameters
        RelativeLayout.LayoutParams videoLayoutParams = (RelativeLayout.LayoutParams)getVideoSurfaceView().getLayoutParams();

        // Size content for view
        if( videoSize.width > videoSize.height )
        {
            targetVideoWidth = screenWidth;
            targetVideoHeight = screenWidth * videoSize.height / videoSize.width;
        }else
        {
            targetVideoWidth = screenHeight * videoSize.width / videoSize.height;
            targetVideoHeight = screenHeight;
        }

        // Immediate value change
        videoLayoutParams.width = targetVideoWidth;
        videoLayoutParams.height= targetVideoHeight;

        // Apply layout parameters
        getVideoSurfaceView().setLayoutParams(videoLayoutParams);
    }

    private void openMedia(String url) {
        mPlayer.openUrl(url, mMediaIsLive).subscribe(new Action1<String>(){

            @Override
            public void call(String url) {
                startMedia();
            }

        }, new Action1<Throwable>(){

            @Override
            public void call(Throwable e) {
                e.printStackTrace();
                Toast.makeText(LocalPlayerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

        });
    }

    private void startMedia() {
        try {
            Log.v(TAG, "startMedia(): previous = "+mPreviousPosition);
            seekTo(mPreviousPosition);
            play();

            if( mPlayerWasPaused )
                pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLastUserInteractionTime() {
        mLastUserIneractionTimeNs = System.nanoTime();
    }

    private void quickSkipForward() {
        long seekPoint = getSeekBar().getProgress() + QUICK_SKIP_FORWARD_MS;
        seekTo( seekPoint );
    }

    private void quickSkipBackward() {
        long seekPoint = getSeekBar().getProgress() + QUICK_SKIP_BACKWARD_MS;
        seekTo( seekPoint );
    }

    private void onPlayerStateChanged(PlayerState state) {
        if( state == PlayerState.END || state == PlayerState.PLAYBACK_COMPLETE || state == PlayerState.ERROR ) {
            seekTo(0L);
            pause();
            getUiVisibilityManager().showSystemUi();
            if( state == PlayerState.ERROR )
                Toast.makeText(this, "Playback Failed", Toast.LENGTH_LONG).show();
        }

        if( state == PlayerState.BUFFERING || state == PlayerState.PREPARING || state == PlayerState.PREPARED )
            showSpinner();
        else
            hideSpinner();

        // Allow device to sleep in non-playback states.
        if( state == PlayerState.IDLE || state == PlayerState.ERROR || state == PlayerState.END || state == PlayerState.PLAYBACK_COMPLETE || state == PlayerState.PAUSED )
            getUiVisibilityManager().setKeepScreenOn(false);
        else
            getUiVisibilityManager().setKeepScreenOn(true);

        updatePlayPauseButton(state);
    }

    @Override
    protected void seekTo(long position) {
        if( mPlayer != null )
            mPlayer.seekTo(position);
    }

    @Override
    protected void play() {
        if( mPlayer != null )
            mPlayer.start();
    }

    @Override
    protected void pause() {
        if( mPlayer != null )
            mPlayer.pause();
    }

    @Override
    protected long getPosition() {
        if( mPlayer != null )
            return mPlayer.getPosition();

        return 0L;
    }

    @Override
    protected long getDuration() {
        if( mPlayer != null )
            return mPlayer.getDuration();

        return 0L;
    }

    // ------------------------------------------------------------------------
    // Internal: Cast

    private void finishAndLaunchCastPlayer() {
        finish();

        long playPosition = mPreviousPosition;
        if( mPlayer != null && mPlayer.isPlaying() )
            playPosition = mPlayer.getPosition();

        boolean wasPaused = mPlayerWasPaused;
        if( mPlayer != null )
            wasPaused = mPlayer.isPaused();

        boolean captionsEnabled = mCaptionsEnabled;
        if( mPlayer != null )
            captionsEnabled = mPlayer.isClosedCaptionsEnabled();

        Intent intent = new Intent(this, CastPlayerActivity.class);

        // If there is already a local player activity in the task,
        // move it to the front instead of starting another one.
        intent.putExtra( EXTRA_MEDIA_INFO,    MediaInfoUtil.mediaInfoToBundle(getMediaInfo()) );
        intent.putExtra( EXTRA_PLAY_POSITION, playPosition );
        intent.putExtra( EXTRA_PLAYER_PAUSED, wasPaused );
        intent.putExtra( EXTRA_CAPTIONS_ENABLED, captionsEnabled);
        intent.putExtra( EXTRA_SHOULD_START,  true );

        intent.putExtra( EXTRA_MEDIA_LIVE_STREAM,   mMediaIsLive );
        startActivity(intent);
    }

    @Override
    protected void onCastApplicationStateChanged(boolean castApplicationRunning) {
        super.onCastApplicationStateChanged(castApplicationRunning);

        if( castApplicationRunning && getCastManager().isConnected() )
            finishAndLaunchCastPlayer();
    }

    // ------------------------------------------------------------------------
    // interface: SurfaceHolder.Callback

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if( mPlayer != null )
            mPlayer.onSurfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if( mPlayer != null )
            mPlayer.onSurfaceChanged(holder, format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if( mPlayer != null ) {
            mPlayer.onSurfaceDestroyed(holder);

            mPlayer.destroy();
            mPlayer = null;
        }
    }

    // ------------------------------------------------------------------------
    // Interface: AudioCapabilitiesReceiver.Listener

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        Log.v(TAG, "onAudioCapabilitiesChanged(): maxChannelCount = "+audioCapabilities.getMaxChannelCount());

        boolean audioCapabilitiesChanged = !audioCapabilities.equals(mAudioCapabilities);
        if( mPlayer == null || audioCapabilitiesChanged ) {
            mAudioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
            openMedia(getMediaInfo().getContentId());
        }
    }

    // ------------------------------------------------------------------------
    // Audio Focus Management

    private void requestAudioFocus() {
        if( mHasAudioFocus )
            return;

        int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if( result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ) {
            Log.e(TAG, "requestAudioFocus(): Focus granted.");
            mHasAudioFocus = true;
            registerMediaButtonReceiver();
        } else {
            // Denied
            mHasAudioFocus = false;
            Log.e(TAG, "requestAudioFocus(): Focus denied.");
        }
    }

    private void releaseAudioFocus() {
        if( !mHasAudioFocus )
            return;

        if( mAudioManager.abandonAudioFocus(mAudioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ) {
            Log.e(TAG, "releaseAudioFocus(): Release granted.");
            mHasAudioFocus = false;
        } else {
            Log.e(TAG, "releaseAudioFocus(): Release denied.");
        }
    }

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int i) {
            switch (i) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.e(TAG, "AudioFocusChange(): GAIN");
                    if( mPlayer == null ) {
                        Log.e(TAG, "AudioFocusChange(): Player is null -- preparing.");

                        preparePlayer();
                    }

                    registerMediaButtonReceiver();
                    play();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.e(TAG, "AudioFocusChange(): LOSS");
                    deregisterMediaButtonReceiver();

                    if( mPlayer == null ) {
                        Log.e(TAG, "AudioFocusChange(): Player is null -- returning.");
                        return;
                    }

                    pause();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.e(TAG, "AudioFocusChange(): LOSS_TRANSIENT");
                    if( mPlayer == null ) {
                        Log.e(TAG, "AudioFocusChange(): Player is null -- returning.");
                        return;
                    }

                    pause();
                    deregisterMediaButtonReceiver();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.e(TAG, "AudioFocusChange(): LOSS_TRANSIENT_CAN_DUCK");
                    if( mPlayer == null ) {
                        Log.e(TAG, "AudioFocusChange(): Player is null -- returning.");
                        return;
                    }

                    pause();
                    deregisterMediaButtonReceiver();
                    break;

                default:
                    Log.e(TAG, "AudioFocusChange(): Unknown: "+i);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Interface: MediaButtonEventReceiver


    private void registerMediaButtonReceiver() {
        if( mMediaButtonReceiverRegistered )
            return;

        Log.e(TAG, "registerMediaButtonReceiver(): Registered");
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverName);
        registerReceiver(mMediaControlReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
        mMediaButtonReceiverRegistered = true;
    }

    private void deregisterMediaButtonReceiver() {
        if( !mMediaButtonReceiverRegistered )
            return;

        Log.e(TAG, "registerMediaButtonReceiver(): Deregistered");
        mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiverName);
        unregisterReceiver(mMediaControlReceiver);
        mMediaButtonReceiverRegistered = false;
    }

    private class MediaControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent == null ) {
                Log.e(TAG, "onReceive(): Null intent");
                return;
            }

            String mediaControl = intent.getStringExtra(EXTRA_MEDIA_CONTROL);
            if( mediaControl.equals(MEDIA_CONTROL_REWIND) )
                onSkipBackwardClicked();
            else if( mediaControl.equals(MEDIA_CONTROL_FAST_FORWARD) )
                onSkipForwardClicked();
            else
                Log.e(TAG, "onReceive(): Unknown media control: "+mediaControl);
        }
    }
}
