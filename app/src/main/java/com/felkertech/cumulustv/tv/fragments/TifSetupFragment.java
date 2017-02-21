package com.felkertech.cumulustv.tv.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
    private int mErrorFound = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = super.onCreateView(inflater, container, savedInstanceState);
        setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        setBadge(getResources().getDrawable(R.mipmap.ic_launcher));
        setChannelListVisibility(true);
        setTitle(getString(R.string.app_name));
        setDescription("");
        setButtonText(getString(R.string.sync_channels));
        return fragmentView;
    }

    @Override
    public void onScanStarted() {
        EpgSyncJobService.cancelAllSyncRequests(getActivity());
        CumulusJobService.requestImmediateSync1(getActivity(), mInputId, CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
                new ComponentName(getActivity(), CumulusJobService.class));

        new SettingsManager(getActivity())
                .setString(EpgSyncJobService.BUNDLE_KEY_INPUT_ID, mInputId);

        setButtonText(getString(R.string.in_progress));
    }

    @Override
    public String getInputId() {
        return mInputId;
    }

    @Override
    public void onScanFinished() {
        if (mErrorFound == -1) {
            EpgSyncJobService.cancelAllSyncRequests(getActivity());
            EpgSyncJobService.setUpPeriodicSync(getActivity(), mInputId,
                    new ComponentName(getActivity(), CumulusJobService.class),
                    FULL_SYNC_FREQUENCY_MILLIS, FULL_SYNC_WINDOW_SEC);
            getActivity().setResult(Activity.RESULT_OK);
            Toast.makeText(getActivity(), R.string.toast_scan_channels_added, Toast.LENGTH_SHORT).show();
        } else {
            getActivity().setResult(Activity.RESULT_CANCELED);
            Toast.makeText(getActivity(), R.string.toast_scan_error_found + mErrorFound, Toast.LENGTH_SHORT).show();
        }
        getActivity().finish();
    }

    @Override
    public void onScanError(int reason) {
        mErrorFound = reason;
        switch (reason) {
            case EpgSyncJobService.ERROR_EPG_SYNC_CANCELED:
                setDescription(getString(R.string.error_sync_canceled));
                break;
            case EpgSyncJobService.ERROR_NO_PROGRAMS:
            case EpgSyncJobService.ERROR_NO_CHANNELS:
                setDescription(getString(R.string.error_sync_no_data));
                break;
            default:
                setDescription(getString(R.string.error_sync_default, reason));
                break;
        }
    }
}
