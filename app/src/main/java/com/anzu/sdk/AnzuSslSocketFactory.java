package com.anzu.sdk;

import javax.net.ssl.SSLSocketFactory;

public class AnzuSslSocketFactory {
    public static SSLSocketFactory create() { return (SSLSocketFactory) SSLSocketFactory.getDefault(); }
}
