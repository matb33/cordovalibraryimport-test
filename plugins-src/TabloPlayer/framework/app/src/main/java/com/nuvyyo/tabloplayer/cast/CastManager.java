package com.nuvyyo.tabloplayer.cast;

import android.content.Context;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.nuvyyo.tabloplayer.activity.CastPlayerActivity;

import org.json.JSONObject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * Created by mike on 05/06/15.
 */
public class CastManager extends VideoCastConsumerImpl {
    private static final String TAG               = "CastManager";
    private static final String CHROMECAST_APP_ID = "58180F3F";

    private static final int ENABLED_FEATURES     = //VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                                                    VideoCastManager.FEATURE_DEBUGGING           |
                                                    VideoCastManager.FEATURE_LOCKSCREEN          |
                                                    VideoCastManager.FEATURE_NOTIFICATION        |
                                                    VideoCastManager.FEATURE_WIFI_RECONNECT;

    private static final int RECONNECTION_TIMEOUT = 10000;


    private static CastManager _instance;
    private static boolean _videoCastManagerInitialized;

    private Context mContext;
    private boolean mDestroyed;

    private VideoCastManager mVideoCastManager;

    private BehaviorSubject<Boolean> mCastAvailabilitySubject;
    private BehaviorSubject<Boolean> mApplicationLaunchedSubject;
    private BehaviorSubject<Boolean> mApplicationRunningSubject;

    private BehaviorSubject<Boolean> mDeviceConnectedSubject;
    private BehaviorSubject<MediaMetadata> mRemotePlayerMetadataSubject;
    private BehaviorSubject<MediaStatus> mRemotePlayerStatusSubject;

    private CastManager(Context context) {
        initializeCastManagerIfNeeded(context);

        mContext = context;
        mVideoCastManager = VideoCastManager.getInstance();
        mVideoCastManager.addVideoCastConsumer(this);

        mCastAvailabilitySubject     = BehaviorSubject.create(false);
        mApplicationRunningSubject   = BehaviorSubject.create(false);
        mDeviceConnectedSubject      = BehaviorSubject.create(false);
        mRemotePlayerMetadataSubject = BehaviorSubject.create();
        mRemotePlayerStatusSubject   = BehaviorSubject.create();

        mApplicationLaunchedSubject  = BehaviorSubject.create(false);
    }

    public static CastManager getInstance(Context context) {
        Assertions.checkArgument(context != null);

        if( _instance != null && _instance.mDestroyed )
            _instance = null;

        if( _instance == null ) {
            Log.v(TAG, "getInstance(): Initialising cast manager.");
            _instance = new CastManager(context);
        }

        return _instance;
    }

    private static void initializeCastManagerIfNeeded(Context context) {
        if( _videoCastManagerInitialized ) {
            Log.w(TAG, "initializeCastManagerIfNeeded(): Cast manager already initialized.");
            return;
        }

        // Verify context != null
        Assertions.checkArgument( context != null );

        // Verify context == applicationContext
        Assertions.checkArgument( context.getApplicationContext().equals(context) );

        // Initialise with selected features.
        VideoCastManager.initialize(context, CHROMECAST_APP_ID, CastPlayerActivity.class, null)
                        .enableFeatures( ENABLED_FEATURES );

        // Done.
        _videoCastManagerInitialized = true;
    }

    public void incrementUiCounter() {
        mVideoCastManager.incrementUiCounter();
    }

    public void decrementUiCounter() {
        mVideoCastManager.decrementUiCounter();
    }

    public boolean onDispatchVolumeKeyEvent(final KeyEvent event, final float volumeIncrement) {
        return mVideoCastManager.onDispatchVolumeKeyEvent(event, volumeIncrement);
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return mVideoCastManager.getMediaRouteSelector();
    }

    public void configureMediaRouteButton(MediaRouteButton button) {
        Assertions.checkArgument( button != null );

        Log.v(TAG, "configureMediaRouteButton(): Configuring mediaRouteButton = "+button);
        mVideoCastManager.addMediaRouterButton(button);

        mVideoCastManager.stopCastDiscovery();
        mVideoCastManager.startCastDiscovery();
    }

    public void reconnectToSessionIfPossible() {
        mVideoCastManager.reconnectSessionIfPossible( RECONNECTION_TIMEOUT );
    }

    public boolean isConnected() {
        return mVideoCastManager.isConnected();
    }

    public boolean isConnecting() {
        return mVideoCastManager.isConnecting();
    }

    public void loadRemoteMedia(MediaInfo mediaInfo, boolean autoplay, long position) throws TransientNetworkDisconnectionException, NoConnectionException {
        loadRemoteMedia(mediaInfo, autoplay, position, null);
    }

    public void loadRemoteMedia(MediaInfo mediaInfo, boolean autoplay, long position, JSONObject customData) throws TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.loadMedia(mediaInfo, autoplay, (int)position, customData);
    }

    public MediaInfo getRemoteMediaInfo() throws TransientNetworkDisconnectionException, NoConnectionException {
        return mVideoCastManager.getRemoteMediaInformation();
    }

    public MediaStatus getRemoteMediaStatus() {
        return mVideoCastManager.getMediaStatus();
    }

    public long getStreamPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        return mVideoCastManager.getCurrentMediaPosition();
    }

    public long getStreamDuration() throws TransientNetworkDisconnectionException, NoConnectionException {
        return mVideoCastManager.getMediaDuration();
    }

    public void play() throws CastException, TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.play();
    }

    public void pause() throws CastException, TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.pause();
    }

    public void stop() throws CastException, TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.stop();
    }

    public void seekTo(long position) throws TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.seek((int)position);
    }

    public void togglePlayback() throws CastException, TransientNetworkDisconnectionException, NoConnectionException {
        mVideoCastManager.togglePlayback();
    }

    public Observable<Boolean> getCastAvailabilityObservable() {
        return mCastAvailabilitySubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Boolean> getApplicationLaunchedObservable() {
        return mApplicationLaunchedSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Boolean> getApplicationRunningObservable() {
        return mApplicationRunningSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Boolean> getDeviceConnectedObservable() {
        return mDeviceConnectedSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<MediaMetadata> getRemotePlayerMetadataObservable() {
        return mRemotePlayerMetadataSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<MediaStatus> getRemotePlayerStatusObservable() {
        return mRemotePlayerStatusSubject.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // ---------------------------------------------------------------------------------------------
    // Internal: VideoCastConsumer

    @Override
    public void onCastAvailabilityChanged(boolean castIsPresent) {
        super.onCastAvailabilityChanged(castIsPresent);

        mCastAvailabilitySubject.onNext(castIsPresent);
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
        super.onApplicationConnected(appMetadata, sessionId, wasLaunched);

        Log.v(TAG, "onApplicationConnected(): sessionId = "+sessionId+", wasLaunched = "+wasLaunched);

        mApplicationLaunchedSubject.onNext(wasLaunched);
        mApplicationRunningSubject.onNext(true);
    }

    @Override
    public void onApplicationDisconnected(int error) {
        super.onApplicationDisconnected(error);

        Log.v(TAG, "onApplicationDisconnected(): error = "+error);
        mApplicationRunningSubject.onNext(false);
    }

    @Override
    public void onConnectionSuspended(int reason) {
        super.onConnectionSuspended(reason);

        Log.v(TAG, "onConnectionSuspended(): reason = "+reason);
    }

    @Override
    public void onConnectivityRecovered() {
        super.onConnectivityRecovered();

        Log.v(TAG, "onConnectivityRecovered(): -");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);

        Log.v(TAG, "onConnectionFailed(): result = "+result.toString());
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();

        Log.v(TAG, "onDisconnected(): connecting = "+mVideoCastManager.isConnecting());
        mDeviceConnectedSubject.onNext(false);
    }

    @Override
    public void onConnected() {
        super.onConnected();

        Log.v(TAG, "onConnected(): connecting = "+mVideoCastManager.isConnecting());
        mDeviceConnectedSubject.onNext(true);
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
        super.onRemoteMediaPlayerMetadataUpdated();

        Log.v(TAG, "onRemoteMediaPlayerMetadataUpdate(): -");
        try {
            MediaInfo info = mVideoCastManager.getRemoteMediaInformation();
            if( info != null )
                mRemotePlayerMetadataSubject.onNext(info.getMetadata());
            else
                mRemotePlayerMetadataSubject.onNext(null);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        super.onRemoteMediaPlayerStatusUpdated();

        Log.v(TAG, "onRemoteMediaPlayerStatusUpdated(): -");
        MediaStatus status = mVideoCastManager.getRemoteMediaPlayer().getMediaStatus();
        mRemotePlayerStatusSubject.onNext(status);
    }
}
