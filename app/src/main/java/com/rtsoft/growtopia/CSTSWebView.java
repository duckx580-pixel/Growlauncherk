package com.rtsoft.growtopia;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

class CSTSWebView extends WebView {
    private CSTSWebViewClient _webClient;

    public CSTSWebView(Context context) {
        super(context);
        this._webClient = null;
        setupWebView();
    }

    public CSTSWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this._webClient = null;
        setupWebView();
    }

    public CSTSWebView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this._webClient = null;
        setupWebView();
    }

    private void setupWebView() {
        if (this._webClient == null) {
            CSTSWebViewClient client = new CSTSWebViewClient();
            this._webClient = client;
            setWebViewClient(client);
            getSettings().setJavaScriptEnabled(true);
            getSettings().setDomStorageEnabled(true);
            clearCache(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }
    }

    public CSTSWebViewClient getWebClient() {
        return this._webClient;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    public boolean shouldGoBackToFirstURL() {
        if (getUrl() != null && getUrl().contains("facebook")) {
            return true;
        }
        return this._webClient != null && this._webClient.isInCreateAccount();
    }
}
