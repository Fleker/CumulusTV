package com.felkertech.cumulustv.tv.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.n.cumulustv.R;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.media.tv.companionlibrary.ChannelSetupFragment;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;

/**
 * Fragment which shows a sample UI for registering channels and setting up SampleJobService to
 * provide program information in the background.
 */
public class TifSetupFragment extends ChannelSetupFragment {
    public static final long FULL_SYNC_FREQUENCY_MILLIS = 1000 * 60 * 60 * 24;  // 24 hour
    private static final long FULL_SYNC_WINDOW_SEC = 1000 * 60 * 60 * 24 * 14;  // 2 weeks

    private String mInputId = null;
    private boolean mErrorFound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = super.onCreateView(inflater, container, savedInstanceState);
        setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.detail_background));
        setBadge(getResources().getDrawable(R.mipmap.ic_launcher));
        setChannelListVisibility(true);
        setTitle("Cumulus TV");
        setDescription("");
        setButtonText("Sync channels");
        return fragmentView;
    }

    @Override
    public void onScanStarted() {
        EpgSyncJobService.cancelAllSyncRequests(getActivity());
        EpgSyncJobService.requestImmediateSync(getActivity(), mInputId,
                new ComponentName(getActivity(), CumulusJobService.class));

        new SettingsManager(getActivity())
                .setString(EpgSyncJobService.BUNDLE_KEY_INPUT_ID, mInputId);

        setButtonText("In Progress");
    }

    @Override
    public String getInputId() {
        return mInputId;
    }

    @Override
    public void onScanFinished() {
        if (!mErrorFound) {
            EpgSyncJobService.cancelAllSyncRequests(getActivity());
            EpgSyncJobService.setUpPeriodicSync(getActivity(), mInputId,
                    new ComponentName(getActivity(), CumulusJobService.class),
                    FULL_SYNC_FREQUENCY_MILLIS, FULL_SYNC_WINDOW_SEC);
            getActivity().setResult(Activity.RESULT_OK);
        } else {
            getActivity().setResult(Activity.RESULT_CANCELED);
        }
        getActivity().finish();
    }

    @Override
    public void onScanError(int reason) {
        mErrorFound = true;
        switch (reason) {
            case EpgSyncJobService.ERROR_EPG_SYNC_CANCELED:
                setDescription("Sync canceled");
                break;
            case EpgSyncJobService.ERROR_NO_PROGRAMS:
            case EpgSyncJobService.ERROR_NO_CHANNELS:
                setDescription("No data found");
                break;
            default:
                setDescription("Scan error " + reason);
                break;
        }
    }
}
