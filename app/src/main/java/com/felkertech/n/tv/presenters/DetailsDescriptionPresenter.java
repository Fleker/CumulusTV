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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.felkertech.n.cumulustv.model.JsonChannel;

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
