package com.felkertech.n.cumulustv.Intro;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;

import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.boilerplate.Utils.PermissionUtils;
import com.felkertech.n.cumulustv.R;
import com.github.paolorotolo.appintro.AppIntro2;

/**
 * Created by N on 7/1/2015.
 */
public class Intro extends AppIntro2 {
    @Override
    public void init(Bundle savedInstanceState) {
        addSlide(new FirstSlide());
        addSlide(new SecondSlide());
        addSlide(new SecondOneSlide());
        addSlide(new ThirdSlide());
        addSlide(new FourthSlide());
        setFadeAnimation();
    }

    @Override
    public void selectDot(int index) {
        super.selectDot(index);
    }

    private void loadMainActivity(){
        DriveSettingsManager sm = new DriveSettingsManager(this);
        sm.setInt(R.string.sm_last_version, ActivityUtils.LAST_GOOD_BUILD);
        Intent intent = new Intent(this, ActivityUtils.getMainActivity(this));
        startActivity(intent);
    }

    @Override
    public void onDonePressed() {
        loadMainActivity();
    }

    public void getStarted(View v){
        loadMainActivity();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent kvent) {
        if(code == KeyEvent.KEYCODE_DPAD_CENTER) {
            ViewPager vp = (ViewPager)this.findViewById(com.github.paolorotolo.appintro.R.id.view_pager);
            if(vp.getCurrentItem() == vp.getAdapter().getCount()-1) {
                onDonePressed();
            } else {
                vp.setCurrentItem(vp.getCurrentItem()+1);
            }
            return false;
        }
        return super.onKeyDown(code, kvent);
    }

    @Override
    public void onDotSelected(int index) {
        if(index == 3) {
            PermissionUtils.requestPermissionIfDisabled(this, Manifest.permission_group.STORAGE, getString(R.string.permission_storage_rationale));
        }
    }
}
