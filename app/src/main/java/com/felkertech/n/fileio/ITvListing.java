package com.felkertech.n.fileio;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;

import java.util.List;

/**
 * Created by Nick on 9/3/2016.
 */

public interface ITvListing {
    List<Channel> getChannels();
    List<Program> getProgramsFor(Channel channel);
}
