package com.rtsoft.growtopia;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import java.net.URLEncoder;

public class CSTSWebViewActivity extends Activity implements CSTSWebViewClient.CSTSWebViewClientCallback {
    private static final String TAG = "cstslog";
    private String _initialURL;
    private CSTSWebView _webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1); // No title bar
        FrameLayout frame = new FrameLayout(this);
        CSTSWebView webView = new CSTSWebView(this);
        this._webView = webView;
        webView.getWebClient().setCSTSWebViewActivityCallback(this);
        frame.addView(webView);
        setContentView(frame);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            String cstsuid    = intent.getStringExtra("cstsuid");
            String country    = intent.getStringExtra("country");
            String language   = intent.getStringExtra("language");
            boolean payer     = intent.getBooleanExtra("payer", false);
            String playerId   = intent.getStringExtra("ingameplayerid");
            String env        = intent.getStringExtra("environment");
            String misc       = intent.getStringExtra("misc");

            String base = (env != null && env.equals("PROD"))
                    ? "https://csts-mob.ubi.com/index.php"
                    : "https://dev-csts-mob.ubi.com/index.php";

            StringBuilder sb = new StringBuilder(base);
            sb.append("?cstsuid=").append(cstsuid != null ? cstsuid : "");
            sb.append("&platform=android");
            sb.append("&language=").append(language != null ? language : "en");
            sb.append("&country=").append(country != null ? country : "US");
            sb.append("&iap=").append(payer);
            sb.append("&igpid=").append(playerId != null ? playerId : "");
            sb.append("&device=").append(urlencode(getDeviceInfos()));
            sb.append("&dnaid=").append(playerId != null ? playerId : "");

            if (misc != null && !misc.isEmpty()) {
                sb.append("&misc=").append(urlencode(misc));
            }

            String url = sb.toString();
            Log.v(TAG, "connecting to CSTS: " + url);
            this._initialURL = url;
            this._webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (this._webView != null && this._webView.canGoBack()) {
            this._webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCSExit() {
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        onCSExit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this._webView != null) this._webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (this._webView != null) this._webView.restoreState(savedInstanceState);
    }

    public String getDeviceInfos() {
        return "android version:" + System.getProperty("os.version")
                + "(" + Build.VERSION.INCREMENTAL + ")"
                + ";android API Level:" + Build.VERSION.SDK_INT
                + ";device:" + Build.DEVICE
                + ";model:" + Build.MODEL;
    }

    public String urlencode(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (Exception e) {
            Log.e(TAG, "CSTS_urlencode: " + e.getMessage());
            return str;
        }
    }
}
