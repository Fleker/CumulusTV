package com.felkertech.n.cumulustv.activities;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.CumulusDreams;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JSONChannel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A settings Activity for {@link CumulusDreams}.
 * <p/>
 * A DreamService can only be used on devices with API v17+, so it is safe
 * for us to use a {@link PreferenceFragment} here.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CumulusDreamsSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new DreamPreferenceFragment()).commit();
    }

    public static class DreamPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.cumulus_dreams_prefs);
            final ChannelDatabase cdn = new ChannelDatabase(getActivity());
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.daydream_select_channel)
                    .items(cdn.getChannelNames())
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            DriveSettingsManager sm = new DriveSettingsManager(getActivity());
                            try {
                                sm.setString(R.string.daydream_url, new JSONChannel((JSONObject) cdn.getJSONChannels().get(which)).getUrl());
                                Toast.makeText(getActivity(), R.string.daydream_success, Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            getActivity().finish();
                        }
                    })
                    .show();
        }

    }

}
