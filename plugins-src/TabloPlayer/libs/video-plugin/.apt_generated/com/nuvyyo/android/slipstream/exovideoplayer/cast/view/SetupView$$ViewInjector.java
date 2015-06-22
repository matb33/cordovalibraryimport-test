// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer.cast.view;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class SetupView$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.cast.view.SetupView> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131493022, "field 'input_playlist'");
    target.input_playlist = finder.castView(view, 2131493022, "field 'input_playlist'");
    view = finder.findRequiredView(source, 2131492981, "field 'mediaRouteButton'");
    target.mediaRouteButton = finder.castView(view, 2131492981, "field 'mediaRouteButton'");
    view = finder.findRequiredView(source, 2131493025, "field 'check_live'");
    target.check_live = finder.castView(view, 2131493025, "field 'check_live'");
    view = finder.findRequiredView(source, 2131493021, "field 'input_ip_addr'");
    target.input_ip_addr = finder.castView(view, 2131493021, "field 'input_ip_addr'");
    view = finder.findRequiredView(source, 2131492954, "field 'input_title'");
    target.input_title = finder.castView(view, 2131492954, "field 'input_title'");
    view = finder.findRequiredView(source, 2131493023, "field 'input_position'");
    target.input_position = finder.castView(view, 2131493023, "field 'input_position'");
    view = finder.findRequiredView(source, 2131493024, "field 'input_episode'");
    target.input_episode = finder.castView(view, 2131493024, "field 'input_episode'");
    view = finder.findRequiredView(source, 2131493026, "method 'setValues'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.setValues();
        }
      });
  }

  @Override public void reset(T target) {
    target.input_playlist = null;
    target.mediaRouteButton = null;
    target.check_live = null;
    target.input_ip_addr = null;
    target.input_title = null;
    target.input_position = null;
    target.input_episode = null;
  }
}
