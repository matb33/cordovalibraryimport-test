package com.nuvyyo.tabloplayer.exoplayer.hls;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.text.eia608.Eia608Parser;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.nuvyyo.tabloplayer.exoplayer.TabloExoPlayer;
import com.nuvyyo.tabloplayer.exoplayer.renderer.RendererBuilder;
import com.nuvyyo.tabloplayer.exoplayer.renderer.RendererBuilderCallback;

import java.io.IOException;

/**
 * Created by mike on 01/06/15.
 */
public class HLSRendererBuilder implements RendererBuilder, ManifestFetcher.ManifestCallback<HlsPlaylist> {
    private static final String TAG = "HLSRendererBuilder";

    private static final int BUFFER_SEGMENT_SIZE = 256 * 1024;
    private static final int BUFFER_SEGMENTS     = 64;

    private final Context mContext;
    private final String mUserAgent;
    private final String mPlaylistUrl;
    private final String mContentId;
    private final AudioCapabilities mAudioCapabilities;

    private TabloExoPlayer mPlayer;
    private RendererBuilderCallback mCallback;
    private boolean mCanceled;

    public HLSRendererBuilder(Context context, String userAgent, String playlistUrl, String contentId, AudioCapabilities audioCapabilities) {
        assert( playlistUrl != null );

        mContext           = context;
        mPlaylistUrl       = playlistUrl;
        mUserAgent         = userAgent;
        mContentId         = contentId;
        mAudioCapabilities = audioCapabilities;

        Log.v(TAG, "HLSRendererBuilder(): playlistUrl = "+mPlaylistUrl);
    }

    @Override
    public void buildRenderers(TabloExoPlayer player, RendererBuilderCallback callback) {
        assert( mPlayer == null );

        mCanceled = false;
        mCallback = callback;
        mPlayer   = player;

        // Setup a parser and data source we can use to download
        // the playlist.
        HlsPlaylistParser playlistParser = new HlsPlaylistParser();
        HttpDataSource    playlistSource = new DefaultHttpDataSource(mUserAgent, null);

        // Setup a ManifestFetcher to use our parser and data source.
        ManifestFetcher<HlsPlaylist> playlistFetcher = new ManifestFetcher<HlsPlaylist>(mPlaylistUrl, playlistSource, playlistParser);
        playlistFetcher.singleLoad(Looper.getMainLooper(), this);
    }

    @Override
    public void cancel() {
        mCanceled = true;
        mPlayer = null;
        mCallback = null;
    }

    @Override
    public void onSingleManifest(HlsPlaylist manifest) {
        if( mCanceled ) {
            Log.w(TAG, "onSingleManifest(): Canceled. ("+manifest.baseUri+")");
            return;
        }

        Looper  mainLooper  = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));

        // TODO: Decouple constants: { TRACK_TYPE*, TRACK_RENDERER_COUNT }
        DataSource dataSource = new DefaultUriDataSource(mContext, bandwidthMeter, mUserAgent);
        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, mPlaylistUrl, manifest, bandwidthMeter, null, HlsChunkSource.ADAPTIVE_MODE_ABRUPT, mAudioCapabilities);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl, BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, mPlayer, 0);

        MediaCodecVideoTrackRenderer videoTrackRenderer = new MediaCodecVideoTrackRenderer( sampleSource,
                                                                                            MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                                                                                            5000,
                                                                                            mainHandler,
                                                                                            mPlayer,
                                                                                            50 );

        MediaCodecAudioTrackRenderer audioTrackRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
        Eia608TrackRenderer closedCaptionRenderer = new Eia608TrackRenderer(sampleSource, mPlayer, mainHandler.getLooper());

        TrackRenderer[] renderers = new TrackRenderer[TabloExoPlayer.TRACK_RENDERER_COUNT];
        renderers[TabloExoPlayer.TRACK_TYPE_VIDEO] = videoTrackRenderer;
        renderers[TabloExoPlayer.TRACK_TYPE_AUDIO] = audioTrackRenderer;
        renderers[TabloExoPlayer.TRACK_TYPE_TEXT]  = closedCaptionRenderer;

        if( mCanceled ) {
            Log.w(TAG, "onSingleManifest(): Canceled. ("+manifest.baseUri+")");
            return;
        }
        mCallback.onRenderers(null, null, renderers);
    }

    @Override
    public void onSingleManifestError(IOException e) {
        if( mCanceled ) {
            Log.w(TAG, "onSingleManifestError(): Canceled. ("+e+")");
            return;
        }
        mCallback.onRenderersError(e);
    }
}
