package com.felkertech.cumulustv.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by guest1 on 9/11/2015.
 */
public class AppUtils {
    public static boolean isTV(Context context) {
        UiModeManager uiModeManager =
                (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}
