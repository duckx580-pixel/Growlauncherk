package com.tapjoy;

public interface TJGetCurrencyBalanceListener {
    void onGetCurrencyBalanceResponse(String currencyName, int balance);
    void onGetCurrencyBalanceResponseFailure(String error);
}
