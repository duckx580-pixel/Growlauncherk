package com.rtsoft.growtopia;

import android.content.Context;

public class MAFManager {
    private Context baseContext;

    public MAFManager(Context context) {
        this.baseContext = context;
    }

    public void Init() {}
    public void SetCustomParam(int type, String value) {}
    public void SetUserId(String userId) {}
    public void ShowOfferwall(String adunitId) {}
    public void SetUserConsent(boolean consent) {}
}
