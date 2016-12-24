package com.felkertech.cumulustv.player;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by guest1 on 12/23/2016.
 */

public class CumulusWebPlayer extends WebView {
    private WebViewListener mListener;

    public CumulusWebPlayer(Context context, WebViewListener listener) {
        super(context);
        super.getSettings().setJavaScriptEnabled(true);
        super.getSettings().setSupportZoom(false);
        super.getSettings().setSupportMultipleWindows(false);
        super.setWebViewClient(new BridgeClient());
        super.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.35 Safari/537.36"); //Claim to be a desktop
        super.setKeepScreenOn(true);
        this.mListener = listener;
    }

    public void load(String url) {
        super.loadUrl(url);
    }

    public class BridgeClient extends WebViewClient {
        public void onPageFinished(WebView v, String url) {
            super.onPageFinished(v, url);
            Handler h = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    mListener.onPageFinished();
                }
            };
            h.sendEmptyMessageDelayed(0, 1000);
        }
    }

    public interface WebViewListener {
        void onPageFinished();
    }
}
