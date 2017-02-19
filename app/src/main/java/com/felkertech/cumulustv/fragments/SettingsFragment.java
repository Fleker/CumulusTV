package com.felkertech.cumulustv.fragments;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v17.preference.LeanbackSettingsFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.cumulustv.tv.CumulusTvTifService;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.BaseTvInputService;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nick on 1/9/2017.
 */
public class SettingsFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment {
    private final static String PREFERENCE_RESOURCE_ID = "preferenceResource";
    private final static String PREFERENCE_ROOT = "root";
    private PreferenceFragment mPreferenceFragment;

    @Override
    public void onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.preferences, null);
        startPreferenceFragment(mPreferenceFragment);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
                                             Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
                                           PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment(R.xml.preferences,
                preferenceScreen.getKey());
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return mPreferenceFragment.findPreference(charSequence);
    }

    private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
        PreferenceFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId);
        args.putString(PREFERENCE_ROOT, root);
        fragment.setArguments(args);
        return fragment;
    }

    public static class PrefFragment extends LeanbackPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            String root = getArguments().getString(PREFERENCE_ROOT, null);
            int prefResId = getArguments().getInt(PREFERENCE_RESOURCE_ID);
            if (root == null) {
                addPreferencesFromResource(prefResId);
                try {
                    findPreference("VERSION").setTitle("v" +
                            getActivity().getPackageManager().getPackageInfo(
                                    getActivity().getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException | NullPointerException e) {
                    e.printStackTrace();
                }
            } else {
                setPreferencesFromResource(prefResId, root);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("PRINT_DATABASE")) {
                new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.Theme_AppCompat_Dialog))
                        .setTitle(R.string.print_database)
                        .setMessage(ChannelDatabase.getInstance(getActivity()).toString())
                        .show();
                return true;
            } else if (preference.getKey().equals("PRINT_SYSTEM")) {
                List<Channel> tvChannels = null;
                try {
                    tvChannels = ChannelDatabase.getInstance(getActivity()).getChannels();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateChannels(getActivity(), "com.felkertech.n.cumulustv/com.felkertech.cumulustv.tv.CumulusTvTifService", tvChannels);

                Cursor cursor = getActivity().getContentResolver().query(
                        TvContract.buildChannelsUriForInput("com.felkertech.n.cumulustv/com.felkertech.cumulustv.tv.CumulusTvTifService"), null, null, null, null);
                Log.d("S", "Query from " + TvContract.buildChannelsUriForInput("com.felkertech.n.cumulustv/com.felkertech.cumulustv.tv.CumulusTvTifService"));
                StringBuilder builder = new StringBuilder();
                if (cursor == null) {
                    builder.append("null");
                } else {
                    cursor.moveToFirst();
                }
                while (cursor != null) {
                    try {
                        builder.append(cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)) + "\n");
                        cursor.moveToNext();
                    } catch (CursorIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.Theme_AppCompat_Dialog))
                        .setTitle(R.string.print_database)
                        .setMessage(builder.toString())
                        .show();
                return true;
            } else if (preference.getKey().equals("HELP")) {
                ActivityUtils.launchWebsite(getActivity());
            }
            return super.onPreferenceTreeClick(preference);
        }
    }

    public static void updateChannels(Context context, String inputId, List<Channel> channels) {
        // Create a map from original network ID to channel row ID for existing channels.
        SparseArray<Long> channelMap = new SparseArray<>();
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        Log.d("S", channelsUri.toString());
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID};
        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(channelsUri, projection, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                int originalNetworkId = cursor.getInt(1);
                channelMap.put(originalNetworkId, rowId);
                Log.d("S", "Got " + rowId + ", " + originalNetworkId);
            }
        }

        // If a channel exists, update it. If not, insert a new one.
        Map<Uri, String> logos = new HashMap<>();
        for (Channel channel : channels) {
            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_INPUT_ID, inputId);
            values.putAll(channel.toContentValues());
            // If some required fields are not populated, the app may crash, so defaults are used
            if (channel.getPackageName() == null) {
                // If channel does not include package name, it will be added
                values.put(TvContract.Channels.COLUMN_PACKAGE_NAME, context.getPackageName());
                Log.d("S", "Put " + context.getPackageName());
            }
            if (channel.getInputId() == null) {
                // If channel does not include input id, it will be added
                values.put(TvContract.Channels.COLUMN_INPUT_ID, inputId);
                Log.d("S", "Put II " + inputId);
            }
            if (channel.getType() == null) {
                // If channel does not include type it will be added
                values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER);
            }

            Long rowId = channelMap.get(channel.getOriginalNetworkId());
            Uri uri;
            if (rowId == null) {
                uri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
                    Log.d("S", "Adding channel " + channel.getDisplayName() + " at " + uri);
            } else {
                values.put(TvContract.Channels._ID, rowId);
                uri = TvContract.buildChannelUri(rowId);
                    Log.d("S", "Updating channel " + channel.getDisplayName() + " at " + uri);
                resolver.update(uri, values, null, null);
                channelMap.remove(channel.getOriginalNetworkId());
            }
            if (channel.getChannelLogo() != null && !TextUtils.isEmpty(channel.getChannelLogo())) {
                logos.put(TvContract.buildChannelLogoUri(uri), channel.getChannelLogo());
            }
        }
        // Deletes channels which don't exist in the new feed.
        int size = channelMap.size();
        for (int i = 0; i < size; ++i) {
            Long rowId = channelMap.valueAt(i);
                Log.d("S", "Deleting channel " + rowId);
            resolver.delete(TvContract.buildChannelUri(rowId), null, null);
            SharedPreferences.Editor editor = context.getSharedPreferences(
                    BaseTvInputService.PREFERENCES_FILE_KEY, Context.MODE_PRIVATE).edit();
            editor.remove(BaseTvInputService.SHARED_PREFERENCES_KEY_LAST_CHANNEL_AD_PLAY + rowId);
            editor.apply();
        }
    }
}
