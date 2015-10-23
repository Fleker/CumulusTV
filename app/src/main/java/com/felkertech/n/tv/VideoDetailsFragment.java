/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.felkertech.n.tv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.felkertech.n.cumulustv.ChannelDatabase;
import com.felkertech.n.cumulustv.JSONChannel;
import com.felkertech.n.cumulustv.MainActivity;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.SamplePlayer;
import com.felkertech.n.plugins.CumulusTvPlugin;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class VideoDetailsFragment extends DetailsFragment {
    private static final String TAG = "VideoDetailsFragment";

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;
    private static final int ACTION_EDIT = 5;
    private static final int ACTION_WATCH = 6;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private Movie mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        mSelectedMovie = (Movie) getActivity().getIntent()
                .getSerializableExtra(DetailsActivity.MOVIE);
        if (mSelectedMovie != null) {
            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setupMovieListRow();
            setupMovieListRowPresenter();
            updateBackground(mSelectedMovie.getBackgroundImageUrl());
            setOnItemViewClickedListener(new ItemViewClickedListener());
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void updateBackground(String uri) {
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable> glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
    }

    private void setupAdapter() {
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
        row.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
        int width = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_HEIGHT);
        Glide.with(getActivity())
                .load(mSelectedMovie.getCardImageUrl())
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        Log.d(TAG, "details overview card image url ready: " + resource);
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });

//        row.addAction(new Action(ACTION_WATCH_TRAILER, getResources().getString(
//                R.string.watch_trailer_1), getResources().getString(R.string.watch_trailer_2)));
//        row.addAction(new Action(ACTION_RENT, getResources().getString(R.string.rent_1),
//                getResources().getString(R.string.rent_2)));
//        row.addAction(new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
//                getResources().getString(R.string.buy_2)));
        ArrayObjectAdapter actions = new ArrayObjectAdapter();
        actions.add(new Action(ACTION_EDIT, "Edit Channel"));
        actions.add(new Action(ACTION_WATCH, "Play"));
        row.setActionsAdapter(actions);

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        DetailsOverviewRowPresenter detailsPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(getResources().getColor(R.color.selected_background));
        detailsPresenter.setStyleLarge(true);

        // Hook up transition element.
        detailsPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if(action.getId() == ACTION_EDIT) {
                    ChannelDatabase cdn = new ChannelDatabase(getActivity());
                    JSONChannel jsonChannel = cdn.findChannel(mSelectedMovie.getStudio()); //Find by number
                    if(jsonChannel.hasSource()) {
                        //Search through all plugins for one of a given source
                        PackageManager pm = getActivity().getPackageManager();
                        final String pack = jsonChannel.getSource().split(",")[0];
                        boolean app_installed = false;
                        try {
                            pm.getPackageInfo(pack, PackageManager.GET_ACTIVITIES);
                            app_installed = true;
                            //Open up this particular activity
                            Intent intent = new Intent();
                            intent.setClassName(pack,
                                    jsonChannel.getSource().split(",")[1]);
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                            intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                            startActivity(intent);
                        }
                        catch (PackageManager.NameNotFoundException e) {
                            app_installed = false;
                            new MaterialDialog.Builder(getActivity())
                                    .title("Plugin " + pack + " not installed")
                                    .content("What do you want to do instead?")
                                    .positiveText("Download app")
                                    .negativeText("Open in another plugin")
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            super.onPositive(dialog);
                                            Intent i = new Intent(Intent.ACTION_VIEW);
                                            i.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + pack));
                                            startActivity(i);
                                        }

                                        @Override
                                        public void onNegative(MaterialDialog dialog) {
                                            super.onNegative(dialog);
                                            openPluginPicker(false, mSelectedMovie.getStudio());
                                        }
                                    }).show();
                            Toast.makeText(getActivity(), "Plugin "+pack+" not installed.", Toast.LENGTH_SHORT).show();
                            openPluginPicker(false, mSelectedMovie.getStudio());
                        }
                    } else {
                        Log.d(TAG, "No specified source");
                        openPluginPicker(false, mSelectedMovie.getStudio());
                    }
                } else if(action.getId() == ACTION_WATCH) {
                    Intent i = new Intent(getActivity(), SamplePlayer.class);
                    i.putExtra(SamplePlayer.KEY_VIDEO_URL, mSelectedMovie.getVideoUrl());
                    startActivity(i);
                }
/*
                if (action.getId() == ACTION_WATCH_TRAILER) {
                    Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                    intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }
*/
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private void setupMovieListRow() {
        /*String subcategories[] = {getString(R.string.related_movies)};
        List<Movie> list = MovieList.list;

        Collections.shuffle(list);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (int j = 0; j < NUM_COLS; j++) {
            listRowAdapter.add(list.get(j % 5));
        }

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, listRowAdapter));*/
    }

    private void setupMovieListRowPresenter() {
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);
                intent.putExtra(getResources().getString(R.string.should_start), true);
                startActivity(intent);


                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }
    /* PLUGIN INTERFACES */
    public void openPluginPicker(final boolean newChannel, final String channelNo) {
        final PackageManager pm = getActivity().getPackageManager();
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
                ChannelDatabase cdn = new ChannelDatabase(getActivity());
                JSONChannel jsonChannel = cdn.findChannel(channelNo);
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
            }
            startActivity(intent);
        } else {
            new MaterialDialog.Builder(getActivity())
                    .items(plugin_names2)
                    .title("Choose an app")
                    .content("Yes, if there's only one app, default to that one")
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            Toast.makeText(getActivity(), "Pick " + i, Toast.LENGTH_SHORT).show();
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
                                ChannelDatabase cdn = new ChannelDatabase(getActivity());
                                JSONChannel jsonChannel = cdn.findChannel(channelNo);
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
                            }
                            startActivity(intent);
                        }
                    }).show();
        }
    }
}
