package com.felkertech.n.plugins;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
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
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.players.TvInputPlayer;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.plugins.CumulusTvPlugin;
import com.felkertech.n.boilerplate.Utils.AppUtils;
import com.felkertech.n.boilerplate.Utils.PermissionUtils;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.activities.CumulusTvPlayer;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.fileio.M3uParser;
import com.felkertech.settingsmanager.common.CommaArray;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;

import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * For the sake of open source software and examples, the built-in picker will be a plugin
 * Created by Nick on 8/7/2015.
 */
public class MainPicker extends CumulusTvPlugin {
    private static final String TAG = "cumulus:MainPicker";
    private static final boolean DEBUG = false;

    private String label = "";
    private IMainPicker mPickerDialog;
    private TvInputPlayer mTvInputPlayer;
    private TextWatcher filterTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mPickerDialog != null) {
                mPickerDialog.loadValidation();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setLabel(label);
        setProprietaryEditing(false);
        setContentView(R.layout.fullphoto);
        Intent i = getIntent();
        if(i.getAction() != null && (i.getAction().equals(Intent.ACTION_SEND) ||
                i.getAction().equals(Intent.ACTION_VIEW))) {
            final Uri uri = getIntent().getData();
            if (DEBUG) {
                Log.d(TAG, "User shared to this");
                Log.d(TAG, "Uri "+uri);
            }
            //At this point we need to check for the storage
            if(uri == null) {
                Toast.makeText(this, R.string.import_null, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if(uri.getScheme().contains("file") &&
                    PermissionUtils.isDisabled(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionUtils.requestPermissionIfDisabled(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                Toast.makeText(this, R.string.permission_not_allowed_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                if(uri.toString().contains("http")) { //Import a channel
                    // Copy from `loadDialogs()` in edit mode
                    mPickerDialog = getPicker().show(this, true);
                    RelativeLayout l = (RelativeLayout) mPickerDialog.getCustomView();
                    ((EditText) l.findViewById(R.id.stream)).setText(uri.toString());
                    loadStream(mPickerDialog, uri.toString());
                } else {
                    ContentResolver resolver = getContentResolver();
                    InputStream input = resolver.openInputStream(uri);
                    final M3uParser.TvListing listings = M3uParser.parse(input,
                            getApplicationContext());
                    new MaterialDialog.Builder(MainPicker.this)
                            .title(getString(R.string.import_bulk_title, listings.channels.size()))
                            .content(listings.getChannelList())
                            .positiveText(R.string.ok)
                            .negativeText(R.string.no)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    Toast.makeText(MainPicker.this, R.string.import_bulk_wait,
                                            Toast.LENGTH_SHORT).show();
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ChannelDatabase channelDatabase =
                                                    ChannelDatabase.getInstance(MainPicker.this);
                                            for (M3uParser.XmlTvChannel channel :
                                                    listings.channels) {
                                                CumulusChannel jsonChannel =
                                                        new JsonChannel.Builder()
                                                        .setName(channel.displayName)
                                                        .setNumber(channel.displayNumber)
                                                        .setMediaUrl(channel.url)
                                                        .setGenres(TvContract.Programs.Genres.MOVIES)
                                                        .build();
                                                try {
                                                    channelDatabase.add(jsonChannel);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            channelDatabase.save();
                                            Handler finishedImporting =
                                                    new Handler(Looper.getMainLooper()) {
                                                @Override
                                                public void handleMessage(Message msg) {
                                                    super.handleMessage(msg);
                                                    Toast.makeText(MainPicker.this,
                                                            R.string.import_bulk_success,
                                                            Toast.LENGTH_SHORT).show();
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
            if (getChannel() != null && DEBUG) {
                Log.d(TAG, getChannel().getName() + "<<");
            }
            loadDialogs();
        }
    }

    @Override
    protected void onStop() {
        SurfaceView sv = (SurfaceView) mPickerDialog.getCustomView().findViewById(R.id.surface);
        sv.getHolder().getSurface().release();
        mTvInputPlayer.setSurface(null);
        if (mTvInputPlayer != null) {
            mTvInputPlayer.stop();
            mTvInputPlayer.release();
        } else {
            Log.w(TAG, "TvInputPlayer is null.");
        }
        mTvInputPlayer = null;
        super.onStop();
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
            mPickerDialog = getPicker().show(this, true);
        } else if(!areReadingAll()) {
            mPickerDialog = getPicker().show(this, false);
        } else {
            Toast.makeText(MainPicker.this, R.string.toast_msg_no_support_READALL,
                    Toast.LENGTH_SHORT).show();
            if (DEBUG) {
                Log.d(TAG, getAllChannels());
            }
            finish();
        }
    }

    public void includeGenrePicker(final IMainPicker picker, final String gString) {
        picker.getCustomView().findViewById(R.id.genres).setOnClickListener(
                new View.OnClickListener() {
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
                        .itemsCallbackMultiChoice(selections.toArray(
                                new Integer[selections.size()]),
                                new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog,
                                    Integer[] integers, CharSequence[] charSequences) {
                                CommaArray genres = new CommaArray("");
                                for (CharSequence g : charSequences) {
                                    genres.add(g.toString());
                                }
                                ((Button) picker.getCustomView().findViewById(R.id.genres))
                                        .setText(genres.toString());
                                return false;
                            }
                        })
                        .positiveText(R.string.done)
                        .show();
            }
        });
    }

    public void loadStream(IMainPicker viewHolder) {
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
            exoPlayer.prepare(getApplicationContext(), Uri.parse(url),
                    TvInputPlayer.SOURCE_TYPE_HLS);
        } catch(Exception e) {
            //Do nothing
            Log.d(TAG, "IllegalArgumentException");
        }
        exoPlayer.setPlayWhenReady(true);
    }

    public void loadStream(IMainPicker viewHolder, final String url) {
        SurfaceView sv = (SurfaceView) viewHolder.getCustomView().findViewById(R.id.surface);
        if (mTvInputPlayer == null) {
            mTvInputPlayer = new TvInputPlayer();
        }
        mTvInputPlayer.setSurface(sv.getHolder().getSurface());
        mTvInputPlayer.setVolume(1);
        try {
            mTvInputPlayer.prepare(getApplicationContext(), Uri.parse(url),
                    TvInputPlayer.SOURCE_TYPE_HLS);
        } catch (Exception e) {
                e.printStackTrace();
        }
        mTvInputPlayer.addCallback(new TvInputPlayer.Callback() {
            @Override
            public void onPrepared() {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                if (state == ExoPlayer.STATE_READY) {
                    mPickerDialog.handlePlaybackSuccess();
                }
            }

            @Override
            public void onPlayWhenReadyCommitted() {

            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
                mPickerDialog.handlePlaybackError(e);
            }

            @Override
            public void onDrawnToSurface(Surface surface) {

            }

            @Override
            public void onText(String text) {

            }
        });
        mTvInputPlayer.setPlayWhenReady(true);
    }

    public String getUrl() {
        String url = "";
        if(getChannel() != null) {
            url = getChannel().getMediaUrl();
        }
        if(mPickerDialog != null &&
                mPickerDialog.getCustomView().findViewById(R.id.stream) != null) {
            url = ((EditText) mPickerDialog.getCustomView().findViewById(R.id.stream)).getText()
                    .toString();
        }
        if (DEBUG) {
            Log.d(TAG, "Found '" + url + "'");
        }
        return url;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStream(mPickerDialog);
    }

    private IMainPicker getPicker() {
        if (AppUtils.isTV(this)) {
            return new TvPickerDialog();
        } else {
            return new MobilePickerDialog();
        }
    }

    private abstract class PickerDialog implements IMainPicker {

    }

    private class MobilePickerDialog extends PickerDialog {
        private MaterialDialog mDialog;
        private Context mContext;
        private boolean mIsNewChannel;

        protected MobilePickerDialog() {
        }

        public void loadValidation() {
            boolean isValid = true;
            String number = String.valueOf(
                    ((EditText) mDialog.getCustomView().findViewById(R.id.number)).getText());
            if(number.length() == 0) {
                mDialog.getCustomView().findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                mDialog.getCustomView().findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String name = String.valueOf(
                    ((EditText) mDialog.getCustomView().findViewById(R.id.name)).getText());
            if(name.length() == 0) {
                mDialog.getCustomView().findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                mDialog.getCustomView().findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String mediaUrl = String.valueOf(
                    ((EditText) mDialog.getCustomView().findViewById(R.id.stream)).getText());
            if(mediaUrl.length() == 0) {
                mDialog.getCustomView().findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                loadStream(MobilePickerDialog.this, getUrl());
                mDialog.getCustomView().findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            Log.d(TAG, "'" + number + "', '" + name + "', '" + mediaUrl + "'");

            if (isValid) {
                mDialog.setActionButton(DialogAction.POSITIVE,
                        mIsNewChannel ? mContext.getString(R.string.create) :
                                mContext.getString(R.string.update));
                Log.w(TAG, "Yes this is a valid channel.");
            } else {
                mDialog.setActionButton(DialogAction.POSITIVE, null);
                Log.w(TAG, "This is not a valid channel.");
            }
        }

        private MaterialDialog build(Context context, boolean isNewChannel) {
            mContext = context;
            mIsNewChannel = isNewChannel;
            mDialog = new MaterialDialog.Builder(context)
                    .title(isNewChannel ? context.getString(R.string.manage_add_new) :
                            context.getString(R.string.edit_channel))
                    .neutralText(context.getString(R.string.cancel))
                    .negativeText(isNewChannel ? null : context.getString(R.string.delete))
                    .dismissListener(new MaterialDialog.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .customView(R.layout.dialog_channel_new, true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                @NonNull DialogAction which) {
                            RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                            String number = ((EditText) l.findViewById(R.id.number))
                                    .getText().toString();
                            Log.d(TAG, "Channel " + number);
                            String name = ((EditText) l.findViewById(R.id.name))
                                    .getText().toString();
                            String logo = ((EditText) l.findViewById(R.id.logo))
                                    .getText().toString();
                            String mediaUrl = ((EditText) l.findViewById(R.id.stream))
                                    .getText().toString();
                            String splash = ((EditText) l.findViewById(R.id.splash))
                                    .getText().toString();
                            String genres = ((Button) l.findViewById(R.id.genres))
                                    .getText().toString();

                            if(number.length() == 0) {
                                Toast.makeText(MainPicker.this,
                                        R.string.toast_error_channel_number,
                                        Toast.LENGTH_SHORT).show();
                            } else if (name.length() == 0) {
                                Toast.makeText(MainPicker.this,
                                        R.string.toast_error_channel_name,
                                        Toast.LENGTH_SHORT).show();
                            } else if (mediaUrl.length() == 0) {
                                Toast.makeText(MainPicker.this,
                                        R.string.toast_error_channel_media_url,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                CumulusChannel jsonChannel = new JsonChannel.Builder()
                                        .setName(name)
                                        .setNumber(number)
                                        .setMediaUrl(mediaUrl)
                                        .setLogo(logo)
                                        .setSplashscreen(splash)
                                        .setGenres(genres)
                                        .build();
                                saveChannel(jsonChannel);
                            }
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                @NonNull DialogAction which) {
                            RelativeLayout l = (RelativeLayout) dialog.getCustomView();
                            String number = ((EditText) l.findViewById(R.id.stream)).getText()
                                    .toString();
                            if (DEBUG) {
                                Log.d(TAG, "Channel " + number);
                                Log.d(TAG, "DEL Channel " + number);
                            }
                            String name = ((EditText) l.findViewById(R.id.name)).getText()
                                    .toString();
                            String logo = ((EditText) l.findViewById(R.id.logo)).getText()
                                    .toString();
                            String stream = ((EditText) l.findViewById(R.id.stream)).getText()
                                    .toString();
                            String splash = ((EditText) l.findViewById(R.id.splash)).getText()
                                    .toString();
                            String genres = ((Button) l.findViewById(R.id.genres)).getText()
                                    .toString();

                            CumulusChannel jsonChannel = new JsonChannel.Builder()
                                    .setNumber(number)
                                    .setName(name)
                                    .setMediaUrl(stream)
                                    .setLogo(logo)
                                    .setSplashscreen(splash)
                                    .setGenres(genres)
                                    .build();
                            deleteChannel(jsonChannel);
                        }
                    })
                    .build();
            mDialog.findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainPicker.this, CumulusTvPlayer.class);
                    i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, getUrl());
                    startActivity(i);
                }
            });
            if (AppUtils.isTV(context)) {
                mDialog.getCustomView().findViewById(R.id.stream_open)
                        .setVisibility(View.GONE);
            }
            final CumulusChannel cumulusChannel = getChannel();
            if (cumulusChannel != null && cumulusChannel.getGenresString() != null) {
                includeGenrePicker(MobilePickerDialog.this, cumulusChannel.getGenresString());
            } else {
                includeGenrePicker(MobilePickerDialog.this, "");
            }

            ((EditText) mDialog.getCustomView().findViewById(R.id.number))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) mDialog.getCustomView().findViewById(R.id.name))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) mDialog.getCustomView().findViewById(R.id.stream))
                    .addTextChangedListener(filterTextWatcher);

            mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    RelativeLayout l = (RelativeLayout) mDialog.getCustomView();
                    if (cumulusChannel != null) {
                        ((EditText) l.findViewById(R.id.number)).setText(cumulusChannel.getNumber());
                        Log.d(TAG, "Channel " + cumulusChannel.getNumber());
                        ((EditText) l.findViewById(R.id.name)).setText(cumulusChannel.getName());
                        ((EditText) l.findViewById(R.id.logo)).setText(cumulusChannel.getLogo());
                        ((EditText) l.findViewById(R.id.stream)).setText(cumulusChannel.getMediaUrl());
                        ((Button) l.findViewById(R.id.genres)).setText(cumulusChannel.getGenresString());
                    }

                    loadStream(MobilePickerDialog.this);
                }
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadValidation();
                }
            }, 1000);
            return mDialog;
        }

        public MainPicker.IMainPicker show(Context context, boolean isNewChannel) {
            mDialog = build(context, isNewChannel);
            mDialog.show();
            return this;
        }

        public View getCustomView() {
            return mDialog.getCustomView();
        }

        @Override
        public void handlePlaybackError(Exception e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void handlePlaybackSuccess() {
        }
    }

    private class TvPickerDialog extends PickerDialog {
        private Context mContext;
        private boolean mIsNewChannel;

        protected TvPickerDialog() {
        }

        @Override
        public void loadValidation() {
            boolean isValid = true;
            String number = String.valueOf(
                    ((EditText) findViewById(R.id.number)).getText());
            if(number.length() == 0) {
                findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String name = String.valueOf(
                    ((EditText) findViewById(R.id.name)).getText());
            if(name.length() == 0) {
                findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String mediaUrl = String.valueOf(
                    ((EditText) findViewById(R.id.stream)).getText());
            if(mediaUrl.length() == 0) {
                findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                loadStream(TvPickerDialog.this, getUrl());
                findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            Log.d(TAG, "'" + number + "', '" + name + "', '" + mediaUrl + "'");

            if (isValid) {
                ((Button) findViewById(R.id.positive_button)).setText(
                        mIsNewChannel ? mContext.getString(R.string.create) :
                        mContext.getString(R.string.update));
                findViewById(R.id.positive_button).setVisibility(View.VISIBLE);
                Log.w(TAG, "Yes this is a valid channel.");
            } else {
                findViewById(R.id.positive_button).setVisibility(View.INVISIBLE);
                Log.w(TAG, "This is not a valid channel.");
            }
        }

        @Override
        public IMainPicker show(Context context, boolean isNewChannel) {
            setContentView(R.layout.dialog_channel_new);
            mContext = context;
            mIsNewChannel = isNewChannel;

            ((TextView) findViewById(R.id.title)).setText(isNewChannel ?
                    context.getString(R.string.manage_add_new) :
                    context.getString(R.string.edit_channel));
            if (isNewChannel) {
                findViewById(R.id.negative_button).setVisibility(View.GONE);
            }

            findViewById(R.id.positive_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String number = ((EditText) findViewById(R.id.number))
                            .getText().toString();
                    String name = ((EditText) findViewById(R.id.name))
                            .getText().toString();
                    String logo = ((EditText) findViewById(R.id.logo))
                            .getText().toString();
                    String mediaUrl = ((EditText) findViewById(R.id.stream))
                            .getText().toString();
                    String splash = ((EditText) findViewById(R.id.splash))
                            .getText().toString();
                    String genres = ((Button) findViewById(R.id.genres))
                            .getText().toString();

                    if(number.length() == 0) {
                        Toast.makeText(MainPicker.this,
                                R.string.toast_error_channel_number,
                                Toast.LENGTH_SHORT).show();
                    } else if (name.length() == 0) {
                        Toast.makeText(MainPicker.this,
                                R.string.toast_error_channel_name,
                                Toast.LENGTH_SHORT).show();
                    } else if (mediaUrl.length() == 0) {
                        Toast.makeText(MainPicker.this,
                                R.string.toast_error_channel_media_url,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        CumulusChannel jsonChannel = new JsonChannel.Builder()
                                .setName(name)
                                .setNumber(number)
                                .setMediaUrl(mediaUrl)
                                .setLogo(logo)
                                .setSplashscreen(splash)
                                .setGenres(genres)
                                .build();
                        saveChannel(jsonChannel);
                        finish();
                    }
                }
            });

            findViewById(R.id.neutral_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            findViewById(R.id.negative_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String number = ((EditText) findViewById(R.id.stream)).getText()
                            .toString();
                    if (DEBUG) {
                        Log.d(TAG, "Channel " + number);
                        Log.d(TAG, "DEL Channel " + number);
                    }
                    String name = ((EditText) findViewById(R.id.name)).getText()
                            .toString();
                    String logo = ((EditText) findViewById(R.id.logo)).getText()
                            .toString();
                    String stream = ((EditText) findViewById(R.id.stream)).getText()
                            .toString();
                    String splash = ((EditText) findViewById(R.id.splash)).getText()
                            .toString();
                    String genres = ((Button) findViewById(R.id.genres)).getText()
                            .toString();

                    CumulusChannel jsonChannel = new JsonChannel.Builder()
                            .setNumber(number)
                            .setName(name)
                            .setMediaUrl(stream)
                            .setLogo(logo)
                            .setSplashscreen(splash)
                            .setGenres(genres)
                            .build();
                    deleteChannel(jsonChannel);
                    finish();
                }
            });
            CumulusChannel cumulusChannel = getChannel();
            if (cumulusChannel != null && cumulusChannel.getGenresString() != null) {
                includeGenrePicker(TvPickerDialog.this, cumulusChannel.getGenresString());
            } else {
                includeGenrePicker(TvPickerDialog.this, "");
            }

            ((EditText) findViewById(R.id.number))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) findViewById(R.id.name))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) findViewById(R.id.stream))
                    .addTextChangedListener(filterTextWatcher);

            CumulusChannel jsonChannel = getChannel();
            if (jsonChannel != null) {
                ((EditText) findViewById(R.id.number)).setText(jsonChannel.getNumber());
                Log.d(TAG, "Channel " + jsonChannel.getNumber());
                ((EditText) findViewById(R.id.name)).setText(jsonChannel.getName());
                ((EditText) findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                ((EditText) findViewById(R.id.stream)).setText(jsonChannel.getMediaUrl());
                ((Button) findViewById(R.id.genres)).setText(jsonChannel.getGenresString());
            }

            loadStream(TvPickerDialog.this);
            loadValidation();
            return this;
        }

        @Override
        public View getCustomView() {
            return findViewById(R.id.picker);
        }

        @Override
        public void handlePlaybackError(Exception e) {
            ((TextView) findViewById(R.id.playback_error)).setText(e.getMessage());
        }

        @Override
        public void handlePlaybackSuccess() {
            ((TextView) findViewById(R.id.playback_error)).setText("");
        }
    }

    private interface IMainPicker {
        void loadValidation();
        IMainPicker show(Context context, boolean isNewChannel);
        View getCustomView();
        void handlePlaybackError(Exception e);
        void handlePlaybackSuccess();
    }
}
