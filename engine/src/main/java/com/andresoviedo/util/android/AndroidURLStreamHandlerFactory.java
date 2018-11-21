package com.andresoviedo.util.android;

import com.andresoviedo.util.android.content.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class AndroidURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("assets".equals(protocol)) {
            return new com.andresoviedo.util.android.assets.Handler();
        } else if ("content".equals(protocol)){
            return new Handler();
        }
        return null;
    }
}
