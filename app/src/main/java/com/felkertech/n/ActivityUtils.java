package com.felkertech.n;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.sampletvinput.syncadapter.SyncUtils;
import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.felkertech.n.cumulustv.ChannelDatabase;
import com.felkertech.n.cumulustv.JSONChannel;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.plugins.CumulusTvPlugin;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guest1 on 10/29/2015.
 */
public class ActivityUtils {
    private static final String TAG = "cumulus:ActivityUtils";
    /* DRIVE */
    public static void writeDriveData(Context context, GoogleApiClient gapi) {
        SettingsManager sm = new SettingsManager(context);
        sm.setGoogleDriveSyncable(gapi, new SettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Action finished "+cloudToLocal);
            }
        });
        try {
            sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)), new ChannelDatabase(context).toString());

            final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
            SyncUtils.requestSync(info);
        } catch(Exception e) {
            //Probably invalid drive id. No worries, just let someone know
            Log.e(TAG, e.getMessage() + "");
            Toast.makeText(context, "Invalid drive file. Please choose a different file.", Toast.LENGTH_SHORT).show();
        }
    }

    /* LICENSES */
    public static void oslClick(Activity activity) {
        new MaterialDialog.Builder(activity)
                .title("Software Licenses")
                .content(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity))
                .show();
    }

    /* PLUGIN INTERFACES */
    public static void openPluginPicker(final boolean newChannel, Activity activity) {
        openPluginPicker(newChannel, 0, activity);
    }
    public static void openPluginPicker(final boolean newChannel, final int index, final Activity activity) {
        final PackageManager pm = activity.getPackageManager();
        final Intent plugin_addchannel = new Intent("com.felkertech.cumulustv.ADD_CHANNEL");
        final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
        ArrayList<String> plugin_names = new ArrayList<String>();
        for (ResolveInfo ri : plugins) {
            plugin_names.add(ri.loadLabel(pm).toString());
        }
        String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);
        Log.d(TAG, "Load plugins " + plugin_names.toString());
        if(plugin_names.size() == 1) {
            Intent intent = new Intent();
            if (newChannel) {
                Log.d(TAG, "Try to start ");
                ResolveInfo plugin_info = plugins.get(0);
                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                        plugin_info.activityInfo.name);

                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                        plugin_info.activityInfo.name);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
            } else {
                ChannelDatabase cdn = new ChannelDatabase(activity.getApplicationContext());
                try {
                    JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(index));
                    ResolveInfo plugin_info = plugins.get(0);
                    Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                            plugin_info.activityInfo.name);
                    intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                            plugin_info.activityInfo.name);
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            activity.startActivity(intent);
        } else {
            new MaterialDialog.Builder(activity)
                    .items(plugin_names2)
                    .title("Choose an app")
                    .content("Yes, if there's only one app, default to that one")
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            Toast.makeText(activity, "Pick " + i, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            if (newChannel) {
                                Log.d(TAG, "Try to start ");
                                ResolveInfo plugin_info = plugins.get(i);
                                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                                        plugin_info.activityInfo.name);

                                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                        plugin_info.activityInfo.name);

                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
                            } else {
                                ChannelDatabase cdn = new ChannelDatabase(activity);
                                try {
                                    JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(index));
                                    ResolveInfo plugin_info = plugins.get(i);
                                    intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                            plugin_info.activityInfo.name);
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            activity.startActivity(intent);
                        }
                    }).show();
        }
    }
}
