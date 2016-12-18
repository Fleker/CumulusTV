package com.felkertech.cumulustv.fileio;

import android.util.Log;

/**
 * This is a utility class, making it easy to do central activities surrounding files
 * Created by Nick on 5/1/2016.
 */
public class FileParserFactory {
    private static final String TAG = FileParserFactory.class.getSimpleName();

    /**
     * In order to determine the correct file parser, you can provide the URI and this method
     * will find the appropriate parser to use
     * @param uri The Uri of the file you are parsing
     * @param identifier This interface can be generated in order to handle different actions
     *                   based on the source of the file
     */
    public static void parseGenericFileUri(String uri, FileIdentifier identifier) {
        if(uri.startsWith("file://")) {
            identifier.onLocalFile(uri);
        } else if(uri.startsWith("http://") || uri.startsWith("https://")) {
            identifier.onHttpFile(uri);
        } else if(uri.startsWith("android.resource://")) {
            identifier.onAsset(uri);
        } else {
            Log.e(TAG, "None of the above match " + uri);
        }
    }

    public static String getFileExtension(String uri) {
        String[] dots = uri.split("\\.");
        return dots[dots.length - 1];
    }

    public interface FileIdentifier {
        /**
         * Called when the file is locally on the device
         */
        void onLocalFile(String uri);

        /**
         * Called when the file is located in your app's assets
         */
        void onAsset(String uri);

        /**
         * Called when the file is from a web address
         */
        void onHttpFile(String uri);
    }
}