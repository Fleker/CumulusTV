package com.felkertech.cumulustv.fileio;

/**
 * This is a utility class, making it easy to do central activities surrounding files
 * Created by Nick on 5/1/2016.
 */
public class FileParserFactory {
    /**
     * In order to determine the correct file parser, you can provide the URI and this method
     * will find the appropriate parser to use
     * @param uri The Uri of the file you are parsing
     * @param identifier This interface can be generated in order to handle different actions
     *                   based on the source of the file
     */
    public static void parseGenericFileUri(String uri, FileIdentifier identifier) {
        if(uri.startsWith("file://"))
            identifier.onLocalFile();
        else if(uri.startsWith("http://"))
            identifier.onHttpFile();
        else if(uri.startsWith("android.resource://"))
            identifier.onAsset();
    }
    public interface FileIdentifier {
        /**
         * Called when the file is locally on the device
         */
        void onLocalFile();

        /**
         * Called when the file is located in your app's assets
         */
        void onAsset();

        /**
         * Called when the file is from a web address
         */
        void onHttpFile();
    }
}