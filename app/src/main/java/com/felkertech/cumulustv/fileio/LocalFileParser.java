package com.felkertech.cumulustv.fileio;

import android.os.Environment;
import android.util.Log;

import java.io.File;
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
        Log.d(LocalFileParser.class.getSimpleName(), fileUri);
        Log.d(LocalFileParser.class.getSimpleName(), new File(fileUri).exists() + "");
        // Have to hack in an exception
        File localFile = new File(fileUri);
        if (fileUri.startsWith("file:///storage/emulated/0/")) {
            Log.i(LocalFileParser.class.getSimpleName(), "Apply a hack to import the file");
            localFile = new File(Environment.getExternalStorageDirectory(), fileUri.substring(27));
        }
        Log.d(LocalFileParser.class.getSimpleName(), localFile.exists() + "");
        FileInputStream f = new FileInputStream(localFile);
        fileLoader.onFileLoaded(f);
    }
}