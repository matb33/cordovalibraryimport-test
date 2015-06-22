// Generated code from Butter Knife. Do not modify!
package com.nuvyyo.android.slipstream.exovideoplayer.playercontrols;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.Injector;

public class TVMediaControllerView$$ViewInjector<T extends com.nuvyyo.android.slipstream.exovideoplayer.playercontrols.TVMediaControllerView> implements Injector<T> {
  @Override public void inject(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131492983, "field 'scrubber'");
    target.scrubber = finder.castView(view, 2131492983, "field 'scrubber'");
    view = finder.findRequiredView(source, 2131492972, "field 'media_controls'");
    target.media_controls = view;
    view = finder.findRequiredView(source, 2131492978, "field 'pauseButton'");
    target.pauseButton = finder.castView(view, 2131492978, "field 'pauseButton'");
    view = finder.findRequiredView(source, 2131492982, "field 'position'");
    target.position = finder.castView(view, 2131492982, "field 'position'");
    view = finder.findRequiredView(source, 2131492984, "field 'duration'");
    target.duration = finder.castView(view, 2131492984, "field 'duration'");
  }

  @Override public void reset(T target) {
    target.scrubber = null;
    target.media_controls = null;
    target.pauseButton = null;
    target.position = null;
    target.duration = null;
  }
}
