package com.felkertech.n.cumulustv.Intro;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.felkertech.n.cumulustv.MainActivity;
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
        addSlide(new ThirdSlide());
        addSlide(new FourthSlide());
        setFadeAnimation();
    }

    @Override
    public void selectDot(int index) {
        super.selectDot(index);
    }

    private void loadMainActivity(){
        SettingsManager sm = new SettingsManager(this);
        sm.setInt(R.string.sm_last_version, MainActivity.LAST_GOOD_BUILD);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDonePressed() {
        loadMainActivity();
    }

    public void getStarted(View v){
        loadMainActivity();
    }

    /*@Override
    public boolean onKeyDown(int code, KeyEvent kvent) {
        if(code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A) {
            ViewPager vp = (ViewPager)this.findViewById(com.github.paolorotolo.appintro.R.id.view_pager);
//            if(((ImageView) findViewById(com.github.paolorotolo.appintro.R.id.done)).getVisibility() == View.VISIBLE) {
            Log.d("weather:intro", vp.getCurrentItem()+" "+(vp.getAdapter().getCount()-1));
            if(vp.getCurrentItem() == vp.getAdapter().getCount()-1) {
                onDonePressed();
            } else {
                vp.setCurrentItem(vp.getCurrentItem()+1);
            }
            return false;
        }
        return super.onKeyDown(code, kvent);
    }*/
}
