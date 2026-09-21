package com.rtsoft.growtopia;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebViewManager {
    public static String originalURL = "";
    private Activity baseActivity;
    private final ExecutorService webViewWorkExecutor;
    boolean allowExternalLinks = true;
    private WebView webView = null;

    public boolean needed_to_render = false;
    public String to_render = "";
    public String last_packet = "";
    public String last_url = "";

    private interface WebViewCallbackListener {
        void OnError(int errorCode);
        void OnPageLoaded(String url);
    }

    native void nativeOnErrorOccurred(int i);
    native void nativeOnPageContent(String str);
    public native void nativeOnPageLoaded(String str);
    public native void nativeOnScriptCall(String str, String str2);

    public WebViewManager(Activity activity) {
        this.baseActivity = activity;
        this.webViewWorkExecutor = Executors.newSingleThreadExecutor();
    }

    public void destroy() { this.webViewWorkExecutor.shutdown(); }

    public boolean IsVisible() {
        WebView wv = this.webView;
        return wv != null && wv.getVisibility() == android.view.View.VISIBLE;
    }

    private void DestroyWebView() {
        if (this.webView == null) return;
        ViewGroup parent = (ViewGroup) this.webView.getParent();
        if (parent != null) parent.removeView(this.webView);
        this.webView.stopLoading();
        this.webView.loadUrl("about:blank");
        this.webView.removeJavascriptInterface("NativeApp");
        this.webView.destroy();
        this.webView = null;
    }

    public synchronized void ShowWebView() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) return;
        if (this.webView == null) {
            WebView wv = new WebView(this.baseActivity);
            this.webView = wv;
            wv.setWebViewClient(new WebViewClientImpl(this.baseActivity,
                    new WebViewCallbackListener() {
                        @Override public void OnError(int e) { nativeOnErrorOccurred(e); }
                        @Override public void OnPageLoaded(String url) { nativeOnPageLoaded(url); }
                    }));
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setSupportMultipleWindows(true);
            s.setJavaScriptCanOpenWindowsAutomatically(true);
            wv.setBackgroundColor(0);
            wv.addJavascriptInterface(new WebViewJavascriptInterface(this), "NativeApp");
            this.baseActivity.addContentView(wv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        this.webView.setVisibility(android.view.View.VISIBLE);
    }

    public void LoadURL(final String url, final boolean allowExternal) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            this.allowExternalLinks = allowExternal;
            ShowWebView();
            originalURL = url;
            this.webView.loadUrl(url);
        }));
    }

    public void LoadURLPost(final String url, final byte[] postData, final boolean allowExternal) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            this.allowExternalLinks = allowExternal;
            originalURL = url;
            this.last_url = url;
            if (postData != null) this.last_packet = new String(postData, StandardCharsets.ISO_8859_1);
            LoginSpoof spoof = getActiveSpoof();
            if (spoof != null) {
                String ltoken = spoof.getLtoken();
                if (ltoken != null && !ltoken.isEmpty()) {
                    nativeOnScriptCall("nativeSignIn", ltoken);
                    return;
                }
            }
            ShowWebView();
            this.webView.postUrl(url, postData);
        }));
    }

    /**
     * Posts to the OAuth dashboard without the saved-token short-circuit.
     * Used by ZennKuyBridge.startResolving() so Ubisoft always mints a fresh
     * session and returns a Google OAuth URL containing the required state= param.
     */
    public void postOAuthDashboard(final String url, final byte[] postData) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            this.allowExternalLinks = false;
            originalURL = url;
            this.last_url = url;
            if (postData != null) this.last_packet = new String(postData, StandardCharsets.ISO_8859_1);
            ShowWebView();
            this.webView.postUrl(url, postData);
        }));
    }

    private static LoginSpoof getActiveSpoof() {
        try {
            if (Main.mainApp == null) return null;
            LoginSpoof s = new LoginSpoof(Main.mainApp);
            return s.isEnabled() ? s : null;
        } catch (Exception e) { return null; }
    }

    public void SetFrame(final float x, final float y, final float w, final float h) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            if (this.webView == null) return;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) w, (int) h);
            lp.setMargins((int) x, (int) y, 0, 0);
            this.webView.setLayoutParams(lp);
        }));
    }

    public void SetBgColor(final int r, final int g, final int b, final int a) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            if (this.webView != null) this.webView.setBackgroundColor(Color.argb(r, g, b, a));
        }));
    }

    public void MoveView(int height) {
        if (this.webView == null) return;
        ObjectAnimator.ofFloat(this.webView, "translationY", (-height) / 2.0f).setDuration(200L).start();
    }

    public void HideWebView() {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            if (this.webView == null) return;
            this.webView.setVisibility(android.view.View.GONE);
            DestroyWebView();
        }));
    }

    public void requestPageSource() {
        if (this.webView == null) return;
        this.baseActivity.runOnUiThread(() -> {
            if (this.needed_to_render) {
                nativeOnPageContent(this.to_render);
                this.needed_to_render = false;
                this.to_render = "";
                return;
            }
            this.webView.loadUrl("javascript:NativeApp.pageContent(document.body.innerText)");
        });
    }

    public class WebViewJavascriptInterface {
        WebViewManager webviewManager;
        WebViewJavascriptInterface(WebViewManager wvm) { this.webviewManager = wvm; }

        @JavascriptInterface
        public void nativeSignIn(String str) {
            WebViewManager.this.baseActivity.runOnUiThread(() -> {
                Toast.makeText(WebViewManager.this.baseActivity,
                        "Logging in with google... wait a moment...", Toast.LENGTH_LONG).show();
                WebViewManager.this.HideWebView();
                if (str != null && !str.isEmpty() && !"undefined".equals(str) && !"null".equals(str)) {
                    webviewManager.nativeOnScriptCall("nativeSignIn", str);
                }
            });
        }

        @JavascriptInterface
        public void onloginselection(String token) {
            this.webviewManager.nativeOnScriptCall("onloginselection", token);
        }

        @JavascriptInterface
        public void onnameselection(String token) {
            this.webviewManager.nativeOnScriptCall("onnameselection", token);
        }

        @JavascriptInterface
        public void pageContent(String content) {
            this.webviewManager.nativeOnPageContent(content);
        }

        @JavascriptInterface
        public void openInBrowser(final String url) {
            WebViewManager.this.baseActivity.runOnUiThread(() -> {
                if (url == null) return;
                Toast.makeText(WebViewManager.this.baseActivity,
                        "Logging in with google... wait a moment...", Toast.LENGTH_LONG).show();
                WebViewManager.this.baseActivity.startActivityForResult(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)), 1);
            });
        }
    }

    private class WebViewClientImpl extends WebViewClient {
        private final Activity baseActivity;
        private final WebViewCallbackListener listener;
        WebViewClientImpl(Activity a, WebViewCallbackListener l) {
            this.baseActivity = a; this.listener = l;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
            return shouldOverrideUrlLoading(v, req.getUrl() == null ? "" : req.getUrl().toString());
        }

        @Override @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView v, String url) {
            try {
                Uri next = Uri.parse(url == null ? "" : url);
                String scheme = next.getScheme();

                // grow:// — Ubisoft's post-OAuth redirect carrying the login token.
                // Route it as an Intent so Main.onNewIntent can pick up the token.
                if ("grow".equalsIgnoreCase(scheme)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, next);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        this.baseActivity.startActivity(intent);
                    } catch (Exception ex) {
                        android.util.Log.e("WebViewManager", "grow:// route failed: " + ex.getMessage());
                    }
                    WebViewManager.this.HideWebView();
                    return true;
                }

                String nh = next.getHost();
                if (nh != null && nh.contains("accounts.google.com")) {
                    // Only open the Google OAuth URL if Ubisoft included state=.
                    // Without state= the /google/callback endpoint rejects the auth flow.
                    String stateParam = next.getQueryParameter("state");
                    if (stateParam == null || stateParam.isEmpty()) {
                        android.util.Log.w("WebViewManager",
                                "Google OAuth URL missing state= — not opening: " + url);
                        return true;
                    }
                    Toast.makeText(this.baseActivity, "Logging in with google... wait a moment...", Toast.LENGTH_LONG).show();
                    this.baseActivity.startActivityForResult(new Intent(Intent.ACTION_VIEW, next), 1);
                    return true;
                }
                Uri orig = Uri.parse(WebViewManager.originalURL == null ? "" : WebViewManager.originalURL);
                String oh = orig.getHost();
                if (!WebViewManager.this.allowExternalLinks || oh == null || nh == null || oh.equals(nh)) {
                    v.loadUrl(url);
                    return true;
                }
                this.baseActivity.startActivity(new Intent(Intent.ACTION_VIEW, next));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            this.listener.OnPageLoaded(url);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
            super.onReceivedError(v, req, err);
            this.listener.OnError(err.getErrorCode());
        }
    }
}
