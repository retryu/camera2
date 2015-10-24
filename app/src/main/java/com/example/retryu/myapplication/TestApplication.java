package com.example.retryu.myapplication;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;


public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
