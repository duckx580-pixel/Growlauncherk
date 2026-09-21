package com.android.vending.licensing;

import android.os.IInterface;

public interface ILicensingService extends IInterface {
    void checkLicense(long nonce, String packageName, ILicenseResultListener listener);
}
