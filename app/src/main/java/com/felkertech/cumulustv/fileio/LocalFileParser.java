package com.felkertech.cumulustv.fileio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * This file parser can be used to read from the device's local filesystem
 * Created by Nick on 5/1/2016.
 */
public class LocalFileParser extends AbstractFileParser {
    /**
     * @param fileUri The URI of the local file
     * @param fileLoader Callback which runs when the InputStream is gotten
     * @throws FileNotFoundException
     */
    public LocalFileParser(String fileUri, FileLoader fileLoader) throws FileNotFoundException {
        FileInputStream f = new FileInputStream(fileUri);
        fileLoader.onFileLoaded(f);
    }
}