package com.felkertech.cumulustv.tv.presenters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.n.cumulustv.R;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

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
        if (jsonChannel.getLogo() == null || jsonChannel.getLogo().isEmpty()) {
            cardView.setMainImage(mContext.getResources().getDrawable(R.drawable.c_banner_3_2));
            cardView.findViewById(R.id.info_field)
                    .setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap logo = Glide.with(mContext)
                                .load(ChannelDatabase.getNonNullChannelLogo(jsonChannel))
                                .asBitmap()
                                .error(R.drawable.c_banner_3_2)
                                .fitCenter()
                                .into(CARD_WIDTH, CARD_HEIGHT)
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
                                    } else if (colors.getDarkMutedSwatch() != null) {
                                        cardView.findViewById(R.id.info_field).setBackgroundColor(
                                                colors.getDarkMutedSwatch().getRgb());
                                    } else if (colors.getSwatches().size() > 0) {
                                        cardView.findViewById(R.id.info_field).setBackgroundColor(
                                                colors.getSwatches().get(0).getRgb());
//                                        ((TextView) cardView.findViewById(R.id.info_field))
//                                                .setTextColor(colors.getSwatches().get(0).getTitleTextColor());
                                    } else {
                                        cardView.findViewById(R.id.info_field).setBackgroundColor(
                                                ContextCompat.getColor(mContext,
                                                        R.color.colorPrimaryDark));
                                    }
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "There was a problem loading " + jsonChannel.getLogo());
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
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
