package com.felkertech.cumulustv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.AssetsFileParser;
import com.felkertech.cumulustv.fileio.FileParserFactory;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.LocalFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.fileio.XmlTvParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nick on 12/16/2016.
 */
public class FileIoTestActivity extends Activity {
    private ImportedFile[] files = new ImportedFile[] {
        new ImportedFile("M3U 1", "https://raw.githubusercontent.com/Fleker/CumulusTV/master/app/src/test/resources/m3u_test1.m3u")
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new MaterialDialog.Builder(this)
                .title("Choose a file")
                .items(getTitles())
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position,
                        CharSequence text) {
                        FileParserFactory.parseGenericFileUri(files[position].uri,
                                new FileParserFactory.FileIdentifier() {
                            @Override
                            public void onLocalFile(final String uri) {
                                try {
                                    new LocalFileParser(uri, new AbstractFileParser.FileLoader() {
                                        @Override
                                        public void onFileLoaded(InputStream inputStream) {
                                            try {
                                                parse(uri, inputStream);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onAsset(final String uri) {
                                try {
                                    new AssetsFileParser(getApplicationContext(), uri,
                                            new AbstractFileParser.FileLoader() {
                                        @Override
                                        public void onFileLoaded(InputStream inputStream) {
                                            try {
                                                parse(uri, inputStream);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onHttpFile(final String uri) {
                                new HttpFileParser(uri, new AbstractFileParser.FileLoader() {
                                    @Override
                                    public void onFileLoaded(InputStream inputStream) {
                                        try {
                                            parse(uri, inputStream);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
    }

    private void parse(String uri, InputStream inputStream) throws IOException {
        String extension = FileParserFactory.getFileExtension(uri);
        String result;
        if (extension.equals("xml")) {
            XmlTvParser.TvListing listing = XmlTvParser.parse(inputStream);
            result = listing.toString();
        } else if (extension.equals("m3u")) {
            M3uParser.TvListing listing = M3uParser.parse(inputStream);
            result = listing.toString();
        } else {
            result = "This file has no parser";
        }
        new MaterialDialog.Builder(this)
                .title("Parsing Result")
                .content(result)
                .show();
    }

    private class ImportedFile extends Pair<String, String> {
        public final String title;
        public final String uri;

        /**
         * Constructor for a Pair.
         *
         * @param first  the first object in the Pair
         * @param second the second object in the pair
         */
        ImportedFile(String first, String second) {
            super(first, second);
            title = first;
            uri = second;
        }
    }

    private String[] getTitles() {
        String[] titles = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            titles[i] = files[i].title;
        }
        return titles;
    }
}
