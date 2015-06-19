package com.nuvyyo.tabloplayer.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.common.images.WebImage;

import com.nuvyyo.tabloplayer.R;
import com.nuvyyo.tabloplayer.util.MediaInfoUtil;

import java.util.Arrays;

public class PlayerTestActivity extends Activity {
    private static final String PLAYLIST_URL_LIVE           = "http://192.168.1.183/stream/pl.m3u8?1080bphXsHjNiJyktDxJqw";
    private static final String PLAYLIST_URL_REC            = "http://192.168.1.183/stream/pl.m3u8?pQCp1ttmfCQ-66ZCUWDsSQ";
    private static final String PLAYLIST_URL_BIG_BUCK_BUNNY = "http://184.72.239.149/vod/smil:BigBuckBunny.smil/playlist.m3u8";

    private static final String SNAPSHOTS_JSON =
            "{\n" +
            "\t\"cols\": 6,\n" +
            "\t\"grids\": 4,\n" +
            "\t\"height\": 136,\n" +
            "\t\"width\": 240,\n" +
            "\t\"rows\": 10,\n" +
            "\t\"urlPattern\": \"http://192.168.1.183/stream/img?vA4uI4LZEWfiQk_MMZztUA&sd&{{n}}\",\n" +
            "\t\"step\": [\n" +
            "\t\t2,\n" +
            "\t\t2,\n" +
            "\t\t2,\n" +
            "\t\t3,\n" +
            "\t\t3,\n" +
            "\t\t3,\n" +
            "\t\t3,\n" +
            "\t\t4,\n" +
            "\t\t4,\n" +
            "\t\t5,\n" +
            "\t\t5,\n" +
            "\t\t6,\n" +
            "\t\t6,\n" +
            "\t\t7,\n" +
            "\t\t7,\n" +
            "\t\t8,\n" +
            "\t\t8,\n" +
            "\t\t9,\n" +
            "\t\t9,\n" +
            "\t\t10\n" +
            "\t]\n" +
            "}";

    private EditText    mPlaylistUrlView;
    private RadioGroup  mStreamTypeGroupView;

    private EditText    mStartPositionView;
    private EditText    mMediaTitleView;
    private EditText    mMediaSubtitleView;
    private EditText    mMediaCover;
    private EditText    mMediaBackground;
    private EditText    mMediaThumbnail;

    private Button      mPlayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_test);

        mPlaylistUrlView = (EditText)findViewById(R.id.txtPlaylistUrl);

        mStreamTypeGroupView = (RadioGroup)findViewById(R.id.radioGroupStreamType);
        mStreamTypeGroupView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                onStreamTypeChanged(checkedId);
            }
        });

        mStartPositionView = (EditText)findViewById(R.id.txtStartPosition);
        mMediaTitleView    = (EditText)findViewById(R.id.txtMediaTitle);
        mMediaSubtitleView = (EditText)findViewById(R.id.txtMediaSubtitle);
        mMediaCover        = (EditText)findViewById(R.id.txtMediaCover);
        mMediaBackground   = (EditText)findViewById(R.id.txtMediaBackground);
        mMediaThumbnail    = (EditText)findViewById(R.id.txtMediaThumbnail);

        mPlayButton        = (Button)findViewById(R.id.btnPlay);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayClicked();
            }
        });

        // Default to "Rec"
        mStreamTypeGroupView.check( R.id.radioButtonRec );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    // Behavior

    private void onStreamTypeChanged(int checkedId) {
        mPlaylistUrlView.setText( getUrlForStreamType(checkedId) );
    }

    private void onPlayClicked() {
        Intent intent = new Intent(this, LocalPlayerActivity.class);

        final MediaInfo info = buildMediaInfo();
        final int position   = Integer.parseInt(mStartPositionView.getText().toString());

        intent.putExtra(PlayerActivity.EXTRA_MEDIA_INFO,          MediaInfoUtil.mediaInfoToBundle(info));
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_LIVE_STREAM,   info.getStreamType() == MediaInfo.STREAM_TYPE_LIVE);
        intent.putExtra(PlayerActivity.EXTRA_PLAY_POSITION,       position);
        startActivity(intent);
    }

    private MediaInfo buildMediaInfo() {
        final String playlistUrl = mPlaylistUrlView.getText().toString();
        final boolean isLive     = mStreamTypeGroupView.getCheckedRadioButtonId() == R.id.radioButtonLive;
        final int streamType     = (isLive) ? MediaInfo.STREAM_TYPE_LIVE : MediaInfo.STREAM_TYPE_BUFFERED;

        MediaTrack track = new MediaTrack.Builder(0, MediaTrack.TYPE_VIDEO)
                .setContentType(PlayerActivity.HLS_MIME_TYPE)
                .setContentId(playlistUrl)
                .build();

        MediaMetadata metadata = new MediaMetadata();
        metadata.putString(MediaMetadata.KEY_TITLE, mMediaTitleView.getText().toString());
        metadata.putString(MediaMetadata.KEY_SUBTITLE, mMediaSubtitleView.getText().toString());
        metadata.putString(MediaInfoUtil.KEY_SNAPSHOT_JSON, null);

        metadata.addImage( new WebImage(Uri.parse(mMediaCover.getText().toString())) );
        metadata.addImage( new WebImage(Uri.parse(mMediaBackground.getText().toString())) );
        metadata.addImage( new WebImage(Uri.parse(mMediaThumbnail.getText().toString())) );

        MediaInfo mediaInfo = new MediaInfo.Builder(playlistUrl)
                .setMediaTracks(Arrays.asList(new MediaTrack[]{track}))
                .setContentType(PlayerActivity.HLS_MIME_TYPE)
                .setStreamType(streamType)
                .setMetadata(metadata)
                .build();

        return mediaInfo;
    }

    private String getUrlForStreamType(int streamTypeRadioButtonId) {
        if( streamTypeRadioButtonId == R.id.radioButtonLive ) {
            return PLAYLIST_URL_LIVE;
        }else if( streamTypeRadioButtonId == R.id.radioButtonRec ) {
                return PLAYLIST_URL_REC;
        }else if( streamTypeRadioButtonId == R.id.radioButtonBBB ) {
                return PLAYLIST_URL_BIG_BUCK_BUNNY;
        }else{
                throw new IllegalArgumentException("Invalid streamTypeButtonId: "+streamTypeRadioButtonId);
        }
    }
}
