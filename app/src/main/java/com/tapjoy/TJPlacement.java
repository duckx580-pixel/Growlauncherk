package com.tapjoy;

import android.content.Context;

public class TJPlacement {
    private String name;
    private TJPlacementListener listener;

    public interface TJPlacementListener {
        void onRequestSuccess(TJPlacement placement);
        void onRequestFailure(TJPlacement placement, TJError error);
        void onContentReady(TJPlacement placement);
        void onContentShow(TJPlacement placement);
        void onContentDismiss(TJPlacement placement);
        void onPurchaseRequest(TJPlacement placement, TJActionRequest request, String productId);
        void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId, int quantity);
    }

    public TJPlacement(Context context, String name, TJPlacementListener listener) {
        this.name = name;
        this.listener = listener;
    }

    public void requestContent() {}
    public void showContent() {}
    public boolean isContentReady() { return false; }
    public boolean isContentAvailable() { return false; }
    public String getName() { return name; }
}
