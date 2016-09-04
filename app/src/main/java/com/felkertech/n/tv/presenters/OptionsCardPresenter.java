package com.felkertech.n.tv.presenters;

import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.cumulustv.model.Option;

/**
 * A presenter which can be used to show options with an optional title along the bottom.
 *
 * @author Nick
 * @version 2016.09.04
 */
public class OptionsCardPresenter extends CardPresenter {
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
        cardView.setBackgroundColor(parent.getResources().getColor(R.color.colorPrimaryDark));
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final Option option = (Option) item;
        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setMainImage(option.getDrawable());
        cardView.setTitleText(option.getText());
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        cardView.getMainImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
    }
}
