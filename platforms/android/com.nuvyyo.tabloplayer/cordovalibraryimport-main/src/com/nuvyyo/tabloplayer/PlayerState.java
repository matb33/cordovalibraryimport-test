package com.nuvyyo.tabloplayer;

/**
 * Created by mike on 01/06/15.
 */
public enum PlayerState {
    IDLE,
    PREPARING,
    PREPARED,
    BUFFERING,
    STARTED,
    PAUSED,
    STOPPED,
    PLAYBACK_COMPLETE,
    END,
    ERROR;
}
