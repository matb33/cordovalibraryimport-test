package com.nuvyyo.tabloplayer.exoplayer.renderer;

import com.nuvyyo.tabloplayer.exoplayer.TabloExoPlayer;

/**
 * Created by mike on 01/06/15.
 */
public interface RendererBuilder {
    /**
     * Constructs the necessary components for playback.
     *
     * @param callback The callback to invoke with the constructed components.
     */
    public void buildRenderers(TabloExoPlayer player, RendererBuilderCallback callback);

    /**
     * Cancels any in-progress build operations.
     */
    public void cancel();
}
