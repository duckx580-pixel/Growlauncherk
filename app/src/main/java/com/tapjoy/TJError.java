package com.tapjoy;

public class TJError {
    public int code;
    public String message;

    public TJError(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
