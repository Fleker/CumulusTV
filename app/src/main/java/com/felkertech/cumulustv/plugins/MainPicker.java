package com.felkertech.cumulustv.plugins;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.activities.CumulusVideoPlayback;
import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.FileParserFactory;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.LocalFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.player.CumulusTvPlayer;
import com.felkertech.cumulustv.utils.AppUtils;
import com.felkertech.cumulustv.utils.PermissionUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.settingsmanager.common.CommaArray;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.media.tv.companionlibrary.TvPlayer;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * For the sake of open source software and examples, the built-in picker will be a plugin
 * Created by Nick on 8/7/2015.
 */
public class MainPicker extends CumulusTvPlugin {
    private static final String TAG = MainPicker.class.getSimpleName();
    private static final boolean DEBUG = true;

    @VisibleForTesting
    /**
     * @hide
     */
    public static View streamView;

    private String label = "";
    private IMainPicker mPickerDialog;
    private CumulusTvPlayer mTvPlayer;
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
            if(uri.toString().endsWith("m3u8")) { // Import a single channel
                // Copy from `loadDialogs()` in edit mode
                mPickerDialog = getPicker().show(this, true);
                RelativeLayout l = (RelativeLayout) mPickerDialog.getCustomView();
                ((EditText) l.findViewById(R.id.stream)).setText(uri.toString());
                loadStream(mPickerDialog, uri.toString());
            } else if (uri.toString().endsWith("m3u")) {
                Log.d(TAG, "Import m3u playlist");
                Toast.makeText(this, R.string.loading_data, Toast.LENGTH_SHORT).show();
                FileParserFactory.parseGenericFileUri(uri.toString(),
                        new FileParserFactory.FileIdentifier() {
                    @Override
                    public void onLocalFile(String uri) {
                        try {
                            new LocalFileParser(uri, new AbstractFileParser.FileLoader() {
                                @Override
                                public void onFileLoaded(InputStream inputStream) {
                                    importChannels(inputStream);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            Toast.makeText(MainPicker.this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAsset(String uri) {
                        throw new UnsupportedOperationException("Don't import from assets pls");
                    }

                    @Override
                    public void onHttpFile(String uri) {
                        new HttpFileParser(uri, new AbstractFileParser.FileLoader() {
                            @Override
                            public void onFileLoaded(InputStream inputStream) {
                                importChannels(inputStream);
                            }
                        });
                    }
                });
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
        if (mPickerDialog != null) {
            SurfaceView sv = (SurfaceView) mPickerDialog.getCustomView().findViewById(R.id.surface);
            sv.getHolder().getSurface().release();
        }
        if (mTvPlayer != null) {
            mTvPlayer.setSurface(null);
            mTvPlayer.stop();
            mTvPlayer.release();
        } else {
            Log.w(TAG, "TvInputPlayer is null.");
        }
        mTvPlayer = null;
        super.onStop();
    }

    private void importChannels(InputStream input) {
        try {
            Log.d(TAG, "Parse channels");
            final M3uParser.TvListing listings = M3uParser.parse(input);
            Log.d(TAG, "Import " + listings.channels.size() + " channels");
            new AlertDialog.Builder(MainPicker.this)
                    .setTitle(getString(R.string.import_bulk_title, listings.channels.size()))
                    .setMessage(listings.getChannelList())
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(MainPicker.this, R.string.import_bulk_wait,
                                    Toast.LENGTH_SHORT).show();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ChannelDatabase channelDatabase =
                                            ChannelDatabase.getInstance(MainPicker.this);
                                    for (M3uParser.M3uTvChannel channel :
                                            listings.channels) {
                                        try {
                                            channelDatabase.add(channel.toJsonChannel());
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
                                                    finish();
                                                }
                                            };
                                    finishedImporting.sendEmptyMessage(0);
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
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
                boolean[] selections = new boolean[ChannelDatabase.getAllGenres().length];
                for (int i = 0; i < ChannelDatabase.getAllGenres().length; i++) {
                    selections[i] = gString.contains(ChannelDatabase.getAllGenres()[i]);
                }
                final CommaArray genres = new CommaArray("");
                new AlertDialog.Builder(MainPicker.this)
                        .setTitle(R.string.select_genres)
                        .setMultiChoiceItems(ChannelDatabase.getAllGenres(), selections, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                if (b) {
                                    genres.add(ChannelDatabase.getAllGenres()[i]);
                                } else {
                                    genres.remove(ChannelDatabase.getAllGenres()[i]);
                                }
                                ((Button) picker.getCustomView().findViewById(R.id.genres))
                                        .setText(genres.toString());
                            }
                        })
                        .setPositiveButton(R.string.done, null)
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
        mTvPlayer = new CumulusTvPlayer(this);
        mTvPlayer.setSurface(sv.getHolder().getSurface());
        try {
            mTvPlayer.startPlaying(Uri.parse(url));
        } catch(Exception e) {
            //Do nothing
            Log.d(TAG, "IllegalArgumentException");
        }
        mTvPlayer.play();
        mTvPlayer.registerCallback(new TvPlayer.Callback() {
            @Override
            public void onStarted() {
                super.onStarted();
                mPickerDialog.handlePlaybackSuccess();
            }
        });
        mTvPlayer.registerErrorListener(new CumulusTvPlayer.ErrorListener() {
            @Override
            public void onError(Exception error) {
                mPickerDialog.handlePlaybackError(error);
            }
        });
    }

    public void loadStream(IMainPicker viewHolder, final String url) {
        SurfaceView sv = (SurfaceView) viewHolder.getCustomView().findViewById(R.id.surface);
        mTvPlayer = new CumulusTvPlayer(this);
        mTvPlayer.setSurface(sv.getHolder().getSurface());
        try {
            mTvPlayer.startPlaying(Uri.parse(url));
        } catch(Exception e) {
            //Do nothing
            Log.d(TAG, "IllegalArgumentException");
        }
        mTvPlayer.play();
        mTvPlayer.registerCallback(new TvPlayer.Callback() {
            @Override
            public void onStarted() {
                super.onStarted();
                mPickerDialog.handlePlaybackSuccess();
            }
        });
        mTvPlayer.registerErrorListener(new CumulusTvPlayer.ErrorListener() {
            @Override
            public void onError(Exception error) {
                mPickerDialog.handlePlaybackError(error);
            }
        });
    }

    public String getUrl() {
        String url = "";
        if(getChannel() != null) {
            url = getChannel().getMediaUrl();
            Log.d(TAG, "Have actual channel, have url " + url);
        }
        if(mPickerDialog != null &&
                mPickerDialog.getCustomView().findViewById(R.id.stream) != null) {
            url = ((EditText) mPickerDialog.getCustomView().findViewById(R.id.stream)).getText()
                    .toString();
        }
        if (DEBUG) {
            Log.d(TAG, "Found url '" + url + "'");
        }
        return url;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStream(mPickerDialog);
    }

    @VisibleForTesting
    @Deprecated
    public void getMediaUrl() {
        ((EditText) findViewById(R.id.stream)).getText();
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
        private AlertDialog mDialog;
        private Context mContext;
        private boolean mIsNewChannel;

        protected MobilePickerDialog() {
        }

        public void loadValidation() {
            boolean isValid = true;
            String number = String.valueOf(
                    ((EditText) mDialog.getWindow().findViewById(R.id.number)).getText());
            if(number.length() == 0) {
                mDialog.getWindow().findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                mDialog.getWindow().findViewById(R.id.number).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String name = String.valueOf(
                    ((EditText) mDialog.getWindow().findViewById(R.id.name)).getText());
            if(name.length() == 0) {
                mDialog.getWindow().findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                mDialog.getWindow().findViewById(R.id.name).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            String mediaUrl = String.valueOf(
                    ((EditText) mDialog.getWindow().findViewById(R.id.stream)).getText());
            if(mediaUrl.length() == 0) {
                mDialog.getWindow().findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.holo_red_dark));
                isValid = false;
            } else {
                loadStream(MobilePickerDialog.this, getUrl());
                mDialog.getWindow().findViewById(R.id.stream).setBackgroundColor(getResources()
                        .getColor(android.R.color.transparent));
            }

            Log.d(TAG, "'" + number + "', '" + name + "', '" + mediaUrl + "'");

            if (isValid) {
                mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mIsNewChannel ? mContext.getString(R.string.create) : mContext.getString(R.string.update), new Message());
                Log.w(TAG, "Yes this is a valid channel.");
            } else {
                mDialog.setButton(DialogInterface.BUTTON_POSITIVE, "", new Message());
                Log.w(TAG, "This is not a valid channel.");
            }
        }

        private String getPositiveButtonText() {
            return mIsNewChannel ? mContext.getString(R.string.create) :
                    mContext.getString(R.string.update);
        }

        private AlertDialog build(Context context, boolean isNewChannel) {
            mContext = context;
            mIsNewChannel = isNewChannel;
            mDialog = new AlertDialog.Builder(context)
                    .setTitle(isNewChannel ? context.getString(R.string.manage_add_new) :
                            context.getString(R.string.edit_channel))
                    .setNeutralButton(context.getString(R.string.cancel), null)
                    .setNegativeButton(isNewChannel ? null : context.getString(R.string.delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RelativeLayout l = (RelativeLayout) mDialog.getWindow().getDecorView();
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
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                        }
                    })
                    .setView(R.layout.dialog_channel_new)
                    .setPositiveButton(getPositiveButtonText(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RelativeLayout l = (RelativeLayout) mDialog.getWindow().getDecorView();
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
                            String epgUrl = ((EditText) l.findViewById(R.id.epg))
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
                                        .setEpgUrl(epgUrl)
                                        .setSplashscreen(splash)
                                        .setGenres(genres)
                                        .build();
                                saveChannel(jsonChannel);
                            }
                        }
                    })
                    .create();
            if (AppUtils.isTV(context)) {
                if (mDialog == null) {
                    Toast.makeText(context, "mDialog is null", Toast.LENGTH_SHORT);
                } else {
                    mDialog.getWindow().findViewById(R.id.stream_open)
                            .setVisibility(View.GONE);
                }
            } else {
                mDialog.getWindow().findViewById(R.id.stream_open).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(MainPicker.this, CumulusVideoPlayback.class);
                        i.putExtra(CumulusVideoPlayback.KEY_VIDEO_URL, getUrl());
                        startActivity(i);
                    }
                });
            }
            final CumulusChannel cumulusChannel = getChannel();
            if (cumulusChannel != null && cumulusChannel.getGenresString() != null) {
                includeGenrePicker(MobilePickerDialog.this, cumulusChannel.getGenresString());
            } else {
                includeGenrePicker(MobilePickerDialog.this, "");
            }

            ((EditText) mDialog.getWindow().findViewById(R.id.number))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) mDialog.getWindow().findViewById(R.id.name))
                    .addTextChangedListener(filterTextWatcher);
            ((EditText) mDialog.getWindow().findViewById(R.id.stream))
                    .addTextChangedListener(filterTextWatcher);

            mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    RelativeLayout l = (RelativeLayout) mDialog.getWindow().getDecorView();
                    if (DEBUG) {
                        Log.d(TAG, "Populate the form?");
                        Log.d(TAG, (cumulusChannel != null) + "  " + (getChannel() != null));
                    }
                    if (cumulusChannel != null) {
                        ((EditText) l.findViewById(R.id.number)).setText(cumulusChannel.getNumber());
                        Log.d(TAG, "Channel " + cumulusChannel.getNumber());
                        ((EditText) l.findViewById(R.id.name)).setText(cumulusChannel.getName());
                        ((EditText) l.findViewById(R.id.logo)).setText(cumulusChannel.getLogo());
                        ((EditText) l.findViewById(R.id.stream)).setText(cumulusChannel.getMediaUrl());
                        ((EditText) l.findViewById(R.id.epg)).setText(cumulusChannel.getEpgUrl());
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
            streamView = mDialog.getWindow().findViewById(R.id.stream);
            return this;
        }

        public View getCustomView() {
            return mDialog.getWindow().getDecorView();
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
            setContentView(R.layout.dialog_channel_new_tv);
            mContext = context;
            mIsNewChannel = isNewChannel;

            ((TextView) findViewById(R.id.title)).setText(isNewChannel ?
                    context.getString(R.string.manage_add_new) :
                    context.getString(R.string.edit_channel));
            if (isNewChannel) {
                findViewById(R.id.negative_button).setVisibility(View.GONE);
            }
            streamView = ((EditText) findViewById(R.id.stream));
            findViewById(R.id.positive_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String number = ((EditText) findViewById(R.id.number)).getText().toString();
                    String name = ((EditText) findViewById(R.id.name)).getText().toString();
                    String logo = ((EditText) findViewById(R.id.logo)).getText().toString();
                    String mediaUrl = ((EditText) findViewById(R.id.stream)).getText().toString();
                    String splash = ((EditText) findViewById(R.id.splash)).getText().toString();
                    String epgUrl = ((EditText) findViewById(R.id.epg)).getText().toString();
                    String genres = ((Button) findViewById(R.id.genres)).getText().toString();

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
                                .setEpgUrl(epgUrl)
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
                    String name = ((EditText) findViewById(R.id.name)).getText().toString();
                    String logo = ((EditText) findViewById(R.id.logo)).getText().toString();
                    String stream = ((EditText) findViewById(R.id.stream)).getText().toString();
                    String splash = ((EditText) findViewById(R.id.splash)).getText().toString();
                    String genres = ((Button) findViewById(R.id.genres)).getText().toString();

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

            if (cumulusChannel != null) {
                ((EditText) findViewById(R.id.number)).setText(cumulusChannel.getNumber());
                Log.d(TAG, "Channel " + cumulusChannel.getNumber());
                ((EditText) findViewById(R.id.name)).setText(cumulusChannel.getName());
                ((EditText) findViewById(R.id.logo)).setText(cumulusChannel.getLogo());
                ((EditText) findViewById(R.id.stream)).setText(cumulusChannel.getMediaUrl());
                ((EditText) findViewById(R.id.epg)).setText(cumulusChannel.getEpgUrl());
                ((Button) findViewById(R.id.genres)).setText(cumulusChannel.getGenresString());
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
