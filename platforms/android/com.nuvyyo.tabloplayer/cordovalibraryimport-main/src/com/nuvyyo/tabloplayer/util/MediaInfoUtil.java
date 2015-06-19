package com.nuvyyo.tabloplayer.util;

import android.os.Bundle;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

/**
 * Created by mike on 11/06/15.
 */
public class MediaInfoUtil {
    public static final String KEY_SNAPSHOT_JSON = "SnapshotJson";

    public static MediaInfo bundleToMediaInfo(Bundle bundle) {
        MediaInfo info = Utils.bundleToMediaInfo(bundle);

        info.getMetadata().putString(KEY_SNAPSHOT_JSON, bundle.getString(KEY_SNAPSHOT_JSON));
        return info;
    }

    public static Bundle mediaInfoToBundle(MediaInfo mediaInfo) {
        // Use CastCompanionLibrary utility.
        Bundle bundle = Utils.mediaInfoToBundle(mediaInfo);

        // Put our extra data.
        String json = mediaInfo.getMetadata().getString(KEY_SNAPSHOT_JSON);
        bundle.putString(KEY_SNAPSHOT_JSON, mediaInfo.getMetadata().getString(KEY_SNAPSHOT_JSON));

        return bundle;
    }
}
