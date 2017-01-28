package com.felkertech.cumulustv.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.AssetsFileParser;
import com.felkertech.cumulustv.fileio.FileParserFactory;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.LocalFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.fileio.XmlTvParser;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nick on 12/16/2016.
 */
public class FileIoTestActivity extends AppCompatActivity {
    private static final String TAG = FileIoTestActivity.class.getSimpleName();

    private ImportedFile[] files = new ImportedFile[] {
        new ImportedFile("Test Open", ""),
        new ImportedFile("Print M3U", ""),
        new ImportedFile("M3U 1", "https://raw.githubusercontent.com/Fleker/CumulusTV/master/app/src/test/resources/m3u_test1.m3u"),
        new ImportedFile("M3U 2", "https://raw.githubusercontent.com/Fleker/CumulusTV/master/app/src/test/resources/m3u_test2.m3u"),
        new ImportedFile("M3U 3", "https://raw.githubusercontent.com/Fleker/CumulusTV/master/app/src/test/resources/m3u_test3.m3u")
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = new View(this);
        v.setBackgroundColor(getResources().getColor(R.color.md_material_blue_800));
        setContentView(v);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new MaterialDialog.Builder(this)
                .title("Choose a file")
                .items(getTitles())
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position,
                                            CharSequence text) {
                        if (position > 1) {
                            readFile(position);
                        } else if (position == 0) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse("https://raw.githubusercontent.com/Fleker/Cumulus" +
                                    "TV/master/app/src/test/resources/m3u_test1.m3u"));
                            startActivity(i);
                        } else if (position == 1) {
                            new MaterialDialog.Builder(FileIoTestActivity.this)
                                    .title("Here's how your Channel Data Looks:")
                                    .content(ChannelDatabase.getInstance(FileIoTestActivity.this).toM3u())
                                    .show();
                        }
                    }
                })
                .show();
    }

    private void readFile(int position) {
        Log.d(TAG, "User selected " + files[position].uri);
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
                                    Log.d(TAG, "Printing " + uri);
                                    parse(uri, inputStream);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
    }

    private void parse(String uri, InputStream inputStream) throws IOException {
        String extension = FileParserFactory.getFileExtension(uri);
        final String result;
        if (extension.equals("xml")) {
            XmlTvParser.TvListing listing = XmlTvParser.parse(inputStream);
            result = listing.toString();
        } else if (extension.equals("m3u")) {
            M3uParser.TvListing listing = M3uParser.parse(inputStream);
            if (listing == null) {
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        Toast.makeText(FileIoTestActivity.this, "Null listing", Toast.LENGTH_SHORT).show();
                    }
                }.sendEmptyMessage(0);
                return;
            }
            result = listing.toString();
            Log.d(TAG, listing.getChannelList()+"<");
        } else {
            result = "This file has no parser";
        }
        Log.d(TAG, "Obtained " + result);
        new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                new MaterialDialog.Builder(FileIoTestActivity.this)
                        .title("Parsing Result")
                        .content(result)
                        .negativeText("Restart")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                startActivity(new Intent(FileIoTestActivity.this, FileIoTestActivity.class));
                            }
                        })
                        .show();
            }
        }.sendEmptyMessage(0);
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
