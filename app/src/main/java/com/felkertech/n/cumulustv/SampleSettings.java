package com.felkertech.n.cumulustv;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by N on 7/12/2015.
 */
@Deprecated
public class SampleSettings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Settings complete", Toast.LENGTH_SHORT).show();

        Fabric.with(this, new Crashlytics());
        finish();
    }
}
