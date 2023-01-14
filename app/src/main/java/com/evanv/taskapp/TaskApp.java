package com.evanv.taskapp;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

/**
 * Application class for timezone initialization
 *
 * @author Evan Voogd
 */
public class TaskApp extends Application {

    /**
     * Creates app and sets time zones
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Set up timezones.
        AndroidThreeTen.init(this);
    }
}
