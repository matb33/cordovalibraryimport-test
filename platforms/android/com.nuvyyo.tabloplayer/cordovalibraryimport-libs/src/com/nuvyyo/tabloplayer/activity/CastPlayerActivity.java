package com.nuvyyo.tabloplayer.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;

import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.nuvyyo.tabloplayer.R;
import com.nuvyyo.tabloplayer.cast.CastManager;
import com.nuvyyo.tabloplayer.effects.DrawBorderTransformation;
import com.nuvyyo.tabloplayer.effects.GaussianBlurTransformation;
import com.nuvyyo.tabloplayer.util.MediaInfoUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * Created by mike on 05/06/15.
 */
public class CastPlayerActivity extends PlayerActivity {
    private static final String TAG = "CastPlayerActivity";

    private RelativeLayout mCastMediaContainer;
    private TextView mTitleLabel;
    private TextView mSubtitleLabel;
    private ImageView mCoverImageView;
    private ImageView mBackgroundImageView;

    private CastManager mCastManager;

    private long mPreviousPosition;
    private boolean mShouldStartPlayback;
    private boolean mMediaIsLive;
    private boolean mCaptionsEnabled;

    private Subscription mApplicationRunningSubscription;
    private Subscription mRemotePlayerStatusSubscription;
    private Subscription mIntervalSubscription;

    private boolean mApplicationRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getVideoSurfaceView().setVisibility(View.GONE);

        mCastMediaContainer  = (RelativeLayout)findViewById(R.id.castMediaContainer);
        mCastMediaContainer.setVisibility(View.VISIBLE);

        mTitleLabel          = (TextView)findViewById(R.id.lblTitle);
        mSubtitleLabel       = (TextView)findViewById(R.id.lblSubtitle);
        mCoverImageView      = (ImageView)findViewById(R.id.imgCover);
        mBackgroundImageView = (ImageView)findViewById(R.id.imgBackground);

        mCastManager = getCastManager();

        // No closed captions on chromecast yet.
        getClosedCaptionButton().setVisibility(View.GONE);

        // Observe when the cast application is launched.
        mApplicationRunningSubscription = mCastManager.getApplicationRunningObservable().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean running) {
            Log.v(TAG, "applicationRunning(): running = "+running);

            mApplicationRunning = running;
            startOrJoinSession();
            }
        });

        // Observe player status changes.
        mRemotePlayerStatusSubscription = mCastManager.getRemotePlayerStatusObservable().subscribe(new Action1<MediaStatus>() {
            @Override
            public void call(MediaStatus mediaStatus) {
                Log.v(TAG, "remoteMediaStatus(): mediaStatus = "+mediaStatus.getPlayerState());
                Assertions.checkArgument( mediaStatus != null );

                startOrJoinSession();
                onMediaStatusUpdated(mediaStatus);
            }
        });

        // Update play position on given interval.
        mIntervalSubscription = Observable.interval(1000L, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long sequence) {
                        try {
                            long position = mCastManager.getStreamPosition();
                            long duration = mCastManager.getStreamDuration();

                            onPlayPositionChanged(position, duration);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Allow device to sleep while casting.
        getUiVisibilityManager().setKeepScreenOn(false);

        handleLaunchIntent();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        mApplicationRunningSubscription.unsubscribe();
        mRemotePlayerStatusSubscription.unsubscribe();
        mIntervalSubscription.unsubscribe();

        super.onDestroy();
    }

    @Override
    protected boolean isLiveStream() {
        return mMediaIsLive;
    }

    @Override
    protected boolean isClosedCaptionsEnabled() {
        return false;
    }

    @Override
    protected void setClosedCaptionsEnabled(boolean enabled) {
        // No-op.
    }

    private void handleLaunchIntent() {
        Intent intent = getIntent();
        if( intent == null ) {
            Log.w(TAG, "handleLaunchIntent(): intent is null.");
            return;
        }

        if( !intent.hasExtra(EXTRA_MEDIA_INFO) )
            throw new IllegalStateException("Launch intent does not contain media info.");

        setMediaInfo(MediaInfoUtil.bundleToMediaInfo(intent.getBundleExtra(EXTRA_MEDIA_INFO)));

        mPreviousPosition = intent.getLongExtra(EXTRA_PLAY_POSITION, 0L);
        mShouldStartPlayback = intent.getBooleanExtra(EXTRA_SHOULD_START, true);
        mMediaIsLive = intent.getBooleanExtra(EXTRA_MEDIA_LIVE_STREAM, false);
        mCaptionsEnabled = intent.getBooleanExtra(EXTRA_CAPTIONS_ENABLED, false);
    }

    // -----------------------------------------------------------------------
    // Internal: Player Controls

    private void onMediaStatusUpdated(MediaStatus mediaStatus) {
        int state = mediaStatus.getPlayerState();

        // Update play/pause controls.
        switch(state) {
            case MediaStatus.PLAYER_STATE_IDLE:
            case MediaStatus.PLAYER_STATE_PAUSED:
                getPlayPauseButton().setImageResource(R.drawable.button_video_player_play);
                break;

            case MediaStatus.PLAYER_STATE_PLAYING:
                getPlayPauseButton().setImageResource(R.drawable.button_video_player_pause);
                break;
        }

        // Show or hide buffering spinner.
        if( state == MediaStatus.PLAYER_STATE_BUFFERING )
            showSpinner();
        else
            hideSpinner();

        // Update Play Position.
        long position = mediaStatus.getStreamPosition();
        long duration = mediaStatus.getMediaInfo().getStreamDuration();
        onPlayPositionChanged( position, duration );
    }

    private void onPlayPositionChanged(long position, long duration) {
        Assertions.checkMainThread();

        // If user is actively manipulating the seek bar,
        // we will do nothing.
        if( isUserSeekInProgress() )
            return;

        // Update UI
        SeekBar seekBar = getSeekBar();
        seekBar.setProgress((int) position);
        seekBar.setMax((int) duration);

        // Store last known position.
        mPreviousPosition = position;
    }

    @Override
    protected void onPlayPauseClicked() {
        super.onPlayPauseClicked();

        try {
            mCastManager.togglePlayback();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onSkipBackwardClicked() {
        super.onSkipBackwardClicked();

        try {
            long currentPosition = mCastManager.getStreamPosition();
            long newPosition     = currentPosition + PlayerActivity.QUICK_SKIP_BACKWARD_MS;

            newPosition = Math.min(newPosition, mCastManager.getStreamDuration());

            mCastManager.seekTo(newPosition);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSkipForwardClicked() {
        super.onSkipBackwardClicked();

        try {
            long currentPosition = mCastManager.getStreamPosition();
            long newPosition     = currentPosition + PlayerActivity.QUICK_SKIP_FORWARD_MS;

            newPosition = Math.min(newPosition, mCastManager.getStreamDuration());

            mCastManager.seekTo(newPosition);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSeekProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onSeekProgressChanged(seekBar, progress, fromUser);

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
    protected void seekTo(long position) {
        try {
            mCastManager.seekTo(position);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void play() {
       try {
           mCastManager.play();

       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    protected void pause() {
        try {
            mCastManager.pause();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected long getPosition() {
        long position = 0L;
        try {
            position = mCastManager.getStreamPosition();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return position;
    }

    @Override
    protected long getDuration() {
        long duration = 0L;
        try {
            duration = mCastManager.getStreamDuration();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return duration;
    }

    // ------------------------------------------------------------------------
    // Internal: Cast

    private void startOrJoinSession() {
        boolean sessionJoined = attemptJoinSession();
        if( !sessionJoined )
            startNewSession();
    }

    private void startNewSession() {
        long position = mPreviousPosition;

        try {
            mCastManager.loadRemoteMedia(getMediaInfo(), true, position);

        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private boolean attemptJoinSession() {
        Log.v(TAG, "attemptJoinSession(): - ");

        MediaInfo remoteMediaInfo = null;
        MediaStatus remoteMediaStatus = null;
        try {
            remoteMediaInfo = mCastManager.getRemoteMediaInfo();
            remoteMediaStatus = mCastManager.getRemoteMediaStatus();

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        // Can't join session, no exising mediainfo.
        if( remoteMediaInfo == null || remoteMediaStatus == null )
            return false;

        Log.v(TAG, "startOrJoinSession(): mediaInfo = "+remoteMediaInfo.getContentId());
        Log.v(TAG, "startOrJoinSession(): mediaStatus = "+remoteMediaStatus.getPlayerState());

        // If content Id's are the same, the session can been joined.
        if( getMediaInfo().getContentId().equals(remoteMediaInfo.getContentId()) ) {
            // Replace the local mediaInfo with the mediaInfo
            // retrieved from remote player.
            setMediaInfo(remoteMediaInfo);

            // Edge case: It is possible the application is running and has
            //            the correct media, but is not actually playing.
            //            This seems to happen when the network connection is
            //            poor. Workaround is to start a new session -- the current
            //            position will be restored if necessary.
            if( remoteMediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE ) {
                Log.w(TAG, "attemptJoinSession(): idle reason = "+remoteMediaStatus.getIdleReason());
                Log.w(TAG, "attemptJoinSession(): Session joined but media is idle. Starting new session.");
                return false;
            }

            // Session Joined.
            return true;
        }

        // Content ID's are different, this is a request to play
        // a new video.
        return false;
    }

    @Override
    protected void setMediaInfo(MediaInfo mediaInfo) {
        super.setMediaInfo(mediaInfo);
        updateCastUi();
    }

    private void updateCastUi() {
        MediaMetadata metadata = getMediaInfo().getMetadata();

        String title    = metadata.getString(MediaMetadata.KEY_TITLE);
        String subtitle = metadata.getString(MediaMetadata.KEY_SUBTITLE);

        List<WebImage> images = metadata.getImages();
        WebImage cover      = images.get(0);
        WebImage background = images.get(1);
        WebImage thumbnail  = images.get(2);

        mTitleLabel.setText(title);
        mSubtitleLabel.setText(subtitle);

        // Load Cover Image.
        Picasso.with(this)
               .load(cover.getUrl())
               .transform(new DrawBorderTransformation())
               .into(mCoverImageView);

        // Load Background Image with blur on API 17+
        RequestCreator loader = Picasso.with(this)
                                       .load(background.getUrl());

        if( Build.VERSION.SDK_INT >= 17 )
            loader.transform(new GaussianBlurTransformation(this));

        loader.into(mBackgroundImageView);
    }

    private void finishAndLaunchLocalPlayer() {
        finish();

        long playPosition = mPreviousPosition;

        Intent intent = new Intent(this, LocalPlayerActivity.class);

        // If there is already a cast activity in the task,
        // move it to the front instead of starting another one.
        intent.putExtra(EXTRA_MEDIA_INFO, MediaInfoUtil.mediaInfoToBundle(getMediaInfo()));
        intent.putExtra(EXTRA_PLAY_POSITION, playPosition);
        intent.putExtra(EXTRA_CAPTIONS_ENABLED, mCaptionsEnabled);

        startActivity(intent);
    }

    @Override
    protected void onCastApplicationStateChanged(boolean castApplicationRunning) {
        super.onCastApplicationStateChanged(castApplicationRunning);

        Log.v(TAG, "onCastApplicationStateChanged(): castApplicationRunning = "+castApplicationRunning);
        if( !castApplicationRunning )
            finishAndLaunchLocalPlayer();
    }

    @Override
    protected void onCastDeviceConnectionChanged(boolean connected) {
        if( !connected )
            finishAndLaunchLocalPlayer();
    }
}
