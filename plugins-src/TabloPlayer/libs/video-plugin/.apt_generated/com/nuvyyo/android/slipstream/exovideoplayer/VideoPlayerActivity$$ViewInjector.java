// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class VideoPlayerActivity$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.VideoPlayerActivity> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131492966, "field 'root'");
    target.root = finder.castView(view, 2131492966, "field 'root'");
    view = finder.findRequiredView(source, 2131492969, "field 'subtitles'");
    target.subtitles = finder.castView(view, 2131492969, "field 'subtitles'");
    view = finder.findRequiredView(source, 2131492968, "field 'playback_state_view'");
    target.playback_state_view = finder.castView(view, 2131492968, "field 'playback_state_view'");
    view = finder.findRequiredView(source, 2131492967, "field 'surface_view'");
    target.surface_view = finder.castView(view, 2131492967, "field 'surface_view'");
    view = finder.findRequiredView(source, 2131492971, "field 'mediaControlsView'");
    target.mediaControlsView = finder.castView(view, 2131492971, "field 'mediaControlsView'");
    view = finder.findRequiredView(source, 2131492970, "field 'shutterView'");
    target.shutterView = view;
  }

  @Override public void reset(T target) {
    target.root = null;
    target.subtitles = null;
    target.playback_state_view = null;
    target.surface_view = null;
    target.mediaControlsView = null;
    target.shutterView = null;
  }
}
