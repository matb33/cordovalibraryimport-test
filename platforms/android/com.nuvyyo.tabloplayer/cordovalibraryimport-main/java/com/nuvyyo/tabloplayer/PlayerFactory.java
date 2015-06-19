package com.nuvyyo.tabloplayer;

import com.nuvyyo.tabloplayer.exception.NotImplementedException;
import com.nuvyyo.tabloplayer.exoplayer.TabloExoPlayer;

/**
 * Created by mike on 01/06/15.
 */
public final class PlayerFactory {
    public static enum PlayerType {
        EXO_PLAYER,
        MEDIA_PLAYER,
        VO_PLAYER;
    }

    public static final Player createPlayer(PlayerType playerType) {
        switch(playerType) {
            case EXO_PLAYER:
                return newExoPlayer();

            // TODO: MediaPlayer and VOPlayer are not implemented at this time.
            case MEDIA_PLAYER:
            case VO_PLAYER:
            default:
                throw new NotImplementedException(playerType.name() + " is not currently supported.");
        }
    }

    private static final Player newExoPlayer() {
        return new TabloExoPlayer();
    }

    private static final Player newMediaPlayer() {
        throw new NotImplementedException("MediaPlayer is not currently supported.");
    }

    private static final Player newVOPlayer() {
        throw new NotImplementedException("VOPlayer is not currently supported.");
    }
}
