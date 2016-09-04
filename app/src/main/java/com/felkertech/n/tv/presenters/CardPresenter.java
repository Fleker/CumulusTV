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

package com.felkertech.n.tv.presenters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.squareup.picasso.Picasso;

import java.io.IOException;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand. 
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    protected static int CARD_WIDTH = 313;
    protected static int CARD_HEIGHT = 176;
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;
    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        sDefaultBackgroundColor = parent.getResources().getColor(R.color.default_background);
        sSelectedBackgroundColor = parent.getResources().getColor(R.color.selected_background);

        ImageCardView cardView = new ImageCardView(mContext);

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final CumulusChannel jsonChannel = (CumulusChannel) item;
        final ImageCardView cardView = (ImageCardView) viewHolder.view;

        cardView.setTitleText(jsonChannel.getName());
        cardView.setContentText(jsonChannel.getNumber());
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        if (jsonChannel.getLogo() == null) {
            cardView.setMainImage(mContext.getResources().getDrawable(R.drawable.c_banner_3_2));
            cardView.findViewById(R.id.info_field)
                    .setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap logo = Picasso.with(mContext).load(jsonChannel.getLogo())
                                .error(R.drawable.c_banner_3_2)
                                .centerInside()
                                .resize(CARD_WIDTH, CARD_HEIGHT)
                                .get();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    cardView.getMainImageView().setImageBitmap(logo);
                                    Palette colors = Palette.from(logo).generate();
                                    if (colors.getDarkVibrantSwatch() != null) {
                                        cardView.findViewById(R.id.info_field).setBackgroundColor(
                                                colors.getDarkVibrantSwatch().getRgb());
                                    } else {
                                        cardView.findViewById(R.id.info_field).setBackgroundColor(
                                                colors.getSwatches().get(0).getRgb());
                                    }
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "There was a problem loading " + jsonChannel.getLogo());
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }

    static class ViewHolder extends Presenter.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }
}
