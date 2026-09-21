package com.android.vending.licensing;

import android.os.IInterface;

public interface ILicenseResultListener extends IInterface {
    void verifyLicense(int responseCode, String signedData, String signature);
}
