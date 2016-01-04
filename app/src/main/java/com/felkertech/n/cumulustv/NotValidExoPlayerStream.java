package com.felkertech.n.cumulustv;

/**
 * Created by guest1 on 1/3/2016.
 */
public class NotValidExoPlayerStream extends Exception {
    public NotValidExoPlayerStream() {
        super("Not a valid ExoPlayer Stream");
    }
}
