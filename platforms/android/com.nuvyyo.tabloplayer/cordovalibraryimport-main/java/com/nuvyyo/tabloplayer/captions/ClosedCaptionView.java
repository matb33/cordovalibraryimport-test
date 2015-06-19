package com.nuvyyo.tabloplayer.captions;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nuvyyo.tabloplayer.R;

import java.util.List;

/**
 * Created by mike on 12/06/15.
 */
public class ClosedCaptionView extends LinearLayout {
    private static final String TAG = "ClosedCaptionView";

    public ClosedCaptionView(Context context) {
        this(context, null);
    }

    public ClosedCaptionView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        init();
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        setFitsSystemWindows(false);
    }

    public void setLines(List<String> lines) {
        clearLines();

        for(String line : lines) {
            addLine(line);
        }
    }

    public void addLine(final CharSequence line) {
        addTextView(line);

        if( getVisibility() != View.VISIBLE )
            setVisibility(View.VISIBLE);
    }

    public void clearLines() {
        removeAllViews();
        setVisibility(View.GONE);
        Log.v(TAG, "clearLines(): clear");
    }

    private void addTextView(final CharSequence line) {
        Log.v(TAG, "addLine(): " + line);
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        final TextView view = (TextView)inflater.inflate(R.layout.view_closed_caption_line, null);
        view.setText(line);
        addView(view);

        LayoutParams param = ((LayoutParams)view.getLayoutParams());
        param.gravity = Gravity.CENTER_HORIZONTAL;
        param.bottomMargin = 10;
        param.topMargin = 10;
        view.setLayoutParams(param);
    }
}
