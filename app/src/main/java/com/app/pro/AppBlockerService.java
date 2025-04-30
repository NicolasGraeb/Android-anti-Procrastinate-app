package com.app.pro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.content.pm.ResolveInfo;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {

    private static final String TAG = "AppBlockerService";
    private static final String PREFS_NAME = "settings_prefs";
    private static final String PREF_BLOCKED_APPS_SET = "blocked_packages_set";

    private Set<String> blockedPackagesSet = new HashSet<>();
    private String launcherPackageName = "";
    private String myPackageName = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "------- onServiceConnected -------");

        myPackageName = getPackageName();
        launcherPackageName = getLauncherPackageName();
        loadBlockedApps();

        Log.i(TAG, "Service Connected. MyPkg: " + myPackageName + ", Launcher: " + launcherPackageName);
        Log.i(TAG, "Initial blocked apps: " + blockedPackagesSet.toString());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            Log.v(TAG, "Event is null");
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageNameCharSeq = event.getPackageName();
            if (packageNameCharSeq != null && packageNameCharSeq.length() > 0) {
                String foregroundAppPackage = packageNameCharSeq.toString();
                Log.d(TAG, "Window state changed: App='" + foregroundAppPackage + "'");

                if (myPackageName == null || myPackageName.isEmpty()) myPackageName = getPackageName();
                if (launcherPackageName == null || launcherPackageName.isEmpty()) launcherPackageName = getLauncherPackageName();

                if (foregroundAppPackage.equals(myPackageName)) {
                    return;
                }
                if (foregroundAppPackage.equals(launcherPackageName)) {
                    return;
                }



                loadBlockedApps();
                Log.d(TAG, "Checking against blocked set: " + blockedPackagesSet.toString());


                boolean shouldBlock = blockedPackagesSet.contains(foregroundAppPackage);
                Log.d(TAG, "Checking [" + foregroundAppPackage + "]... Should block? " + shouldBlock);

                if (shouldBlock) {
                    Log.i(TAG, "*** BLOCKING app: " + foregroundAppPackage + " ***");
                    bringMyAppToFront();
                }
            } else {

            }
        }
    }
    private void loadBlockedApps() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        blockedPackagesSet = new HashSet<>(prefs.getStringSet(PREF_BLOCKED_APPS_SET, new HashSet<>()));

    }

    private void bringMyAppToFront() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            Log.i(TAG, "Intent to bring MainActivity to front sent.");
        } catch (Exception e) {
            Log.e(TAG, "Error trying to bring MainActivity to front", e);
        }
    }

    private String getLauncherPackageName() {
        PackageManager localPackageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        try {
            ResolveInfo resolveInfo = localPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                return resolveInfo.activityInfo.packageName;
            } else {
                Log.w(TAG,"Could not resolve launcher package name - resolveInfo or activityInfo null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not resolve launcher package name", e);
        }
        return "";
    }


    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "------- Accessibility Service Destroyed -------");
    }
}