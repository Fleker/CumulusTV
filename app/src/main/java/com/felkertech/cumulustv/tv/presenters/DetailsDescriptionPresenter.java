package com.felkertech.cumulustv.tv.presenters;

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.felkertech.cumulustv.model.JsonChannel;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        JsonChannel jsonChannel = (JsonChannel) item;

        if (jsonChannel != null) {
            viewHolder.getTitle().setText(jsonChannel.getName());
            viewHolder.getSubtitle().setText(jsonChannel.getNumber());
            viewHolder.getBody().setText(getGenresPretty(jsonChannel) +
                    "\n\n" + jsonChannel.getMediaUrl());
        }
    }

    private String getGenresPretty(JsonChannel jsonChannel) {
        String genresString = jsonChannel.getGenresString();
        if (genresString == null) {
            return "";
        }
        genresString = genresString.replaceAll("_", " / ");
        genresString = genresString.toLowerCase();
        genresString = genresString.replaceAll(",", ", ");
        for (int i = 0; i < genresString.length(); i++) {
            if (i == 0 || genresString.charAt(i - 1) == ' ') {
                genresString = genresString.substring(0, i) +
                        genresString.substring(i, i + 1).toUpperCase() +
                        genresString.substring(i + 1);
            }
        }
        return genresString;
    }
}
