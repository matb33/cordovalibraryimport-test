// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer.playercontrols;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class MobileDeviceMediaControllerView$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.playercontrols.MobileDeviceMediaControllerView> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131492972, "field 'media_controls'");
    target.media_controls = view;
    view = finder.findRequiredView(source, 2131492981, "field 'mediaRouteButton'");
    target.mediaRouteButton = finder.castView(view, 2131492981, "field 'mediaRouteButton'");
    view = finder.findRequiredView(source, 2131492978, "field 'pauseButton' and method 'playPauseClick'");
    target.pauseButton = finder.castView(view, 2131492978, "field 'pauseButton'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.playPauseClick();
        }
      });
    view = finder.findRequiredView(source, 2131492971, "field 'controller_view'");
    target.controller_view = view;
    view = finder.findRequiredView(source, 2131492976, "field 'durationLabel'");
    target.durationLabel = finder.castView(view, 2131492976, "field 'durationLabel'");
    view = finder.findRequiredView(source, 2131492975, "field 'scrubSeekBar'");
    target.scrubSeekBar = finder.castView(view, 2131492975, "field 'scrubSeekBar'");
    view = finder.findRequiredView(source, 2131492974, "field 'positionLabel'");
    target.positionLabel = finder.castView(view, 2131492974, "field 'positionLabel'");
    view = finder.findRequiredView(source, 2131492979, "method 'skipBackClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.skipBackClick();
        }
      });
    view = finder.findRequiredView(source, 2131492980, "method 'skipAheadClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.skipAheadClick();
        }
      });
  }

  @Override public void reset(T target) {
    target.media_controls = null;
    target.mediaRouteButton = null;
    target.pauseButton = null;
    target.controller_view = null;
    target.durationLabel = null;
    target.scrubSeekBar = null;
    target.positionLabel = null;
  }
}
