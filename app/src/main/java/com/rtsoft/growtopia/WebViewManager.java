package com.rtsoft.growtopia;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebViewManager {
    public static String originalURL = "";

    // URL and binary payload the engine last passed to LoadURLPost.
    // Cached before any ltoken short-circuit so ZennKuyBridge.startResolving() can replay them.
    public static volatile String sLastLoginUrl  = "";
    public static volatile byte[] sLastPostData  = null;

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
            wv.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    String msg = cm.message() + " -- From line "
                            + cm.lineNumber() + " of " + cm.sourceId();
                    switch (cm.messageLevel()) {
                        case ERROR:   AppLogger.error("WebJS", msg); break;
                        case WARNING: AppLogger.warn("WebJS",  msg); break;
                        default:      AppLogger.log("WebJS",   msg); break;
                    }
                    return true;
                }

                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                    AppLogger.log("WVM", "onCreateWindow: isDialog=" + isDialog
                            + " isUserGesture=" + isUserGesture);
                    WebView probe = new WebView(view.getContext());
                    probe.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                            return shouldOverrideUrlLoading(v,
                                    req.getUrl() == null ? "" : req.getUrl().toString());
                        }
                        @Override @SuppressWarnings("deprecation")
                        public boolean shouldOverrideUrlLoading(WebView v, String url) {
                            if (url != null && !url.isEmpty()) {
                                AppLogger.log("WVM", "onCreateWindow→override: " + url);
                                baseActivity.runOnUiThread(() -> {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    baseActivity.startActivity(i);
                                });
                            }
                            return true;
                        }
                        @Override
                        public void onPageStarted(WebView v, String url, Bitmap favicon) {
                            if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
                                AppLogger.log("WVM", "onCreateWindow→pageStarted: " + url);
                                baseActivity.runOnUiThread(() -> {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    baseActivity.startActivity(i);
                                });
                            }
                        }
                    });
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(probe);
                    resultMsg.sendToTarget();
                    return true;
                }
            });
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setSupportMultipleWindows(true);
            s.setJavaScriptCanOpenWindowsAutomatically(true);
            wv.setBackgroundColor(0);
            wv.addJavascriptInterface(new WebViewJavascriptInterface(this), "NativeApp");
            wv.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AppLogger.log("WVM", "WebView touch DOWN x=" + (int)event.getX()
                            + " y=" + (int)event.getY());
                }
                return false; // don't consume — let WebView handle it
            });
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
            AppLogger.log("WVM", "LoadURLPost: " + url
                    + " bytes=" + (postData == null ? 0 : postData.length));
            this.allowExternalLinks = allowExternal;
            originalURL = url;
            this.last_url = url;
            if (url != null && !url.isEmpty()) {
                sLastLoginUrl = url;
            }
            if (postData != null && postData.length > 0) {
                sLastPostData = postData;
                this.last_packet = new String(postData, StandardCharsets.ISO_8859_1);
            }
            LoginSpoof spoof = getActiveSpoof();
            if (spoof != null) {
                String ltoken = spoof.getLtoken();
                if (ltoken != null && !ltoken.isEmpty()) {
                    AppLogger.log("WVM", "LoadURLPost: ltoken shortcut active, skipping WebView");
                    nativeOnScriptCall("nativeSignIn", ltoken);
                    return;
                }
            }
            ShowWebView();
            this.webView.postUrl(url, postData);
        }));
    }

    /** Replays the engine's last login POST in the WebView so the login page loads,
     *  then JS injection routes "Continue with Google" to Chrome. */
    public void postOAuthDashboard(final String url, final byte[] postData) {
        this.webViewWorkExecutor.execute(() -> this.baseActivity.runOnUiThread(() -> {
            AppLogger.log("WVM", "postOAuthDashboard: " + url
                    + " bytes=" + (postData == null ? 0 : postData.length));
            Toast.makeText(this.baseActivity, "Connecting to Growtopia login…",
                    Toast.LENGTH_SHORT).show();
            this.allowExternalLinks = true;
            originalURL = url;
            ShowWebView();
            if (postData != null && postData.length > 0) {
                this.webView.postUrl(url, postData);
            } else {
                this.webView.loadUrl(url);
            }
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
            AppLogger.log("WVM", "openInBrowser called: " + url);
            WebViewManager.this.baseActivity.runOnUiThread(() -> {
                if (url == null || url.isEmpty()) {
                    AppLogger.warn("WVM", "openInBrowser: URL null/empty, ignoring");
                    return;
                }
                AppLogger.log("WVM", "openInBrowser → Chrome: " + url);
                Toast.makeText(WebViewManager.this.baseActivity,
                        "Launching Chrome for Google login…", Toast.LENGTH_SHORT).show();
                WebViewManager.this.HideWebView();
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                WebViewManager.this.baseActivity.startActivity(i);
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
            String url = req.getUrl() == null ? "" : req.getUrl().toString();
            AppLogger.log("WVM", "override(req): " + url);
            return shouldOverrideUrlLoading(v, url);
        }

        @Override @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView v, String url) {
            AppLogger.log("WVM", "override: " + url);
            try {
                Uri next = Uri.parse(url == null ? "" : url);
                String scheme = next.getScheme();

                // grow:// — Ubisoft's post-OAuth redirect carrying the login token.
                if ("grow".equalsIgnoreCase(scheme)) {
                    AppLogger.log("WVM", "override: grow:// token redirect");
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, next);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        this.baseActivity.startActivity(intent);
                    } catch (Exception ex) {
                        AppLogger.error("WVM", "grow:// route failed: " + ex.getMessage());
                    }
                    WebViewManager.this.HideWebView();
                    return true;
                }

                String nh = next.getHost();

                // Ubisoft dashboard redirect — the device POST was accepted.
                // Swap /player/login/dashboard → /google/redirect (same token), fire Chrome.
                // The WebView is only a silent POST transport; the user never needs to see this page.
                if (nh != null && nh.contains("login.growtopiagame.com")
                        && url.contains("/player/login/dashboard")) {
                    String googleUrl = url.replace("/player/login/dashboard", "/google/redirect");
                    AppLogger.log("WVM", "dashboard→google/redirect → Chrome: " + googleUrl);
                    Toast.makeText(this.baseActivity,
                            "Opening Google login in Chrome…", Toast.LENGTH_SHORT).show();
                    WebViewManager.this.HideWebView();
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.baseActivity.startActivity(i);
                    return true;
                }

                // Fallback: any Google OAuth page that somehow slipped through above.
                if (nh != null && (nh.contains("accounts.google.com")
                        || (nh.contains("google.com") && (url.contains("/o/oauth2") || url.contains("/ServiceLogin"))))) {
                    AppLogger.log("WVM", "override: Google OAuth URL → Chrome: " + url);
                    Toast.makeText(this.baseActivity,
                            "Launching Chrome for Google login…", Toast.LENGTH_SHORT).show();
                    WebViewManager.this.HideWebView();
                    Intent i = new Intent(Intent.ACTION_VIEW, next);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.baseActivity.startActivity(i);
                    return true;
                }

                Uri orig = Uri.parse(WebViewManager.originalURL == null ? "" : WebViewManager.originalURL);
                String oh = orig.getHost();
                if (!WebViewManager.this.allowExternalLinks || oh == null || nh == null || oh.equals(nh)) {
                    return false; // let WebView handle same-origin navigation normally
                }
                this.baseActivity.startActivity(new Intent(Intent.ACTION_VIEW, next));
                return true;
            } catch (Exception e) {
                AppLogger.error("WVM", "override error: " + e.getMessage());
                return false;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            AppLogger.log("WVM", "onPageStarted: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            AppLogger.log("WVM", "onPageFinished: " + url);
            this.listener.OnPageLoaded(url);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
            super.onReceivedError(v, req, err);
            AppLogger.error("WVM", "onReceivedError code=" + err.getErrorCode()
                    + " url=" + (req.getUrl() == null ? "?" : req.getUrl().toString()));
            this.listener.OnError(err.getErrorCode());
        }
    }
}
