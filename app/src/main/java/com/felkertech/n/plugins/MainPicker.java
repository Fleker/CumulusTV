package com.felkertech.n.plugins;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.players.TvInputPlayer;
import com.felkertech.n.boilerplate.Utils.PermissionUtils;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.activities.CumulusTvPlayer;
import com.felkertech.n.cumulustv.model.JSONChannel;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.fileio.M3UParser;
import com.felkertech.settingsmanager.common.CommaArray;

import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * For the sake of open source software and examples, the built-in picker will be a plugin
 * Created by Nick on 8/7/2015.
 */
public class MainPicker extends CumulusTvPlugin {
    public String label = "";
    private String TAG = "cumulus:MainPicker";
    private MaterialDialog pickerDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Start a");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Start b");
        Fabric.with(this, new Crashlytics());
        setLabel(label);
        setProprietaryEditing(false);
        setContentView(R.layout.fullphoto);
        Log.d(TAG, areEditing() + "<");
        Intent i = getIntent();
        Log.d(TAG, i.getAction()+"<<");
        if(i.getAction() != null && (i.getAction().equals(Intent.ACTION_SEND) || i.getAction().equals(Intent.ACTION_VIEW))) {
            Log.d(TAG, "User shared to this");
            final Uri uri = getIntent().getData();
            Log.d(TAG, "Uri "+uri);
            //At this point we need to check for the storage
            if(uri == null) {
                Toast.makeText(this, R.string.import_null, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if(uri.getScheme().contains("file") && PermissionUtils.isDisabled(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionUtils.requestPermissionIfDisabled(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                Toast.makeText(this, R.string.permission_not_allowed_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                if(uri == null) {
                    Toast.makeText(MainPicker.this, "Uri is null", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                Log.d(TAG, uri.toString());
                if(uri.toString().contains("http")) { //Import a channel
                    //Copy from `loadDialogs()` in edit mode
                    pickerDialog = new MaterialDialog.Builder(MainPicker.this)
                            .title(R.string.add_new_channel)
                            .positiveText(R.string.create)
                            .neutralText(R.string.cancel)
                            .dismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    finish();
                                }
                            })
                            .customView(R.layout.dialog_channel_new, true)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                    Log.d(TAG, "Channel " + number);
                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                    String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                                    String genres = ((Button) l.findViewById(R.id.genres)).getText().toString();

                                    if(number == null || number.length() == 0) {
                                        Toast.makeText(MainPicker.this, "Your channel must have a number!", Toast.LENGTH_SHORT).show();
                                        ((EditText) l.findViewById(R.id.number)).setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                                    } else {
                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, genres);
                                        saveChannel(jsch);
                                    }
                                }
                            })
                            .show();
                    pickerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            RelativeLayout l = (RelativeLayout) pickerDialog.getCustomView();
                           ((EditText) l.findViewById(R.id.stream)).setText(uri.toString());
                            loadStream(pickerDialog, uri.toString());
                        }
                    });
                    includeGenrePicker(pickerDialog, "");
                    ((EditText) pickerDialog.getCustomView().findViewById(R.id.stream)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            loadStream(pickerDialog, uri.toString());
                            return false;
                        }
                    });
                    loadStream(pickerDialog, uri.toString());
                    pickerDialog.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(MainPicker.this, CumulusTvPlayer.class);
                            i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, uri.toString());
                            startActivity(i);
                        }
                    });
                } else {
                    ContentResolver resolver = getContentResolver();
                    InputStream input = resolver.openInputStream(uri);
                    final M3UParser.TvListing listings = M3UParser.parse(input, getApplicationContext());
                    new MaterialDialog.Builder(MainPicker.this)
                            .title(getString(R.string.import_bulk_title, listings.channels.size()))
                            .content(listings.getChannelList())
                            .positiveText(R.string.ok)
                            .negativeText(R.string.no)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    Toast.makeText(MainPicker.this, R.string.import_bulk_wait, Toast.LENGTH_SHORT).show();
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ChannelDatabase channelDatabase = new ChannelDatabase(MainPicker.this);
                                            for (M3UParser.XmlTvChannel channel : listings.channels) {
                                                JSONChannel jsonChannel = new JSONChannel(
                                                        channel.displayNumber,
                                                        channel.displayName,
                                                        channel.url,
                                                        null,
                                                        null,
                                                        TvContract.Programs.Genres.MOVIES
                                                );
                                                try {
                                                    channelDatabase.add(jsonChannel);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            channelDatabase.save();
                                            Handler finishedImporting = new Handler(Looper.getMainLooper()) {
                                                @Override
                                                public void handleMessage(Message msg) {
                                                    super.handleMessage(msg);
                                                    Toast.makeText(MainPicker.this, R.string.import_bulk_success, Toast.LENGTH_SHORT).show();
                                                    saveDatabase();
                                                }
                                            };
                                            finishedImporting.sendEmptyMessage(0);
                                        }
                                    }).start();
                                }
                            })
                            .show();
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        } else {
            if (getChannel() != null)
                Log.d(TAG, getChannel().getName() + "<<");
            loadDialogs();
        }
    }

    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }

    public void loadDialogs() {
        if(!areEditing() && !areReadingAll()) {
            pickerDialog = new MaterialDialog.Builder(MainPicker.this)
                    .title(R.string.add_new_channel)
                    .customView(R.layout.dialog_channel_new, true)
                    .positiveText(R.string.create)
                    .negativeText(R.string.cancel)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Log.d(TAG, "Submission");
                            //Get stuff
                            RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                            String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                            Log.d(TAG, "Channel " + number);
                            String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                            String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                            String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                            String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                            String genres = ((Button) l.findViewById(R.id.genres)).getText().toString();
                            JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, genres);
                            saveChannel(jsch);
                        }
                    })
                    .show();
            ((EditText) pickerDialog.getCustomView().findViewById(R.id.stream)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    loadStream(pickerDialog);
                    return false;
                }
            });
            includeGenrePicker(pickerDialog, "");
            loadStream(pickerDialog);
            pickerDialog.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainPicker.this, CumulusTvPlayer.class);
                    i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, getUrl());
                    startActivity(i);
                }
            });
        } else if(!areReadingAll()) {
            final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
            pickerDialog = new MaterialDialog.Builder(MainPicker.this)
                    .title(R.string.edit_new_channel)
                    .positiveText(R.string.update)
                    .negativeText(R.string.delete)
                    .neutralText(R.string.cancel)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .customView(R.layout.dialog_channel_new, true)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                            String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                            Log.d(TAG, "Channel " + number);
                            String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                            String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                            String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                            String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                            String genres = ((Button) l.findViewById(R.id.genres)).getText().toString();

                            JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, genres);
                            saveChannel(jsch, getChannel());
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                            String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                            Log.d(TAG, "Channel " + number);

                            Log.d(TAG, "DEL Channel " + number);
                            String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                            String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                            String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                            String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                            String genres = ((Button) l.findViewById(R.id.genres)).getText().toString();

                            final JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, genres);
                            deleteChannel(jsch);
                        }
                    })
                    .show();
            pickerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    JSONChannel jsonChannel = getChannel();
                    RelativeLayout l = (RelativeLayout) pickerDialog.getCustomView();
                    ((EditText) l.findViewById(R.id.number)).setText(jsonChannel.getNumber());
                    Log.d(TAG, "Channel " + jsonChannel.getNumber());
                    ((EditText) l.findViewById(R.id.name)).setText(jsonChannel.getName());
                    ((EditText) l.findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                    ((EditText) l.findViewById(R.id.stream)).setText(jsonChannel.getUrl());
                    ((Button) l.findViewById(R.id.genres)).setText(jsonChannel.getGenresString());
                    loadStream(pickerDialog);
                }
            });
            includeGenrePicker(pickerDialog, getChannel().getGenresString());
            ((EditText) pickerDialog.getCustomView().findViewById(R.id.stream)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    loadStream(pickerDialog);
                    return false;
                }
            });
            loadStream(pickerDialog);
            pickerDialog.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainPicker.this, CumulusTvPlayer.class);
                    i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, getUrl());
                    startActivity(i);
                }
            });
        } else {
            Toast.makeText(MainPicker.this, "This plugin doesn't support READALL", Toast.LENGTH_SHORT).show();
            Log.d(TAG, getAllChannels());
            finish();
        }
    }
    public void includeGenrePicker(final MaterialDialog d, final String gString) {
        d.findViewById(R.id.genres).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> selections = new ArrayList<>();
                int index = 0;
                for(String g: ChannelDatabase.getAllGenres()) {
                    if(gString.contains(g)) {
                        selections.add(index);
                    }
                    index++;
                }

                new MaterialDialog.Builder(MainPicker.this)
                        .title(R.string.select_genres)
                        .items(ChannelDatabase.getAllGenres())
                        .itemsCallbackMultiChoice(selections.toArray(new Integer[selections.size()]), new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                                CommaArray genres = new CommaArray("");
                                for (CharSequence g : charSequences) {
                                    genres.add(g.toString());
                                }
                                ((Button) d.findViewById(R.id.genres)).setText(genres.toString());
                                return false;
                            }
                        })
                        .positiveText(R.string.done)
                        .show();
            }
        });
    }

    public void loadStream(MaterialDialog viewHolder) {
        String url = getUrl();
        if(url == null || url.isEmpty()) {
            Log.d(TAG, "Ignoring null or empty string");
            return;
        }
        SurfaceView sv = (SurfaceView) viewHolder.getCustomView().findViewById(R.id.surface);
        TvInputPlayer exoPlayer;
        exoPlayer = new TvInputPlayer();
        exoPlayer.setSurface(sv.getHolder().getSurface());
        try {
            exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
        } catch(Exception e) {
            //Do nothing
            Log.d(TAG, "IllegalArgumentException");
        }
        exoPlayer.setPlayWhenReady(true);
    }
    public void loadStream(MaterialDialog viewHolder, final String url) {
        SurfaceView sv = (SurfaceView) viewHolder.getCustomView().findViewById(R.id.surface);
        TvInputPlayer exoPlayer;
        exoPlayer = new TvInputPlayer();
        exoPlayer.setSurface(sv.getHolder().getSurface());
        try {
            exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
        } catch (Exception e) {
                e.printStackTrace();
        }
        exoPlayer.setPlayWhenReady(true);
    }
    public String getUrl() {
        String url = "";
        if(getChannel() != null) {
            url = getChannel().getUrl();
        } if(pickerDialog.getCustomView().findViewById(R.id.stream) != null)
            url = ((EditText) pickerDialog.getCustomView().findViewById(R.id.stream)).getText().toString();
        Log.d(TAG, "Found '"+url+"'");
        return url;
    }
}
