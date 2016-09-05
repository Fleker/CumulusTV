package com.felkertech.n.cumulustv.model;

import android.graphics.drawable.Drawable;

/**
 * Created by Nick on 9/4/2016.
 */

public class Option {
    private final Drawable drawable;
    private final String text;

    public Option(Drawable drawable, String text) {
        this.drawable = drawable;
        this.text = text;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public String getText() {
        return text;
    }
}
