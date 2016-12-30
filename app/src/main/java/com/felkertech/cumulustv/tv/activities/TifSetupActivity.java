package com.felkertech.cumulustv.tv.activities;

import android.app.Activity;
import android.os.Bundle;

import com.felkertech.n.cumulustv.R;

/**
 * Created by guest1 on 12/23/2016.
 */
public class TifSetupActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        getActionBar().hide();
    }
}