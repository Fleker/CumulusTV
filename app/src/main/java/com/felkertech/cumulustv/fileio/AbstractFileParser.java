package com.felkertech.cumulustv.fileio;

import java.io.InputStream;

/**
 * This abstract class standardizes file I/O class features across different file sources
 * Created by Nick on 5/1/2016.
 */
public abstract class AbstractFileParser {
    //TODO Add prefix type
    public interface FileLoader {
        /**
         * When the file is finished loading and ready to parse, this callback is run
         * @param inputStream The inputStream of the file, allowing you to read and parse it
         */
        void onFileLoaded(InputStream inputStream);
    }
}