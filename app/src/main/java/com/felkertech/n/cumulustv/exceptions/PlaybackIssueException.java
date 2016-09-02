package com.felkertech.n.cumulustv.exceptions;

/**
 * When users report a playback issue, that is fed into a crash report by causing a runtime
 * exception. It's hacky, yes.
 *
 * @author Nick
 * @version 2016-09-02
 */
public class PlaybackIssueException extends RuntimeException {
    public PlaybackIssueException(String message) {
        super(message);
    }
}
