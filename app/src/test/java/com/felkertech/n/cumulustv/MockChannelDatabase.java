package com.felkertech.n.cumulustv;

import android.content.Context;

import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.settingsmanager.SettingsManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides a test infrastructure for a {@link ChannelDatabase}
 * @author Nick
 */
public class MockChannelDatabase extends ChannelDatabase {
    public static final String KEY = MockChannelDatabase.class.getCanonicalName();

    private static MockChannelDatabase mMockChannelDatabase;

    protected JSONObject mJsonObject;

    public static MockChannelDatabase getMockedInstance(Context context) throws JSONException {
        if (mMockChannelDatabase == null) {
            mMockChannelDatabase = new MockChannelDatabase(context);
        }
        return mMockChannelDatabase;
    }

    public static void reset() {
        mMockChannelDatabase = null;
    }

    protected MockChannelDatabase(final Context context) throws JSONException {
        super(context);
        SettingsManager settingsManager = new SettingsManager(context);
        String jsonString = settingsManager.getString(KEY, getDefaultJsonString());
        if (jsonString.isEmpty()) {
            jsonString = getDefaultJsonString();
        }
        mJsonObject = new JSONObject(jsonString);
    }

    @Override
    public void save() {
    }
}
