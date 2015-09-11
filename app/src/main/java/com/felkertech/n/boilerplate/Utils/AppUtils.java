package com.felkertech.n.boilerplate.Utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by guest1 on 9/11/2015.
 */
public class AppUtils {
    public static boolean isTV(Context mContext) {
        UiModeManager uiModeManager = (UiModeManager) mContext.getSystemService(mContext.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}
