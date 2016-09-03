package com.felkertech.n;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.channelsurfer.utils.LiveChannelsUtils;
import com.felkertech.n.boilerplate.Utils.AppUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.boilerplate.Utils.PermissionUtils;
import com.felkertech.n.cumulustv.Intro.Intro;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.activities.CumulusTvPlayer;
import com.felkertech.n.cumulustv.activities.MainActivity;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.plugins.CumulusTvPlugin;
import com.felkertech.n.tv.activities.LeanbackActivity;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guest1 on 10/29/2015.
 */
public class ActivityUtils {
    private static final String TAG = ActivityUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    public static final int REQUEST_CODE_CREATOR = 102;
    public static final int REQUEST_CODE_OPENER = 104;
    public static final ComponentName TV_INPUT_SERVICE =
            new ComponentName("com.felkertech.n.cumulustv", ".CumulusTvService");

    public final static int LAST_GOOD_BUILD = 27;
    /* SUGGESTED CHANNELS */
    public static JsonChannel[] getSuggestedChannels() {
                /*
                0xx NEWS
                1xx SCIENCE/TECH/NATURE
                2xx HISTORY/EDUCATION
                3xx SPORTS/VIDEO GAMES
                4xx MUSIC
                5xx FICTION
                6xx NONFICTION
                7xx GOVERNMENT/SOCIETY
                9xx MISC
                 */
        final JsonChannel[] channels = { /* Some via http://rgw.ustream.tv/json.php/Ustream.searchBroadcast/ */
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.TECH_SCIENCE)
                        .setLogo("http://static-cdn1.ustream.tv/i/channel/live/1_6540154,256x144," +
                                "b:2015071514.jpg")
                        .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/6540154/streams/liv" +
                                "e/iphone/playlist.m3u8")
                        .setName("NASA Public")
                        .setNumber("100")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.TECH_SCIENCE)
                        .setLogo("http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562" +
                                "/9408562_iss_hr_1330361780,256x144,r:1.jpg")
                        .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/9408562/streams/liv" +
                                "e/iphone/playlist.m3u8")
                        .setName("ISS Stream")
                        .setNumber("101")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.TECH_SCIENCE + "," +
                                TvContract.Programs.Genres.NEWS)
                        .setLogo("http://wiki.twit.tv//w//images//TWiT-horizontal.png")
                        .setMediaUrl("http://twit.live-s.cdn.bitgravity.com/cdn-live-s1/_definst_" +
                                "/twit/live/high/playlist.m3u8")
                        .setName("TWiT.tv")
                        .setNumber("133")
                        .build(),
                new JsonChannel.Builder()
                        .setLogo("http://static-cdn1.ustream.tv/i/channel/live/1_9600798,256x144," +
                                "b:2015071514.jpg")
                        .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/9600798/streams/liv" +
                                "e/iphone/playlist.m3u8")
                        .setName("Monterey Bay Aquarium")
                        .setNumber("167")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.MUSIC)
                        .setMediaUrl("http://pablogott.videocdn.scaleengine.net/pablogott-iphone/" +
                                "play/ooftv1/playlist.m3u8")
                        .setNumber("400")
                        .setName("OutOfFocus.TV")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.MUSIC)
                        .setLogo("http://payload247.cargocollective.com/1/9/312377/7259316/hits.jpg")
                        .setMediaUrl("http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch1/a" +
                                "ppleman.m3u8")
                        .setName("VEVO TV Hits")
                        .setNumber("401")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.MUSIC)
                        .setLogo("http://payload247.cargocollective.com/1/9/312377/7259316/flow.jpg")
                        .setMediaUrl("http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch2/a" +
                                "ppleman.m3u8")
                        .setName("VEVO TV Flow")
                        .setNumber("402")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.MUSIC)
                        .setLogo("http://payload247.cargocollective.com/1/9/312377/7259316/nashville.jpg")
                        .setMediaUrl("http://vevoplaylist-live.hls.adaptive.level3.net/vevo/ch3/a" +
                                "ppleman.m3u8")
                        .setName("VEVO TV Nashville")
                        .setNumber("403")
                        .build(),
                new JsonChannel.Builder()
                        .setAudioOnly(true)
                        .setGenres(TvContract.Programs.Genres.MUSIC + "," +
                                TvContract.Programs.Genres.ENTERTAINMENT)
                        .setLogo("https://ottleyboothr.files.wordpress.com/2015/06/beats-1.jpg")
                        .setMediaUrl("http://itsliveradio.apple.com/streams/master_session01_hub0" +
                                "1_hub02.m3u8")
                        .setName("Beats One Radio")
                        .setNumber("410")
                        .build(),
                new JsonChannel.Builder()
                        .setGenres(TvContract.Programs.Genres.ARTS + "," +
                                TvContract.Programs.Genres.ENTERTAINMENT)
                        .setLogo("http://content.provideocoalition.com/uploads/ArtbeatsLogo_black" +
                                "box.jpg")
                        .setMediaUrl("http://cdn-fms.rbs.com.br/hls-vod/sample1_1500kbps.f4v.m3u8")
                        .setName("Artbeats Demo")
                        .setNumber("900")
                        .build()
        };
        return channels;
    }
    public static void openSuggestedChannels(final Activity mActivity, final GoogleApiClient gapi) {
        final JsonChannel[] channels = getSuggestedChannels();
        ArrayList<String> channeltext = new ArrayList<String>();
        for(JsonChannel j: channels) {
            channeltext.add(j.getName());
        }
        final String[] channelList = channeltext.toArray(new String[channeltext.size()]);
        new MaterialDialog.Builder(mActivity)
                .title(R.string.suggested_channels)
                .items(channelList)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        JsonChannel j = channels[i];
                        addChannel(mActivity, gapi, j, charSequence+"");
                    }
                }).show();
    }
    public static void addChannel(Activity mActivity, GoogleApiClient gapi, JsonChannel j,
            String name) {
        if (DEBUG) {
            Log.d(TAG, "I've been told to add " + j.toString());
        }
        ChannelDatabase cd = ChannelDatabase.getInstance(mActivity);
        if(cd.channelExists(j)) {
            Toast.makeText(mActivity, R.string.channel_dupe, Toast.LENGTH_SHORT).show();
        } else {
            try {
                if(name != null)
                    Toast.makeText(mActivity, mActivity.getString(R.string.channel_added, name), Toast.LENGTH_SHORT).show();
                cd.add(j);
                ActivityUtils.writeDriveData(mActivity, gapi);
                if (DEBUG) {
                    Log.d(TAG, "Added");
                }
//                SyncUtils.requestSync(info);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Opens the correct intent to start editing the channel.
     *
     * @param activity The activity you're calling this from.
     * @param channelUrl The channel's media url.m
     */
    public static void editChannel(final Activity activity, final String channelUrl) {
        ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
        final JsonChannel jsonChannel = cdn.findChannelByMediaUrl(channelUrl);
        if(channelUrl == null || jsonChannel == null) {
            Toast.makeText(activity, R.string.toast_error_channel_invalid,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if(jsonChannel.getPluginSource() != null) {
            //Search through all plugins for one of a given source
            PackageManager pm = activity.getPackageManager();

            try {
                pm.getPackageInfo(jsonChannel.getPluginSource().getPackageName(),
                        PackageManager.GET_ACTIVITIES);
                //Open up this particular activity
                Intent intent = new Intent();
                intent.setComponent(jsonChannel.getPluginSource());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getMediaUrl());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                activity.startActivity(intent);
            }
            catch (PackageManager.NameNotFoundException e) {
                new MaterialDialog.Builder(activity)
                        .title(activity.getString(R.string.plugin_not_installed_title,
                                jsonChannel.getPluginSource().getPackageName()))
                        .content(R.string.plugin_not_installed_question)
                        .positiveText(R.string.download_app)
                        .negativeText(R.string.open_in_another_plugin)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse("http://play.google.com/store/apps/details?id="
                                        + jsonChannel.getPluginSource().getPackageName()));
                                activity.startActivity(i);
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                                openPluginPicker(false, channelUrl, activity);
                            }
                        }).show();
                Toast.makeText(activity, activity.getString(R.string.toast_msg_pack_not_installed,
                        jsonChannel.getPluginSource().getPackageName()), Toast.LENGTH_SHORT).show();
                openPluginPicker(false, channelUrl, activity);
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "No specified source");
            }
            openPluginPicker(false, channelUrl, activity);
        }
    }

    /* DRIVE */
    public static void writeDriveData(final Activity context, GoogleApiClient gapi) {
        //Ask here for permission to storage
        PermissionUtils.requestPermissionIfDisabled(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                context.getString(R.string.permission_storage_rationale));
        if(PermissionUtils.isDisabled(context, android.Manifest.permission_group.STORAGE)) {
            new MaterialDialog.Builder(context)
                    .title(R.string.permission_not_allowed_error)
                    .content(R.string.permission_not_allowed_text)
                    .positiveText(R.string.permission_action_settings)
                    .negativeText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                            intent.setData(uri);
                        }
                    })
                    .build();
        } else
            actuallyWriteData(context, gapi);
    }

    public static void writeDriveData(final Context context, GoogleApiClient gapi) {
        // This can crash
        actuallyWriteData(context, gapi);
    }

    private static void actuallyWriteData(final Context context, GoogleApiClient gapi) {
        DriveSettingsManager sm = new DriveSettingsManager(context);
        sm.setGoogleDriveSyncable(gapi, new DriveSettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Action finished " + cloudToLocal);
            }
        });
        try {
            sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)),
                    ChannelDatabase.getInstance(context).toString());

            final String info = TvContract.buildInputId(TV_INPUT_SERVICE);
            SyncUtils.requestSync(context, info);
        } catch(Exception e) {
            //Probably invalid drive id. No worries, just let someone know
            Log.e(TAG, e.getMessage() + "");
            Toast.makeText(context, R.string.invalid_file, Toast.LENGTH_SHORT).show();
        }
    }

    public static void readDriveData(@NonNull Context mContext, GoogleApiClient gapi) {
        DriveSettingsManager sm = new DriveSettingsManager(mContext);
        sm.setGoogleDriveSyncable(gapi, null);
        DriveId did;
        try {
            did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
        } catch (Exception e) {
            Toast.makeText(mContext, R.string.invalid_file, Toast.LENGTH_SHORT).show();
            return;
        }
        sm.readFromGoogleDrive(did, ChannelDatabase.KEY);

        final String info = TvContract.buildInputId(TV_INPUT_SERVICE);
        SyncUtils.requestSync(mContext, info);
    }

    public static void createDriveData(Activity activity, GoogleApiClient gapi,
            final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback) {
        PermissionUtils.requestPermissionIfDisabled(activity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                activity.getString(R.string.permission_storage_rationale));
        if(gapi == null)
            gapi = GoogleDrive.gapi;
        try {
            final GoogleApiClient finalGapi = gapi;
            new MaterialDialog.Builder(activity)
                    .title(R.string.create_sync_file_title)
                    .content(R.string.create_sync_file_description)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.no)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Drive.DriveApi.newDriveContents(finalGapi)
                                    .setResultCallback(driveContentsCallback);
                        }
                    })
                    .show();
        } catch(Exception ignored) {}
    }

    public static void switchGoogleDrive(Activity mActivity, GoogleApiClient gapi) {
        syncFile(mActivity, gapi);
    }

    public static void deleteChannelData(final Activity mActivity, GoogleApiClient gapi) {
        final DriveSettingsManager sm = new DriveSettingsManager(mActivity);
        sm.setGoogleDriveSyncable(gapi, null);
        new MaterialDialog.Builder(mActivity)
                .title(R.string.title_delete_all_channels)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        sm.setString(ChannelDatabase.KEY, "{'channels':[]}");
                        try {
                            DriveId did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
                            sm.writeToGoogleDrive(did,
                                    sm.getString(ChannelDatabase.KEY));
                        } catch (Exception e) {
                            Toast.makeText(mActivity, R.string.toast_error_driveid_invalid,
                                    Toast.LENGTH_SHORT).show();
                        }
                        sm.setString(R.string.sm_google_drive_id, "");
                        Toast.makeText(mActivity, R.string.toast_msg_channels_deleted,
                                Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(mActivity, MainActivity.class);
                        mActivity.startActivity(i);
                    }
                })
                .show();
    }

    public static void syncFile(Activity mActivity, GoogleApiClient gapi) {
        if (DEBUG) {
        Log.d(TAG, "About to sync a file");
        }
        if (gapi == null && GoogleDrive.gapi != null) {
            gapi = GoogleDrive.gapi;
        } else if(GoogleDrive.gapi == null) {
            // Is not existant
            return;
        }
        if (gapi.isConnected()) {
            IntentSender intentSender = Drive.DriveApi
                    .newOpenFileActivityBuilder()
                    .setMimeType(new String[]{"application/json", "text/*"})
                    .build(gapi);
            try {
                if (DEBUG) {
                    Log.d(TAG, "About to start activity");
                }
                mActivity.startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0,
                        0);
                if (DEBUG) {
                    Log.d(TAG, "Activity activated");
                }
            } catch (IntentSender.SendIntentException e) {
                if (DEBUG) {
                    Log.w(TAG, "Unable to send intent", e);
                }
                e.printStackTrace();
            }
        } else {
            Toast.makeText(mActivity, R.string.toast_msg_wait_google_api_client,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /* LICENSES */
    public static void oslClick(Activity activity) {
        new MaterialDialog.Builder(activity)
                .title(R.string.software_licenses)
                .content(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity))
                .show();
    }

    /* PLUGIN INTERFACES */
    public static void openPluginPicker(final boolean newChannel, Activity activity) {
        openPluginPicker(newChannel, 0, activity);
    }

    public static void openPluginPicker(final boolean newChannel, final int index,
                                        final Activity activity) {
        try {
            ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
            if(cdn.getJsonChannels().isEmpty()) {
                openPluginPicker(newChannel, new JsonChannel.Builder().build(), activity);
            } else {
                JsonChannel jsonChannel = cdn.getJsonChannels().get(index);
                openPluginPicker(newChannel, jsonChannel, activity);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void openPluginPicker(final boolean newChannel, final String channelUrl,
            final Activity activity) {
        ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
        openPluginPicker(newChannel, cdn.findChannelByMediaUrl(channelUrl), activity);
    }

    public static void openPluginPicker(final boolean newChannel, final JsonChannel queriedChannel,
            final Activity activity) {
        final PackageManager pm = activity.getPackageManager();
        final Intent plugin_addchannel = new Intent(CumulusTvPlugin.ACTION_ADD_CHANNEL);
        final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
        ArrayList<String> plugin_names = new ArrayList<String>();
        for (ResolveInfo ri : plugins) {
            plugin_names.add(ri.loadLabel(pm).toString());
        }
        String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);
        if (DEBUG) {
            Log.d(TAG, "Load plugins " + plugin_names.toString());
        }
        if(plugin_names.size() == 1) {
            Intent intent = new Intent();
            if (newChannel) {
                if (DEBUG) {
                    Log.d(TAG, "Try to start ");
                }
                ResolveInfo plugin_info = plugins.get(0);
                if (DEBUG) {
                    Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                            plugin_info.activityInfo.name);
                }

                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                        plugin_info.activityInfo.name);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
            } else {
                ResolveInfo plugin_info = plugins.get(0);
                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                        plugin_info.activityInfo.name);
                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                        plugin_info.activityInfo.name);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, queriedChannel.getNumber());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, queriedChannel.getName());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, queriedChannel.getMediaUrl());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, queriedChannel.getLogo());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH,
                        queriedChannel.getSplashscreen());
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES,
                        queriedChannel.getGenresString());
            }
            activity.startActivity(intent);
        } else {
            new MaterialDialog.Builder(activity)
                    .items(plugin_names2)
                    .title(R.string.choose_an_app)
                    .content(R.string.choose_default_app)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            //Toast.makeText(activity, "Pick " + i, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            if (newChannel) {
                                if (DEBUG) {
                                    Log.d(TAG, "Try to start ");
                                }
                                ResolveInfo plugin_info = plugins.get(i);
                                if (DEBUG) {
                                    Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName
                                            + " " + plugin_info.activityInfo.name);
                                }

                                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                        plugin_info.activityInfo.name);

                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION,
                                        CumulusTvPlugin.INTENT_ADD);
                            } else {
                                ResolveInfo plugin_info = plugins.get(i);
                                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                        plugin_info.activityInfo.name);
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION,
                                        CumulusTvPlugin.INTENT_EDIT);
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER,
                                        queriedChannel.getNumber());
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME,
                                        queriedChannel.getName());
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL,
                                        queriedChannel.getMediaUrl());
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON,
                                        queriedChannel.getLogo());
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH,
                                        queriedChannel.getSplashscreen());
                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES,
                                        queriedChannel.getGenresString());
                            }
                            activity.startActivity(intent);
                        }
                    }).show();
        }
    }

    public static void browsePlugins(final Activity activity) {
        //Same opening
        final PackageManager pm = activity.getPackageManager();
        final Intent plugin_addchannel = new Intent(CumulusTvPlugin.ACTION_ADD_CHANNEL);
        final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
        ArrayList<String> plugin_names = new ArrayList<String>();
        for (ResolveInfo ri : plugins) {
            plugin_names.add(ri.loadLabel(pm).toString());
        }
        String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);

        new MaterialDialog.Builder(activity)
                .title(R.string.installed_plugins)
                .items(plugin_names2)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        //Load the given plugin with some additional info
                        ChannelDatabase cd = ChannelDatabase.getInstance(activity);
                        String s = cd.toString();
                        Intent intent = new Intent();
                        if (DEBUG) {
                            Log.d(TAG, "Try to start");
                        }
                        ResolveInfo plugin_info = plugins.get(i);
                        Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                                plugin_info.activityInfo.name);

                        intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                plugin_info.activityInfo.name);
                        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION,
                                CumulusTvPlugin.INTENT_EXTRA_READ_ALL);
                        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ALL_CHANNELS, s);
                        activity.startActivity(intent);
                    }
                })
                .positiveText(R.string.download_more_plugins)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(
                                "http://play.google.com/store/search?q=cumulustv&c=apps"));
                        activity.startActivity(i);
                    }
                }).show();
    }

    /* ACTIVITY CLONES */
    public static void launchLiveChannels(Activity mActivity) {
        Intent i = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
        try {
            mActivity.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(mActivity, R.string.no_live_channels, Toast.LENGTH_SHORT).show();
        }
    }

    public static void onActivityResult(final Activity mActivity, GoogleApiClient gapi,
            final int requestCode, final int resultCode, final Intent data) {
        SettingsManager sm = new SettingsManager(mActivity);
        if(gapi == null)
            gapi = GoogleDrive.gapi;
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (DEBUG) {
                        Log.d(TAG, "App connect +1");
                    }
                    gapi.connect();
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "App cannot connect");
                    }
                    final GoogleApiClient finalGapi = gapi;
                    new MaterialDialog.Builder(mActivity)
                            .title(R.string.connection_issue_title)
                            .content(R.string.connection_issue_description)
                            .positiveText(R.string.ok)
                            .negativeText(R.string.try_again)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    super.onNegative(dialog);
                                    finalGapi.connect();
                                }
                            }).show();
                }
                break;
            case REQUEST_CODE_CREATOR:
                if (data == null || !gapi.isConnected()) {
                    // If op was canceled
                    return;
                }
                DriveId driveId = data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                if (DEBUG) {
                    Log.d(TAG, driveId.encodeToString() + ", " + driveId.getResourceId() + ", " +
                            driveId.toInvariantString());
                }
                sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());

                DriveFile file = Drive.DriveApi.getFile(gapi,
                        DriveId.decodeFromString(driveId.encodeToString()));
                //Write initial data
                ActivityUtils.writeDriveData(mActivity, gapi);
                break;
            case ActivityUtils.REQUEST_CODE_OPENER:
                if (data == null) //If op was canceled
                    return;
                driveId = data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                if (DEBUG) {
                    Log.d(TAG, driveId.encodeToString() + ", " + driveId.getResourceId() + ", " +
                            driveId.toInvariantString());
                }
                sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());
                final GoogleApiClient finalGapi1 = gapi;
                new MaterialDialog.Builder(mActivity)
                        .title(R.string.sync_initial)
                        .positiveText(R.string.sync_cloud_local)
                        .negativeText(R.string.sync_local_cloud)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                ActivityUtils.readDriveData(mActivity, finalGapi1);
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                                ActivityUtils.writeDriveData(mActivity, finalGapi1);
                            }
                        })
                        .show();
        }
    }

    /* MISC */
    public static void openAbout(final Activity mActivity) {
        try {
            PackageInfo pInfo = mActivity.getPackageManager().getPackageInfo(
                    mActivity.getPackageName(), 0);
            new MaterialDialog.Builder(mActivity)
                    .title(R.string.app_name)
                    .content(mActivity.getString(R.string.about_app_description, pInfo.versionName))
                    .positiveText(R.string.website)
                    .negativeText(R.string.help)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Intent gi = new Intent(Intent.ACTION_VIEW);
                            gi.setData(Uri.parse("http://cumulustv.herokuapp.com"));
                            mActivity.startActivity(gi);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            ActivityUtils.openIntroVoluntarily(mActivity);
                        }
                    })
                    .show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openStream(Activity mActivity, String url) {
        Intent i = new Intent(mActivity, CumulusTvPlayer.class);
        i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, url);
        mActivity.startActivity(i);
    }

    public static void openIntroIfNeeded(Activity mActivity) {
        SettingsManager sm = new SettingsManager(mActivity);
        if(sm.getInt(R.string.sm_last_version) < LAST_GOOD_BUILD) {
            mActivity.startActivity(new Intent(mActivity, Intro.class));
            mActivity.finish();
        }
    }

    public static void openIntroVoluntarily(Activity mActivity) {
        mActivity.startActivity(new Intent(mActivity, Intro.class));
        mActivity.finish();
    }

    public static Class getMainActivity(Activity mActivity) {
        if(AppUtils.isTV(mActivity)) {
            return LeanbackActivity.class;
        }
        return MainActivity.class;
    }

    public static class GoogleDrive {
        public static GoogleApiClient gapi;

        public static GoogleApiClient connect(Activity mActivity) {
            gapi = new GoogleApiClient.Builder(mActivity)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) mActivity)
                    .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener)
                            mActivity)
                    .build();
            gapi.connect();
            return gapi;
        }

        public static boolean autoConnect(Activity mActivity) {
            if(isDriveEnabled(mActivity)) {
                if (DEBUG) {
                    Log.d(TAG, "Drive is enabled, automatically connect");
                    Log.d(TAG, ">" + new SettingsManager(mActivity).getString(R.string.sm_google_drive_id).length());
                    Log.d(TAG, new SettingsManager(mActivity).getString(R.string.sm_google_drive_id) + "<");
                }
                connect(mActivity);
                return true;
            }
            if (DEBUG) {
                Log.d(TAG, "Drive is not enabled, don't connect yet.");
            }
            return false;
        }

        public static boolean isDriveEnabled(Activity mActivity) {
            String gdriveId = new SettingsManager(mActivity).getString(R.string.sm_google_drive_id);
            return gdriveId.isEmpty() && gdriveId.length() > 0;
        }

        public static boolean isDriveConnected() {
            return gapi.isConnected();
        }

        public static void pickDriveFile(Activity mActivity) {
            ActivityUtils.syncFile(mActivity, gapi);
        }
    }
}

/* new JsonChannel("001",
        "Sky News",
        "https://www.youtube.com/embed/y60wDzZt8yg?autoplay=1",
        "http://news.sky.com/images/33dc2677.sky-news-logo.png", "",
        TvContract.Programs.Genres.NEWS),
new JsonChannel("002",
        "Taiwan Formosa Live News",
        "https://www.youtube.com/embed/XxJKnDLYZz4?autoplay=1",
        "https://i.ytimg.com/vi/XxJKnDLYZz4/maxresdefault_live.jpg", "",
        TvContract.Programs.Genres.NEWS),*/
/*
        new JsonChannel("900", "Euronews De", "http://fr-par-iphone-2.cdn.hexaglobe.net/streaming/euronews_ewns/14-live.m3u8", ""),
        new JsonChannel("901", "TVI (Portugal)", "http://noscdn1.connectedviews.com:1935/live/smil:tvi.smil/playlist.m3u8", ""),
        new JsonChannel("902", "PHOENIXHD", "http://teleboy.customers.cdn.iptv.ch/1122/index.m3u8", ""),
        new JsonChannel("903", "Sport 1 Germany", "http://streaming-hub.com/tv/i/sport1_1@97464/index_1300_av-p.m3u8?sd=10&rebase=on", ""),
        new JsonChannel("904", "RTP International", "http://rtp-pull-live.hls.adaptive.level3.net/liverepeater/rtpi_5ch120h264.stream/livestream.m3u8", "")
*/
