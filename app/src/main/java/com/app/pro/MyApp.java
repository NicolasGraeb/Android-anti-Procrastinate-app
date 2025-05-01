
package com.app.pro;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyApp extends Application {

    public static final String PREFS_NAME = "settings_prefs";
    public static final String KEY_THEME = "theme_preference";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    private static final String USAGE_CHECK_WORK_TAG = "UsageCheckWorkerTag";
    private static final String TAG = "MyApp";

    @Override
    public void onCreate() {
        super.onCreate();
        applyAppTheme();
        scheduleUsageCheckWorker();
    }

    private void applyAppTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themePref = prefs.getString(KEY_THEME, THEME_SYSTEM);
        setTheme(themePref);
    }

    public static void setTheme(String themePref) {
        switch (themePref) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void scheduleUsageCheckWorker() {
        PeriodicWorkRequest usageCheckRequest =
                new PeriodicWorkRequest.Builder(UsageCheckWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                USAGE_CHECK_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                usageCheckRequest
        );

        Log.i(TAG, "UsageCheckWorker scheduled.");
    }
}

