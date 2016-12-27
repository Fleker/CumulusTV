package com.felkertech.cumulustv.tv.presenters;

import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.model.Option;

/**
 * A presenter which can be used to show options with an optional title along the bottom.
 *
 * @author Nick
 * @version 2016.09.04
 */
public class OptionsCardPresenter extends CardPresenter {
    private static final boolean DEFAULT_BANNER = false;

    private ContextThemeWrapper contextThemeWrapper;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        if (contextThemeWrapper == null) {
            contextThemeWrapper = new ContextThemeWrapper(parent.getContext(),
                    R.style.OptionsImageCardViewStyle);
        }
        ImageCardView cardView = new ImageCardView(contextThemeWrapper);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(parent.getResources().getColor(R.color.colorPrimary));
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final Option option = (Option) item;
        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        if (DEFAULT_BANNER) {
            cardView.setMainImage(contextThemeWrapper.getDrawable(R.drawable.c_banner_3_2));
        } else {
            cardView.setMainImage(option.getDrawable());
        }
        cardView.setTitleText(option.getText());
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        cardView.getMainImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
        cardView.findViewById(R.id.info_field).setBackgroundColor(
                ContextCompat.getColor(contextThemeWrapper, R.color.colorPrimaryDark));
    }
}
