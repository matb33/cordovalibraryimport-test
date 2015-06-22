package com.nuvyyo.cordova.plugins.tablovideoplayer;

import java.util.TimeZone;
import java.util.Arrays;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.common.images.WebImage;

import com.nuvyyo.tabloplayer.activity.PlayerActivity;
import com.nuvyyo.tabloplayer.activity.LocalPlayerActivity;
import com.nuvyyo.tabloplayer.util.MediaInfoUtil;

import com.nuvyyo.cordova.plugins.tablovideoplayer.PlayerAPK;

public class TabloVideoPlayer extends CordovaPlugin {
    public static final String TAG                      = "TabloVideoPlayer";

    private static final String PLAYBACK_POSITION       = "playbackPosition";
    private static final String MAIN_TITLE              = "mainTitle";
    private static final String SUB_TITLE               = "subTitle";
    private static final String CHANNEL_TITLE           = "channelTitle";
    private static final String POSTER                  = "poster";
    private static final String THUMBNAIL               = "thumbnail";
    private static final String IS_LIVE                 = "isLive";

    private static final String RESULT_SUCCESS          = "success";
    private static final String RESULT_MESSAGE          = "message";
    private static final String RESULT_PLAY_POSITION    = "endPosition";

    private static final String END_PLAY_POSITION       = "Nuvyyo.Slipstream.END_PLAY_POSITION";

    public static final int ACTIVITY_CODE_PLAY_VIDEO    = 10001;

    private JSONArray args;
    private CallbackContext callbackContext;
    private CordovaPlugin plugin;

    final private JSONObject resultObject = new JSONObject();

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.plugin = this;
        this.args = args;
        this.callbackContext = callbackContext;

        if (action.equals("play")) {
            playVideo();
            return true;
        }

        return false;
    }

    public void playVideo() {
        try {
            final String VAL_PLAYLIST_URL = args.getString(0);

            JSONObject options = args.getJSONObject(1);

            final int VAL_PLAY_POSITION = options.has(PLAYBACK_POSITION)
                    ? options.getInt(PLAYBACK_POSITION)
                    : -1;

            final String VAL_MAIN_TITLE     = options.has(MAIN_TITLE) ? options.getString(MAIN_TITLE) : "";
            final String VAL_SUB_TITLE      = options.has(SUB_TITLE) ? options.getString(SUB_TITLE) : "";
            final String VAL_CHANNEL_TITLE  = options.has(CHANNEL_TITLE) ? options.getString(CHANNEL_TITLE) : "";
            final String VAL_POSTER         = options.has(POSTER) ? options.getString(POSTER) : "";
            final String VAL_THUMBNAIL      = options.has(THUMBNAIL) ? options.getString(THUMBNAIL) : "";
            final boolean VAL_IS_LIVE       = options.has(IS_LIVE) ? options.getBoolean(IS_LIVE) : false;

            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override public void run() {
                    // Intent intent = new Intent(cordova.getActivity().getApplicationContext(), VideoPlayerActivity.class);
                    // intent.putExtra(Extras.PLAYLIST_URL, VAL_PLAYLIST_URL);
                    // intent.putExtra(Extras.PLAY_POSITION_IN_SECONDS, VAL_PLAY_POSITION);
                    // intent.putExtra(Extras.TITLE, VAL_MAIN_TITLE);
                    // intent.putExtra(Extras.SUBTITLE, VAL_SUB_TITLE);
                    // intent.putExtra(Extras.CHANNEL, VAL_CHANNEL_TITLE);
                    // intent.putExtra(Extras.COVER_IMAGE_URL, VAL_POSTER);
                    // intent.putExtra(Extras.THUMB_IMAGE_URL, VAL_THUMBNAIL);
                    // intent.putExtra(Extras.IS_LIVE_STREAM, VAL_IS_LIVE);

                    Context context = cordova.getActivity().getApplicationContext();
                    Intent intent = getLaunchPlayerIntent(context, VAL_PLAYLIST_URL, VAL_PLAY_POSITION, VAL_IS_LIVE, VAL_MAIN_TITLE, VAL_SUB_TITLE, VAL_THUMBNAIL, VAL_POSTER, "");
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    Log.d(TAG, "Intent created: " + intent.toString());
                    Bundle bundle = intent.getExtras();
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        Log.d(TAG, "Intent extra: " + String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                    }
                    Log.d(TAG, "Starting activity...");

                    cordova.startActivityForResult(plugin, intent, ACTIVITY_CODE_PLAY_VIDEO);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "There was a problem invoking the player intent. " + e.getMessage());
            e.printStackTrace();
        }
    }


    private Intent getLaunchPlayerIntent(Context context, String playlistUrl, int position, boolean isLive, String title, String subtitle, String coverUrl, String backgroundUrl, String snapshotsJson) {
        Intent intent = new Intent(context, LocalPlayerActivity.class);

        final MediaInfo info = buildMediaInfo(playlistUrl, isLive, title, subtitle, coverUrl, backgroundUrl, snapshotsJson);

        intent.putExtra(PlayerActivity.EXTRA_MEDIA_INFO,          MediaInfoUtil.mediaInfoToBundle(info));
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_LIVE_STREAM,   isLive);
        intent.putExtra(PlayerActivity.EXTRA_PLAY_POSITION,       position);
        return intent;
    }

    private MediaInfo buildMediaInfo(String playlistUrl, boolean isLive, String title, String subtitle, String coverUrl, String backgroundUrl, String snapshotsJson) {
        final int streamType = (isLive) ? MediaInfo.STREAM_TYPE_LIVE : MediaInfo.STREAM_TYPE_BUFFERED;

        MediaTrack track = new MediaTrack.Builder(0, MediaTrack.TYPE_VIDEO)
                .setContentType(PlayerActivity.HLS_MIME_TYPE)
                .setContentId(playlistUrl)
                .build();

        MediaMetadata metadata = new MediaMetadata();
        metadata.putString(MediaMetadata.KEY_TITLE,         title);
        metadata.putString(MediaMetadata.KEY_SUBTITLE,      subtitle);
        metadata.putString(MediaInfoUtil.KEY_SNAPSHOT_JSON, snapshotsJson);

        metadata.addImage( new WebImage(Uri.parse(coverUrl)) );
        metadata.addImage( new WebImage(Uri.parse(backgroundUrl)) );
        metadata.addImage( new WebImage(Uri.parse("")) ); // Not Used.

        MediaInfo mediaInfo = new MediaInfo.Builder(playlistUrl)
                .setMediaTracks(Arrays.asList(new MediaTrack[]{ track }))
                .setContentType(PlayerActivity.HLS_MIME_TYPE)
                .setStreamType(streamType)
                .setMetadata(metadata)
                .build();

        return mediaInfo;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ACTIVITY_CODE_PLAY_VIDEO) {
            return;
        }

        int playPosition = 0;
        if (data != null && data.hasExtra(END_PLAY_POSITION))
            playPosition = data.getIntExtra(END_PLAY_POSITION, 0);

        switch (resultCode) {
            case Activity.RESULT_OK:
                sendResult_OK(playPosition);
                break;
            default:
                sendResult_ERROR("Video Player Error, resultCode: " + resultCode, playPosition);
        }


        // String result = "SUCCESS";
        // if (resultCode != Activity.RESULT_OK) {
        //     result = "ERROR";
        // }

        // String message = "No data returned.";
        // int returnPos = 0;

        // if (data != null) {
        //     Bundle bundle = data.getExtras();
        //     if (bundle != null) {
        //         message   = bundle.getString(Extras.RETURN_MESSAGE, "No message returned.");
        //         returnPos = bundle.getInt(Extras.END_PLAY_POSITION);
        //     }
        // }

        // if (playVideoContext != null) {
        //     JSONObject r = new JSONObject();
        //     try {
        //         r.put(Extras.RETURN_MESSAGE, message);
        //         r.put(Extras.END_PLAY_POSITION, returnPos);
        //         playVideoContext.success(r);
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //         playVideoContext.error(r);
        //     }

        //     // Invalidate context
        //     playVideoContext = null;
        // }
    }

    public void sendResult_ERROR(final String message, final int playPosition) {
        try {
            resultObject.put(RESULT_SUCCESS, false);
            resultObject.put(RESULT_MESSAGE, message);
            if (playPosition > 0) {
                resultObject.put(RESULT_PLAY_POSITION, playPosition / 1000);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        sendResult(PluginResult.Status.ERROR);
    }

    public void sendResult_OK(final int playPosition) {
        try {
            resultObject.put(RESULT_SUCCESS, true);
            resultObject.put(RESULT_PLAY_POSITION, playPosition / 1000);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        sendResult(PluginResult.Status.OK);
    }

    public void sendResult_OK(final String message) {
        try {
            resultObject.put(RESULT_SUCCESS, true);
            resultObject.put(RESULT_MESSAGE, message);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        sendResult(PluginResult.Status.OK);
    }

    private void sendResult(final PluginResult.Status status) {
        if (this.callbackContext != null) {
            this.callbackContext.sendPluginResult(new PluginResult(status, resultObject));
        }
    }

    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        handlePlayerAPK(cordova.getActivity());
    }

    private boolean mPlayerApkWasInstalled;
    private void handlePlayerAPK(Activity activity) {
        if (!mPlayerApkWasInstalled && !PlayerAPK.isProtected(activity) && PlayerAPK.isInstalled(activity)) {
            // Player is installed and not protected.
            // We must attempt to remove it.
            PlayerAPK.uninstallPlayerAPK(activity);
            mPlayerApkWasInstalled = true;
        } else if (mPlayerApkWasInstalled && PlayerAPK.isInstalled(activity)) {
            // Player apk was installed on launch, and is still installed.
            // This indicates the user opted _not_ to remove it. We
            // will now mark it protected so we don't try to remove it again.
            PlayerAPK.setProtected(activity, true);
            mPlayerApkWasInstalled = false;
        } else if (mPlayerApkWasInstalled && !PlayerAPK.isInstalled(activity)) {
            // Player apk was installed on launch, and is no longer installed.
            // We will display a dialog to the user indicating success.
            mPlayerApkWasInstalled = false;
            PlayerAPK.showCompleteDialog(activity);
        }
    }
}