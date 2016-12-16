package com.felkertech.cumulustv.fileio;

import android.content.Context;

import java.io.IOException;

/**
 * This class allows users to parse data that comes from a resource in your assets
 * Created by Nick on 5/1/2016.
 */
public class AssetsFileParser extends AbstractFileParser {
    /**
     * @param context Application context
     * @param assetsName Name of the asset you're requesting
     * @param fileLoader Callback which runs when the InputStream is created
     * @throws IOException
     */
    public AssetsFileParser(Context context, String assetsName, FileLoader fileLoader) throws IOException {
        fileLoader.onFileLoaded(context.getAssets().open(assetsName));
    }
}