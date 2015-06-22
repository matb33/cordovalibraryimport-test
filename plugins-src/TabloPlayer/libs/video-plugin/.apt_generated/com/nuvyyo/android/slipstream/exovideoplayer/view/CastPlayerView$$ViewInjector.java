// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer.view;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class CastPlayerView$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.view.CastPlayerView> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131493013, "field 'btnSkipBack' and method 'skipBackward'");
    target.btnSkipBack = finder.castView(view, 2131493013, "field 'btnSkipBack'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.skipBackward();
        }
      });
    view = finder.findRequiredView(source, 2131493015, "field 'btnSkipForward' and method 'skipForward'");
    target.btnSkipForward = finder.castView(view, 2131493015, "field 'btnSkipForward'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.skipForward();
        }
      });
    view = finder.findRequiredView(source, 2131493003, "field 'image_thumb'");
    target.image_thumb = finder.castView(view, 2131493003, "field 'image_thumb'");
    view = finder.findRequiredView(source, 2131493002, "field 'text_status'");
    target.text_status = finder.castView(view, 2131493002, "field 'text_status'");
    view = finder.findRequiredView(source, 2131492999, "field 'image_layout'");
    target.image_layout = finder.castView(view, 2131492999, "field 'image_layout'");
    view = finder.findRequiredView(source, 2131493005, "field 'text_subtitle'");
    target.text_subtitle = finder.castView(view, 2131493005, "field 'text_subtitle'");
    view = finder.findRequiredView(source, 2131493000, "field 'loading_layout'");
    target.loading_layout = finder.castView(view, 2131493000, "field 'loading_layout'");
    view = finder.findRequiredView(source, 2131492981, "field 'mediaRouteButton'");
    target.mediaRouteButton = finder.castView(view, 2131492981, "field 'mediaRouteButton'");
    view = finder.findRequiredView(source, 2131493014, "field 'btnTogglePlayback' and method 'togglePlayback'");
    target.btnTogglePlayback = finder.castView(view, 2131493014, "field 'btnTogglePlayback'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.togglePlayback();
        }
      });
    view = finder.findRequiredView(source, 2131493018, "field 'text_player_name'");
    target.text_player_name = finder.castView(view, 2131493018, "field 'text_player_name'");
    view = finder.findRequiredView(source, 2131493010, "field 'seekBarTouchDelegate'");
    target.seekBarTouchDelegate = finder.castView(view, 2131493010, "field 'seekBarTouchDelegate'");
    view = finder.findRequiredView(source, 2131493011, "field 'lblTimeRemaining'");
    target.lblTimeRemaining = finder.castView(view, 2131493011, "field 'lblTimeRemaining'");
    view = finder.findRequiredView(source, 2131493009, "field 'seekBar'");
    target.seekBar = finder.castView(view, 2131493009, "field 'seekBar'");
    view = finder.findRequiredView(source, 2131493007, "field 'lblTimeElapsed'");
    target.lblTimeElapsed = finder.castView(view, 2131493007, "field 'lblTimeElapsed'");
    view = finder.findRequiredView(source, 2131493004, "field 'text_title'");
    target.text_title = finder.castView(view, 2131493004, "field 'text_title'");
    view = finder.findRequiredView(source, 2131493019, "method 'stop'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.stop();
        }
      });
  }

  @Override public void reset(T target) {
    target.btnSkipBack = null;
    target.btnSkipForward = null;
    target.image_thumb = null;
    target.text_status = null;
    target.image_layout = null;
    target.text_subtitle = null;
    target.loading_layout = null;
    target.mediaRouteButton = null;
    target.btnTogglePlayback = null;
    target.text_player_name = null;
    target.seekBarTouchDelegate = null;
    target.lblTimeRemaining = null;
    target.seekBar = null;
    target.lblTimeElapsed = null;
    target.text_title = null;
  }
}
