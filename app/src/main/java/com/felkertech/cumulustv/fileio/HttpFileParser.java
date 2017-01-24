package com.felkertech.cumulustv.fileio;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This parser can be used to read files from the Internet
 * Created by Nick on 5/1/2016.
 */
public class HttpFileParser extends AbstractFileParser {
    private static final String TAG = "HttpFileParser";
    FileLoader fileLoader;

    /**
     * @param url The URL of the file you're requesting
     * @param fileLoader A callback to parse the InputSream
     */
    public HttpFileParser(String url, FileLoader fileLoader) {
        this.fileLoader = fileLoader;
        new DownloadWebpageTask().execute(url);
    }
    private class DownloadWebpageTask extends AsyncTask<String, Void, InputStream> {
        @Override
        protected InputStream doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            Log.d(TAG, "Download from "+urls[0]);
            try {
                InputStream result = downloadUrl(urls[0]);
                fileLoader.onFileLoaded(result);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(InputStream result) {
            Log.d(TAG, "Posting execution");

        }
    }
    /**
     * Given a URL, establishes an HttpUrlConnection and retrieves
     * the web page content as a InputStream, which it returns as
     * a string.
     *
     * @param myurl The URL of the request
     * @return The inputstream of the request's return data
     * @throws IOException
     */
    private InputStream downloadUrl(String myurl) throws IOException {
        // Only display the first 500 characters of the retrieved
        // web page content.
        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(28000 /* milliseconds */);
        //set back to 15000, 10000
        conn.setConnectTimeout(30000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }
}