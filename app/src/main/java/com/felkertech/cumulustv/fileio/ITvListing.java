package com.felkertech.cumulustv.fileio;

import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.Program;

import java.util.List;

/**
 * Created by Nick on 9/3/2016.
 */

public interface ITvListing {
    List<Channel> getChannels();
    List<Program> getProgramsFor(Channel channel);
}
