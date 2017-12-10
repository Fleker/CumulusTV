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
        super.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1"); // Claim to be iPhone
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
