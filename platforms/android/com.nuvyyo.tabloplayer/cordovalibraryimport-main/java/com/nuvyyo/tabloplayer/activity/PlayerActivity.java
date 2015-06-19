package com.nuvyyo.tabloplayer.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuvyyo.tabloplayer.DateTimeUtil;
import com.nuvyyo.tabloplayer.R;
import com.nuvyyo.tabloplayer.bif.SnapshotGrid;
import com.nuvyyo.tabloplayer.bif.SnapshotView;
import com.nuvyyo.tabloplayer.captions.ClosedCaptionView;
import com.nuvyyo.tabloplayer.cast.CastManager;
import com.nuvyyo.tabloplayer.util.MediaInfoUtil;

import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action1;

public abstract class PlayerActivity extends FragmentActivity {

    private static final String TAG = "PlayerActivity";

    public static final String HLS_MIME_TYPE       = "application/x-mpegURL";

    public static final String EXTRA_MEDIA_INFO       = VideoCastManager.EXTRA_MEDIA;
    public static final String EXTRA_PLAY_POSITION    = VideoCastManager.EXTRA_START_POINT;
    public static final String EXTRA_SHOULD_START     = VideoCastManager.EXTRA_SHOULD_START;
    public static final String EXTRA_PLAYER_PAUSED    = "PlayerPaused";
    public static final String EXTRA_CAPTIONS_ENABLED = "ClosedCaptionsEnabled";

    public static final String EXTRA_MEDIA_LIVE_STREAM = "MediaLiveStream";

    protected static final long LONG_PRESS_INTERVAL_MS     = 750L;
    protected static final long INTERVAL_FROM_LIVE_EDGE_MS = 15000L;

    protected static final int QUICK_SKIP_FORWARD_MS  = 30000;
    protected static final int QUICK_SKIP_BACKWARD_MS = -20000;

    protected static final int BUMPER_SKIP_MULTIPLIER = 2;

    protected static final int HIDE_CONTROLS_DELAY_MS = 10000;
    protected static final float CAST_VOLUME_INCREMENT = 0.05f;

    private Handler mHandler;

    private UiVisibilityManager mVisibilityManager;

    private ViewGroup mRootContainer;
    private ViewGroup mMainContentContainer;
    private ViewGroup mFullScreenContentContainer;

    private SurfaceView mVideoSurfaceView;
    private View mVideoSurfaceDimmerView;
    private View mSpinner;
    private ClosedCaptionView mClosedCaptionView;

    private ViewGroup mPlayerControlsContainer;

    private ImageButton mPlayPauseButton;
    private ImageButton mSkipForwardButton;
    private ImageButton mSkipBackwardButton;

    private TextView mPlayPositionLabel;
    private TextView mPlayDurationLabel;
    private SnapshotView mSnapshotView;
    private TextView mClosedCaptionButton;

    private SeekBar mSeekBar;

    private ViewPropertyAnimator mPlayerControlsAnimator;
    private ViewPropertyAnimator mVideoSurfaceDimmerAnimator;

    private MediaRouteButton mMediaRouteButton;
    private boolean mUserSeekInProgress;
    private long mUserSeekStartTime;
    private long mDpadCenterKeyDownTime;

    private MediaInfo mMediaInfo;
    private boolean mDialogIsVisible;


    // ------------------------------------------------------------------------
    // Cast

    private CastManager mCastManager;
    private Subscription mCastAvailabilitySubscription;
    private Subscription mCastApplicationRunningSubscription;
    private Subscription mCastDeviceConnectedSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Thread Handler
        mHandler = new Handler(Looper.getMainLooper());

        // Main Containers
        mRootContainer              = (ViewGroup)findViewById(R.id.rootContainer);
        mMainContentContainer       = (ViewGroup)findViewById(R.id.mainContentContainer);
        mFullScreenContentContainer = (ViewGroup)findViewById(R.id.fullScreenContentContainer);

        // Video Surface
        mVideoSurfaceView           = (SurfaceView)findViewById(R.id.videoSurfaceView);
        mVideoSurfaceDimmerView     = (View)findViewById(R.id.videoSurfaceDimmerView);

        // Captions
        mClosedCaptionView          = (ClosedCaptionView)findViewById(R.id.closedCaptionView);
        mClosedCaptionButton        = (TextView)findViewById(R.id.btnClosedCaption);
        mClosedCaptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClosedCaptionButtonClicked();
            }
        });

        // Hide toggle button on leanback devices.
        if( isLeanbackDevice() )
            mClosedCaptionButton.setVisibility(View.GONE);

        // Snapshot Renderer
        mSnapshotView               = (SnapshotView)findViewById(R.id.snapshotView);
        mSnapshotView.setVisibility(View.GONE);

        // Controls
        mPlayerControlsContainer = (ViewGroup)findViewById(R.id.playerControls);

        mPlayPauseButton = (ImageButton)findViewById(R.id.btnPlayPause);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlayPauseClicked();
            }
        });

        mSkipForwardButton = (ImageButton)findViewById(R.id.btnSkipForwards);
        mSkipForwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onSkipForwardClicked();
            }
        });

        mSkipBackwardButton= (ImageButton)findViewById(R.id.btnSkipBackwards);
        mSkipBackwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onSkipBackwardClicked();
            }
        });

        // Hide quick skip buttons on leanback devices.
        if( isLeanbackDevice() ) {
            mSkipBackwardButton.setVisibility(View.GONE);
            mSkipForwardButton.setVisibility(View.GONE);
        }

        mPlayPositionLabel = (TextView)findViewById(R.id.lblVideoPosition);
        mPlayDurationLabel = (TextView)findViewById(R.id.lblVideoDuration);
        mSeekBar           = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onSeekProgressChanged(seekBar, progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onSeekStartTrackingTouch(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onSeekStopTrackingTouch(seekBar);
            }

        });

        // Set up Visibility Manager
        mVisibilityManager = new UiVisibilityManager(getWindow(), mRootContainer);
        mVisibilityManager.setOnVisibilityChangeListener(new UiVisibilityManager.OnVisibilityChangeListener(){

            @Override
            public void willShowSystemUi() {
                PlayerActivity.this.willShowSystemUi();
            }

            @Override
            public void willHideSystemUi() {
                PlayerActivity.this.willHideSystemUi();
            }

        });

        mSpinner = (View)findViewById(R.id.spinner);
        mFullScreenContentContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onFullScreenContentContainerClicked();
            }
        });

        // If we're running on a non-leanback device (i.e. tablet/phone), we must
        // initialise chromecast. Otherwise, it's not required.
        if( !isLeanbackDevice() ) {
            mMediaRouteButton = (MediaRouteButton) findViewById(R.id.mediaRouteButton);

            // Check if cast is supported.
            BaseCastManager.checkGooglePlayServices(this);

            // Setup the cast manager.
            initCastManager();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        incrementCastUiCounter();
        updateSnapshotViewPosition();
    }

    @Override
    public void onPause() {
        decrementCastUiCounter();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Check if the cast manager is null before trying to
        // unsubscribe from it's observables.
        //
        // For Leanback devices, the cast manager will always be null
        // since they don't act as cast senders.
        if( mCastManager != null ) {
            mCastAvailabilitySubscription.unsubscribe();
            mCastApplicationRunningSubscription.unsubscribe();
            mCastDeviceConnectedSubscription.unsubscribe();
        }
    }

    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        if( isLeanbackDevice() )
            return dispatchLeanbackKeyEvent(event);

        // Send volume increments to cast manager.
        if( mCastManager.onDispatchVolumeKeyEvent(event, CAST_VOLUME_INCREMENT) )
            return true;

        return super.dispatchKeyEvent(event);
    }

    private boolean dispatchLeanbackKeyEvent(final KeyEvent event) {
        // A button was pressed -- lets show the controls.
        mVisibilityManager.showSystemUi();

        int keyCode = event.getKeyCode();

        // Handle single-shot key presses.
        if( event.getAction() == KeyEvent.ACTION_DOWN ) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_SPACE:
                    onPlayPauseClicked();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    play();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    pause();
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if( mDpadCenterKeyDownTime <= 0 )
                        mDpadCenterKeyDownTime = System.nanoTime();

                    long timeButtonWasPressedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mDpadCenterKeyDownTime);
                    if( timeButtonWasPressedMs >= LONG_PRESS_INTERVAL_MS ) {
                        handleCenterLongPress();
                    }

                    return true; // Intercept this event to prevent
                                 // selecting the active view onKeyDown.
            }
        } else if( event.getAction() == KeyEvent.ACTION_UP &&
                   keyCode == KeyEvent.KEYCODE_DPAD_CENTER ) {

            // If dpad center time is 0, we are not releasing a long press.
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mDpadCenterKeyDownTime);
            if( elapsedMs < LONG_PRESS_INTERVAL_MS )
                onPlayPauseClicked();

            // Reset press time.
            mDpadCenterKeyDownTime = 0;
            return true;
        }

        // Handle D-Pad Events.
        if( isSeekKeyEvent(event) ) {
            int action = event.getAction();

            if( action == KeyEvent.ACTION_DOWN ) {
                if( !isUserSeekInProgress() )
                    startUserSeek();

                long amount = 10000L;
                switch(keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_BUTTON_L1:
                        amount = QUICK_SKIP_BACKWARD_MS;
                        break;

                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_BUTTON_R1:
                        amount = QUICK_SKIP_FORWARD_MS;
                        break;
                }

                if( isBumperKeyEvent(event) )
                    amount *= BUMPER_SKIP_MULTIPLIER;

                if( timeSinceUserSeekInitiatedNs() >= TimeUnit.MILLISECONDS.toNanos(1400) )
                    amount *= 4;

                // Update the seekbar with the new position.
                SeekBar seekBar = getSeekBar();
                long position = Math.min(seekBar.getProgress() + amount, getDuration());
                seekBar.setProgress((int) position);
            }

            if( action == KeyEvent.ACTION_UP && isUserSeekInProgress() )
                finishUserSeek();

            // We handled the key event.
            return true;
        }

        // Allow system to handle the event.
        return super.dispatchKeyEvent(event);
    }

    private boolean isSeekKeyEvent(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT  ||
               keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
               keyCode == KeyEvent.KEYCODE_BUTTON_L1  ||
               keyCode == KeyEvent.KEYCODE_BUTTON_R1;
    }

    private boolean isBumperKeyEvent(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
               keyCode == KeyEvent.KEYCODE_BUTTON_R1;
    }

    @TargetApi(21)
    private void handleCenterLongPress() {
        if( mDialogIsVisible )
            return;

        mDialogIsVisible = true;

        View checkBoxView = View.inflate(this, R.layout.view_leanback_dialog, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.chkClosedCaptions);
        checkBox.setChecked( isClosedCaptionsEnabled() );
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setClosedCaptionsEnabled(isChecked);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(checkBoxView)
               .setCancelable(true)
               .setOnDismissListener(new DialogInterface.OnDismissListener() {
                   @Override
                   public void onDismiss(DialogInterface dialog) {
                       mDialogIsVisible = false;
                   }
               }).show();
    }

    private void incrementCastUiCounter() {
        // Check if the cast manager is null. This will be the case on
        // leanback devices, since they don't act as cast senders.
        if( mCastManager == null ) {
            Log.w(TAG, "incrementCastUiCounter(): Cast Manager is null.");
            return;
        }

        Log.d(TAG, "incrementCastUiCounter(): incrementUiCounter");
        mCastManager.incrementUiCounter();
    }

    private void decrementCastUiCounter() {
        // Check if the cast manager is null. This will be the case on
        // leanback devices, since they don't act as cast senders.
        if( mCastManager == null ) {
            Log.w(TAG, "decrementCastUiCounter(): Cast Manager is null.");
            return;
        }

        Log.d(TAG, "decrementCastUiCounter(): decrementUiCounter");
        mCastManager.decrementUiCounter();
    }

    protected void setMediaInfo(MediaInfo mediaInfo) {
        mMediaInfo = mediaInfo;

        // Pull out snapshot grid if possible.
        String snapshotJson = mMediaInfo.getMetadata().getString(MediaInfoUtil.KEY_SNAPSHOT_JSON);
        if( snapshotJson != null )
            setSnapshotGridFromJson(snapshotJson);
        else
            Log.w(TAG, "setMediaInfo(): snapshot grid is null");
    }

    private void seekToInternal(long position) {
        // If this is a live stream, we will guard against seeking
        // to the _real_ end of the stream.
        if( isLiveStream() ) {
            long virtualLiveEdge = getDuration() - INTERVAL_FROM_LIVE_EDGE_MS;
            position = Math.min(position, virtualLiveEdge);
        }

        seekTo(position);
    }

    protected void seekToLive() {
        seekToInternal( getDuration() );
    }

    protected MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    protected Handler getMainHandler() {
        return mHandler;
    }

    protected abstract boolean isLiveStream();

    protected abstract void seekTo(long position);

    protected abstract long getPosition();

    protected abstract long getDuration();

    protected abstract void play();

    protected abstract void pause();

    protected abstract boolean isClosedCaptionsEnabled();

    protected abstract void setClosedCaptionsEnabled(boolean enabled);

    protected SurfaceView getVideoSurfaceView() {
        return mVideoSurfaceView;
    }

    protected ImageButton getPlayPauseButton() {
        return mPlayPauseButton;
    }

    protected ImageButton getSkipForwardButton() {
        return mSkipForwardButton;
    }

    protected ImageButton getSkipBackwardButton() {
        return mSkipBackwardButton;
    }

    protected TextView getPlayPositionLabel() {
        return mPlayPositionLabel;
    }

    protected TextView getPlayDurationLabel() {
        return mPlayDurationLabel;
    }

    protected UiVisibilityManager getUiVisibilityManager() {
        return mVisibilityManager;
    }

    protected CastManager getCastManager() {
        return mCastManager;
    }

    protected SeekBar getSeekBar() {
        return mSeekBar;
    }

    protected ClosedCaptionView getClosedCaptionView() {
        return mClosedCaptionView;
    }

    protected TextView getClosedCaptionButton() {
        return mClosedCaptionButton;
    }

    protected void startUserSeek() {
        mUserSeekInProgress = true;
        mUserSeekStartTime  = System.nanoTime();

        if( mSnapshotView.hasSnapshots() )
            mSnapshotView.setVisibility(View.VISIBLE);
    }

    protected boolean isUserSeekInProgress() {
        return mUserSeekInProgress;
    }

    protected long timeSinceUserSeekInitiatedNs() {
        if( mUserSeekStartTime == 0 )
            return 0;

        return System.nanoTime() - mUserSeekStartTime;
    }

    protected void finishUserSeek() {
        if( mUserSeekInProgress ) {
            // Seek to the currently displayed position.
            long seekPoint = getSeekBar().getProgress();

            // We will get the seek position from the snapshot view
            // if possible. This will guarantee the first frame will
            // match the snapshot.
            if( mSnapshotView.hasSnapshots() && mSnapshotView.getCurrentTimestampMs() > -1 )
                seekPoint = mSnapshotView.getCurrentTimestampMs();

            Log.d(TAG, "finishUserSeek(): seekTo = "+seekPoint);

            // Perform seek.
            seekToInternal(seekPoint);
        }

        mUserSeekInProgress = false;
        mUserSeekStartTime  = 0;
        mSnapshotView.setVisibility(View.GONE);
    }

    protected void onPlayPauseClicked() {

    }

    protected void onSkipForwardClicked() {

    }

    protected void onSkipBackwardClicked() {

    }

    protected void onSeekProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        long seekBarProgress = seekBar.getProgress();
        long seekBarMax      = seekBar.getMax();

        String position = DateTimeUtil.DURATION_FORMATTER_NUMERIC_SHORT.format(seekBarProgress);
        String duration = DateTimeUtil.DURATION_FORMATTER_NUMERIC_SHORT.format(seekBarMax);

        // On screen rotations, this listener will be called _before_
        // the player has a chance to initialise.
        if( isLiveStream() && (seekBarMax - seekBarProgress <= INTERVAL_FROM_LIVE_EDGE_MS * 2) )
            duration = "Live";

        getPlayPositionLabel().setText(position);
        getPlayDurationLabel().setText(duration);

        mSnapshotView.setTimestampMs(seekBarProgress);
        updateSnapshotViewPosition();
    }

    private void updateSnapshotViewPosition() {
        SeekBar seekBar = getSeekBar();
        float seekBarWidth = seekBar.getWidth();

        float scrubberRelX = (seekBarWidth * ((float)seekBar.getProgress() / seekBar.getMax()));
        float scrubberX    = seekBar.getX() + scrubberRelX;

        float snapshotPositionX = scrubberX - (mSnapshotView.getWidth() / 2.f) + seekBar.getThumbOffset();

        mSnapshotView.setX(snapshotPositionX);
    }

    protected void onSeekStartTrackingTouch(SeekBar seekBar) {

    }

    protected void onSeekStopTrackingTouch(SeekBar seekBar) {

    }

    protected void onClosedCaptionButtonClicked() {

    }

    protected void onFullScreenContentContainerClicked() {
        mVisibilityManager.toggleSystemUiVisible();
    }

    protected void willShowSystemUi() {
        showPlayerControls();
    }

    protected void willHideSystemUi() {
        hidePlayerControls();
    }

    protected void showPlayerControls() {
        if( mPlayerControlsAnimator != null )
            mPlayerControlsAnimator.cancel();

        mPlayerControlsAnimator = mMainContentContainer
                .animate()
                .alpha(1.0f)
                .setInterpolator(new LinearInterpolator())
                .withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        mMainContentContainer.setVisibility(View.VISIBLE);
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerControlsAnimator = null;
                    }
                });
    }

    protected void hidePlayerControls() {
        if( mPlayerControlsAnimator != null )
            mPlayerControlsAnimator.cancel();

        mPlayerControlsAnimator = mMainContentContainer
                .animate()
                .alpha(0.0f)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mMainContentContainer.setVisibility(View.GONE);
                        mPlayerControlsAnimator = null;
                    }
                });
    }

    protected void showSpinner() {
        if( mVideoSurfaceDimmerAnimator != null )
            mVideoSurfaceDimmerAnimator.cancel();

        mVideoSurfaceDimmerAnimator = mVideoSurfaceDimmerView
                .animate()
                .alpha(1.0f)
                .setDuration(200L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mVideoSurfaceDimmerAnimator = null;
                    }
                });

    }

    protected void hideSpinner() {
        if( mVideoSurfaceDimmerAnimator != null )
            mVideoSurfaceDimmerAnimator.cancel();

        mVideoSurfaceDimmerAnimator = mVideoSurfaceDimmerView
                .animate()
                .alpha(0.0f)
                .setDuration(200L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mVideoSurfaceDimmerAnimator = null;
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean isLeanbackDevice() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
               getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    protected void setSnapshotGrid(SnapshotGrid grid) {
        Log.d(TAG, "setSnapshotGrid(): "+String.format("{%d, %d}", grid.getRows(), grid.getColumns()));
        mSnapshotView.setSnapshotGrid(grid);
    }

    protected void setSnapshotGridFromJson(String json) {
        Gson gson = new GsonBuilder().create();

        try {
            SnapshotGrid grid = gson.fromJson(json, SnapshotGrid.class);
            setSnapshotGrid(grid);
        }catch(Exception e){
            e.printStackTrace();
            Log.e(TAG, "Failed to parse snapshots from json");
        }
    }

    // ------------------------------------------------------------------------
    // Cast

    private void initCastManager() {
        Assertions.checkArgument( mCastManager == null );
        Assertions.checkState( mCastAvailabilitySubscription == null );
        Assertions.checkState( mCastApplicationRunningSubscription == null );

        mCastManager = CastManager.getInstance(getApplicationContext());
        mCastManager.configureMediaRouteButton(mMediaRouteButton);

        mCastAvailabilitySubscription = mCastManager.getCastAvailabilityObservable().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean castIsPresent) {
                Log.v(TAG, "initCastManageR(): castIsPresent = "+castIsPresent);
                int visibility = castIsPresent ? View.VISIBLE : View.GONE;
                mMediaRouteButton.setVisibility(visibility);
            }
        });

        mCastApplicationRunningSubscription = mCastManager.getApplicationRunningObservable().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean running) {
                onCastApplicationStateChanged(running);
            }
        });

        mCastDeviceConnectedSubscription = mCastManager.getDeviceConnectedObservable().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                onCastDeviceConnectionChanged(connected);
            }
        });

        mCastManager.reconnectToSessionIfPossible();
    }

    protected void onCastApplicationStateChanged(boolean castApplicationRunning) {

    }

    protected void onCastDeviceConnectionChanged(boolean connected) {

    }
}
