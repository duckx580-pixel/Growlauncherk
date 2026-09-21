package com.rtsoft.growtopia;

import android.app.Activity;
import android.util.Log;
import com.usercentrics.sdk.BannerSettings;
import com.usercentrics.sdk.Usercentrics;
import com.usercentrics.sdk.UsercentricsBanner;
import com.usercentrics.sdk.UsercentricsOptions;
import com.usercentrics.sdk.UsercentricsServiceConsent;
import java.util.List;

/** Consent bridge matching the working launcher. */
public class UsercentricsManager {
    private static final String TAG = "GTConsent";
    private final Activity baseContext;

    public UsercentricsManager(Activity activity) {
        this.baseContext = activity;
    }

    private void initUsercentrics(UsercentricsOptions options) {
        Log.i(TAG, "initialize requested");
        Usercentrics.initialize(baseContext, options);
        baseContext.runOnUiThread(() -> Usercentrics.isReady(
                status -> {
                    Log.i(TAG, "initialize ready; calling native InitFinish(true)");
                    InitFinish(true);
                    return kotlin.Unit.INSTANCE;
                },
                error -> {
                    Log.e(TAG, "initialize failed; calling native InitFinish(false)", error);
                    InitFinish(false);
                    return kotlin.Unit.INSTANCE;
                }));
    }

    public void CheckConsentState() {
        Log.i(TAG, "CheckConsentState called by native");
        baseContext.runOnUiThread(() -> Usercentrics.isReady(
                status -> {
                    Log.i(TAG, "consent ready collect=" + status.getShouldCollectConsent()
                            + " count=" + status.getConsents().size());
                    if (status.getShouldCollectConsent()) RequestConsentSettings();
                    else FetchUserConsent(status.getConsents());
                    return kotlin.Unit.INSTANCE;
                },
                error -> {
                    Log.e(TAG, "consent state failed", error);
                    OnConsentFetchedFail(-1, error.getLocalizedMessage());
                    return kotlin.Unit.INSTANCE;
                }));
    }

    public void FetchUserConsent(List<UsercentricsServiceConsent> consents) {
        Log.i(TAG, "returning " + (consents == null ? -1 : consents.size()) + " consents to native");
        OnConsentFetchedSuccess(consents);
        Log.i(TAG, "native OnConsentFetchedSuccess returned");
    }

    public native void InitFinish(boolean success);

    public void InitWithRuleSet(String ruleSetId) {
        Log.i(TAG, "InitWithRuleSet " + ruleSetId);
        UsercentricsOptions options = new UsercentricsOptions();
        options.setRuleSetId(ruleSetId);
        initUsercentrics(options);
    }

    public void InitWithSettings(String settingsId) {
        Log.i(TAG, "InitWithSettings " + settingsId);
        UsercentricsOptions options = new UsercentricsOptions();
        options.setSettingsId(settingsId);
        initUsercentrics(options);
    }

    public native void OnConsentFetchedFail(int code, String message);
    public native void OnConsentFetchedSuccess(List<UsercentricsServiceConsent> consents);

    public void RequestConsentSettings() {
        Log.i(TAG, "showing first consent layer");
        baseContext.runOnUiThread(() -> new UsercentricsBanner(baseContext, new BannerSettings())
                .showFirstLayer(response -> {
                    Log.i(TAG, "first consent layer response=" + (response != null));
                    if (response != null) FetchUserConsent(response.getConsents());
                    return kotlin.Unit.INSTANCE;
                }));
    }

    public void ShowConsentSettings() {
        baseContext.runOnUiThread(() -> new UsercentricsBanner(baseContext, new BannerSettings())
                .showSecondLayer(response -> {
                    if (response != null) FetchUserConsent(response.getConsents());
                    return kotlin.Unit.INSTANCE;
                }));
    }

    public void destroy() {}
}
