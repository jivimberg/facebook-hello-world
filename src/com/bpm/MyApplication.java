package com.bpm;

import android.app.Application;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class MyApplication extends Application {

    private static final String APP_ID = "433688043339608";

    public Facebook facebook;
    public AsyncFacebookRunner asyncRunner;

    @Override
    public void onCreate() {
        super.onCreate();
        facebook = new Facebook(APP_ID);
        asyncRunner = new AsyncFacebookRunner(facebook);
    }
}
