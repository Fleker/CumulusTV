package com.felkertech.n.cumulustv;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.media.tv.TvContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.sampletvinput.syncadapter.SyncUtils;

import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "cumulus:MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Create a new channel")
                        .customView(R.layout.dialog_channel_new, true)
                        .positiveText("Create")
                        .negativeText("Cancel")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Log.d(TAG, "Submission");
                                //Get stuff
                                LinearLayout l = (LinearLayout) dialog.getCustomView();
                                String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                Log.d(TAG, "Channel " + number);
                                String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                try {
                                    Log.d(TAG, cd.toString());
                                    JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                    if (cd.channelExists(jsch)) {
                                        Toast.makeText(getApplicationContext(), "Channel already exists", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "no");
                                    } else {
                                        cd.add(jsch);
                                        Log.d(TAG, "K");
                                    }
                                    Log.d(TAG, cd.toString());
                                    SyncUtils.requestSync(info);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .show();

            }
        });
        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                String[] channelnames = cdn.getChannelNames();
                if(channelnames.length == 0) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("You have no streams")
                            .content("Do you want to add one of these? (TO BE FINISHED)")
                            .show();
                } else {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("My Streams")
                            .items(channelnames)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {
                                    Toast.makeText(getApplicationContext(), charSequence + " selected", Toast.LENGTH_SHORT).show();
                                    final MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                                            .title("Edit Stream")
                                            .positiveText("Update")
                                            .negativeText("Delete")
                                            .neutralText("Cancel")
                                            .customView(R.layout.dialog_channel_new, true)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);
                                                    LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                    Log.d(TAG, "Channel " + number);
                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                    try {
                                                        Log.d(TAG, cd.toString());
                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                                        cd.update(jsch);
                                                        Log.d(TAG, cd.toString());
                                                        SyncUtils.requestSync(info);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    super.onNegative(dialog);
                                                    new MaterialDialog.Builder(MainActivity.this)
                                                            .title("Delete?")
                                                            .positiveText("Yes")
                                                            .negativeText("No")
                                                            .callback(new MaterialDialog.ButtonCallback() {
                                                                @Override
                                                                public void onPositive(MaterialDialog dialog) {
                                                                    super.onPositive(dialog);
                                                                    LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                                    Log.d(TAG, "Channel " + number);
                                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                                    try {
                                                                        Log.d(TAG, cd.toString());
                                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                                                        cd.delete(jsch);
                                                                        Log.d(TAG, cd.toString());
                                                                        SyncUtils.requestSync(info);
                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            }).show();
                                                }
                                            })
                                            .show();
                                    md.setOnShowListener(new DialogInterface.OnShowListener() {
                                        @Override
                                        public void onShow(DialogInterface dialog) {
                                            try {
                                                JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                                                LinearLayout l = (LinearLayout) md.getCustomView();
                                                ((EditText) l.findViewById(R.id.number)).setText(jsonChannel.getNumber());
                                                Log.d(TAG, "Channel " + jsonChannel.getNumber());
                                                ((EditText) l.findViewById(R.id.name)).setText(jsonChannel.getName());
                                                ((EditText) l.findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                                                ((EditText) l.findViewById(R.id.stream)).setText(jsonChannel.getUrl());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            })
                            .show();
                }
            }
        });
    }
}
