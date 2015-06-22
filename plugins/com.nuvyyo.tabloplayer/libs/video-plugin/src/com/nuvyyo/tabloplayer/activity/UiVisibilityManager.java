package com.nuvyyo.tabloplayer.activity;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by mike on 02/06/15.
 */
public class UiVisibilityManager
{
    /**
     * Listener interface for visibility changes.
     */
    public interface OnVisibilityChangeListener {
        /**
         * Called when the system wil be made visible.
         */
        public void willShowSystemUi();

        /**
         * Called when the system ui will be hidden.
         */
        public void willHideSystemUi();
    }

    /**
     * Flags which will be passed to {@code View#setSystemUiVisibility(int)}
     * when transitioning out of full-screen mode.
     */
    private static final int NORMAL_UI_FLAGS =  View.SYSTEM_UI_FLAG_LAYOUT_STABLE     |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    /**
     * Flags which will be passed to {@code View#setSystemUiVisibility(int)}
     * when transitioning into full-screen mode.
     */
    private static final int FULL_SCREEN_UI_FLAGS;
    static
    {
        // Set up Full screen flags
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN      |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

        // If API 19 or higher, we will utilise immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        else
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE;

        // Flags are constructed
        FULL_SCREEN_UI_FLAGS = flags;
    }

    /**
     * The root {@code View} of the activity which will be made fullscreen.
     */
    private View mRootLayout;

    /**
     * The Application's {@code Window}.
     */
    private Window mWindow;

    /**
     * Flag which indicates if the system Ui is currently visible.
     */
    private boolean mSystemUiVisible = true;

    private OnVisibilityChangeListener mListener;

    /**
     * Default constructor.
     *
     * @param window
     *            The application's {@code Window}. This will be used to apply
     *            system ui visibility flags via
     *            {@code View#setSystemUiVisibility(int)}.
     *
     * @param rootLayout
     *            The root layout of the application's visible activity.
     */
    public UiVisibilityManager(Window window, View rootLayout) {
        assert( rootLayout != null );

        mRootLayout = rootLayout;
        mWindow = window;

        mRootLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility)
            {
                // The System UI has become visible
                if( (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0 && visibility != View.SYSTEM_UI_FLAG_FULLSCREEN ) {
                    setSystemUiVisible(true);
                }
            }
        });
        mWindow.getDecorView().setSystemUiVisibility(NORMAL_UI_FLAGS);
    }

    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        mListener = listener;
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        int KEEP_SCREEN_ON = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if( keepScreenOn )
            mWindow.addFlags( KEEP_SCREEN_ON );
        else
            mWindow.clearFlags( KEEP_SCREEN_ON );
    }

    public void toggleSystemUiVisible() {
        if( isSystemUiVisible() )
            hideSystemUi();
        else
            showSystemUi();
    }

    public void hideSystemUi() {
        setSystemUiVisible(false);
        applyUiVisibilityFlags(FULL_SCREEN_UI_FLAGS);
    }

    public void showSystemUi() {
        setSystemUiVisible(true);
        applyUiVisibilityFlags(NORMAL_UI_FLAGS);
    }

    public boolean isSystemUiVisible() {
        return mSystemUiVisible;
    }

    private void applyUiVisibilityFlags(int uiVisibilityFlags) {
        // Hide the status bar
        View decorView = mRootLayout;

        // Only apply flags if it will result in a change.
        decorView.setSystemUiVisibility(uiVisibilityFlags);
    }

    private void setSystemUiVisible(boolean visible) {
        if( visible == mSystemUiVisible )
            return;

        boolean becameVisible   = visible && !mSystemUiVisible;
        boolean becameInvisible = !visible && mSystemUiVisible;

        mSystemUiVisible = visible;
        if( mListener == null )
            return;

        if( becameVisible )
            mListener.willShowSystemUi();

        else if( becameInvisible )
            mListener.willHideSystemUi();
    }
}
