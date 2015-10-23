package com.felkertech.n.plugins;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.android.sampletvinput.player.TvInputPlayer;
import com.felkertech.n.boilerplate.Utils.CommaArray;
import com.felkertech.n.cumulustv.ChannelDatabase;
import com.felkertech.n.cumulustv.JSONChannel;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.SamplePlayer;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * For the sake of open source software and examples, the built-in picker will be a plugin
 * Created by Nick on 8/7/2015.
 */
public class MainPicker extends CumulusTvPlugin {
    public String label = "";
    private String TAG = "cumulus:MainPicker";
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
        if(getChannel() != null)
            Log.d(TAG, getChannel().getName() + "<<");
        loadDialogs();


    }
    public void loadDialogs() {
        if(!areEditing() && !areReadingAll()) {
            final MaterialDialog add = new MaterialDialog.Builder(MainPicker.this)
                    .title("Create a new channel")
                    .customView(R.layout.dialog_channel_new, true)
                    .positiveText("Create")
                    .negativeText("Cancel")
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
            ((EditText) add.getCustomView().findViewById(R.id.stream)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    loadStream(add);
                    return false;
                }
            });
            includeGenrePicker(add, "");
            loadStream(add);
            add.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainPicker.this, SamplePlayer.class);
                    i.putExtra(SamplePlayer.KEY_VIDEO_URL, getUrl());
                    startActivity(i);
                }
            });
        } else if(!areReadingAll()) {
            final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
            final MaterialDialog md = new MaterialDialog.Builder(MainPicker.this)
                    .title("Edit Stream")
                    .positiveText("Update")
                    .negativeText("Delete")
                    .neutralText("Cancel")
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
//                            dialog.dismiss();
                            deleteChannel(jsch);
                            /*Handler h = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    super.handleMessage(msg);
                                    new MaterialDialog.Builder(MainPicker.this)
                                            .title("Delete?")
                                            .positiveText("Yes")
                                            .negativeText("No")
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);

                                                }
                                            })
                                            .show();
                                }
                            };
                            h.sendEmptyMessageDelayed(0, 2);*/
                        }
                    })
                    .show();
            md.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    JSONChannel jsonChannel = getChannel();
                    RelativeLayout l = (RelativeLayout) md.getCustomView();
                    ((EditText) l.findViewById(R.id.number)).setText(jsonChannel.getNumber());
                    Log.d(TAG, "Channel " + jsonChannel.getNumber());
                    ((EditText) l.findViewById(R.id.name)).setText(jsonChannel.getName());
                    ((EditText) l.findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                    ((EditText) l.findViewById(R.id.stream)).setText(jsonChannel.getUrl());
                    ((Button) l.findViewById(R.id.genres)).setText(jsonChannel.getGenresString());
                }
            });
            includeGenrePicker(md, getChannel().getGenresString());
            ((EditText) md.getCustomView().findViewById(R.id.stream)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    loadStream(md);
                    return false;
                }
            });
            loadStream(md);
            md.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainPicker.this, SamplePlayer.class);
                    i.putExtra(SamplePlayer.KEY_VIDEO_URL, getUrl());
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
                        .title("Select Genres")
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
                        .positiveText("Done")
                        .show();
            }
        });
    }

    public void loadStream(MaterialDialog viewHolder) {
        String url = getUrl();
        if(url.isEmpty())
            return;
        SurfaceView sv = (SurfaceView) viewHolder.getCustomView().findViewById(R.id.surface);
        TvInputPlayer exoPlayer;
        exoPlayer = new TvInputPlayer();
        exoPlayer.setSurface(sv.getHolder().getSurface());
        exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
        exoPlayer.setPlayWhenReady(true);
    }
    public String getUrl() {
        String url = "";
        if(getChannel() != null) {
            url = getChannel().getUrl();
        } if(findViewById(R.id.stream) != null)
            url = ((EditText) findViewById(R.id.stream)).getText().toString();
        return url;
    }
}
