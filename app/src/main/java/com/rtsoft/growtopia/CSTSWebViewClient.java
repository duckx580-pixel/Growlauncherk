package com.rtsoft.growtopia;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class CSTSWebViewClient extends WebViewClient {
    private static final String TAG = "cstslog";
    private CSTSWebViewClientCallback _callback;
    private boolean _isInCreateAccount;

    public interface CSTSWebViewClientCallback {
        void onCSExit();
    }

    public boolean isInCreateAccount() {
        return this._isInCreateAccount;
    }

    public void setCSTSWebViewActivityCallback(CSTSWebViewClientCallback callback) {
        this._callback = callback;
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (url.contains("Default/CreateAccount?appId")) {
            this._isInCreateAccount = true;
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.v(TAG, "adding javascript callback");
        view.loadUrl("javascript:function csts_onTicketCreationResult(wasTicketCreated, message) "
                + "{window.location.href = 'ticket://'+(wasTicketCreated?1:0)+'/'+message; };");
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.v(TAG, "shouldOverrideUrlLoading [" + url + "]");

        if (url.equals("exit://")) {
            if (_callback != null) _callback.onCSExit();
            return true;
        }

        if (url.contains("legal.ubi.com")) {
            try {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Log.e(TAG, "Cannot open legal URL: " + e.getMessage());
            }
            return true;
        }

        if (url.startsWith("grow://")) {
            // Google OAuth callback from CSTS web login — route as an Intent so
            // Main.onNewIntent / Main.handleIntent can pick up info= and token=.
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                view.getContext().startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Cannot route grow:// intent: " + e.getMessage());
            }
            if (_callback != null) _callback.onCSExit();
            return true;
        }

        if (url.startsWith("ticket://")) {
            Log.v(TAG, "Ticket detected");
            boolean success = url.length() > 9 && url.charAt(9) == '1';
            String detail = url.length() > 11 ? url.substring(11) : "";
            Log.v(TAG, "Ticket creation status: " + success + " detail: " + detail);
            if (_callback != null) _callback.onCSExit();
            return true;
        }

        this._isInCreateAccount = false;
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        Log.v(TAG, "onReceivedSslError : " + error.toString());
        Log.v(TAG, "the URL : " + error.getUrl());
        Log.v(TAG, "CANCEL");
        handler.cancel();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.e("csts", "onReceivedError [" + description + "] : " + failingUrl);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        Log.e("csts", "onReceivedError [" + error.getDescription() + "] : " + request.getUrl());
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
        Log.e("csts", "onReceivedHttpError [" + response.getStatusCode() + "] : " + request.getUrl());
    }
}
