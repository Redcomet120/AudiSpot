package com.example.mbriseno.audispot;

/**
 * Created by mbriseno on 1/4/2017.
 */

import android.support.annotation.Nullable;

public interface Player {

    void play(String url);

    void pause();

    void resume();

    boolean isPlaying();

    @Nullable
    String getCurrentTrack();

    void release();
}
