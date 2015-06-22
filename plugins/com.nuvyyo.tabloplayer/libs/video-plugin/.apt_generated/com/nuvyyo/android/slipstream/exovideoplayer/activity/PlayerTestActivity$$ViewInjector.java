// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer.activity;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class PlayerTestActivity$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.activity.PlayerTestActivity> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131492956, "field 'button_play' and method 'playVideo'");
    target.button_play = finder.castView(view, 2131492956, "field 'button_play'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.playVideo();
        }
      });
    view = finder.findRequiredView(source, 2131492953, "field 'input_start'");
    target.input_start = finder.castView(view, 2131492953, "field 'input_start'");
    view = finder.findRequiredView(source, 2131492948, "field 'playlist_url'");
    target.playlist_url = finder.castView(view, 2131492948, "field 'playlist_url'");
    view = finder.findRequiredView(source, 2131492954, "field 'input_title'");
    target.input_title = finder.castView(view, 2131492954, "field 'input_title'");
    view = finder.findRequiredView(source, 2131492949, "field 'stream_type'");
    target.stream_type = finder.castView(view, 2131492949, "field 'stream_type'");
    view = finder.findRequiredView(source, 2131492955, "field 'input_subtitle'");
    target.input_subtitle = finder.castView(view, 2131492955, "field 'input_subtitle'");
  }

  @Override public void reset(T target) {
    target.button_play = null;
    target.input_start = null;
    target.playlist_url = null;
    target.input_title = null;
    target.stream_type = null;
    target.input_subtitle = null;
  }
}
