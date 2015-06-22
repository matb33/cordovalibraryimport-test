package com.nuvyyo.tabloplayer.exoplayer.renderer;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;

/**
 * Created by mike on 01/06/15.
 */
public interface RendererBuilderCallback {
    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param trackNames        The names of the available tracks, indexed by {@link ExoPlayer} TYPE_*
     *                          constants. May be null if the track names are unknown. An individual element may be null
     *                          if the track names are unknown for the corresponding type.
     * @param multiTrackSources Sources capable of switching between multiple available tracks,
     *                          indexed by {@link ExoPlayer} TYPE_* constants. May be null if there are no types with
     *                          multiple tracks. An individual element may be null if it does not have multiple tracks.
     * @param renderers         Renderers indexed by {@link ExoPlayer} TYPE_* constants. An individual
     *                          element may be null if there do not exist tracks of the corresponding type.
     */
    public void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers);

    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    public void onRenderersError(Exception e);
}
