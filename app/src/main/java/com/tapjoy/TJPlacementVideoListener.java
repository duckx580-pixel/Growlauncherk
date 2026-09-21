package com.tapjoy;

public interface TJPlacementVideoListener {
    void onVideoComplete(TJPlacement placement);
    void onVideoError(TJPlacement placement, String error);
    void onVideoStart(TJPlacement placement);
}
