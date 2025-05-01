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
    private static final String PREF_OVER_LIMIT_APPS_SET = UsageCheckWorker.PREF_OVER_LIMIT_APPS_SET;
    private static final String PREF_BLOCK_ATTEMPT_COUNT = "block_attempt_count";

    private Set<String> manuallyBlockedSet = new HashSet<>();
    private Set<String> overLimitSet = new HashSet<>();
    private String launcherPackageName = "";
    private String myPackageName = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "------- onServiceConnected -------");

        myPackageName = getPackageName();
        launcherPackageName = getLauncherPackageName();
        loadManuallyBlockedApps();
        loadOverLimitApps();

        Log.i(TAG, "Service Connected. MyPkg: " + myPackageName + ", Launcher: " + launcherPackageName);
        Log.i(TAG, "Initial Manually blocked apps: " + manuallyBlockedSet.toString());
        Log.i(TAG, "Initial Over Limit apps: " + overLimitSet.toString());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageNameCharSeq = event.getPackageName();
            if (packageNameCharSeq != null && packageNameCharSeq.length() > 0) {
                String foregroundAppPackage = packageNameCharSeq.toString();


                if (myPackageName == null || myPackageName.isEmpty()) myPackageName = getPackageName();
                if (launcherPackageName == null || launcherPackageName.isEmpty()) launcherPackageName = getLauncherPackageName();

                if (foregroundAppPackage.equals(myPackageName) || foregroundAppPackage.equals(launcherPackageName)) {
                    return;
                }


                loadManuallyBlockedApps();
                loadOverLimitApps();


                boolean isManuallyBlocked = manuallyBlockedSet.contains(foregroundAppPackage);
                boolean isOverLimit = overLimitSet.contains(foregroundAppPackage);
                boolean shouldBlock = isManuallyBlocked || isOverLimit;



                if (shouldBlock) {
                    Log.i(TAG, "*** BLOCKING app: " + foregroundAppPackage + " ***");


                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    int currentAttemptCount = prefs.getInt(PREF_BLOCK_ATTEMPT_COUNT, 0);
                    int newAttemptCount = currentAttemptCount + 1;
                    prefs.edit().putInt(PREF_BLOCK_ATTEMPT_COUNT, newAttemptCount).apply();
                    Log.i(TAG, "Block attempt count incremented and saved: " + newAttemptCount);


                    bringMyAppToFront(newAttemptCount, foregroundAppPackage);
                }
            }
        }
    }

    private void loadManuallyBlockedApps() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        manuallyBlockedSet = new HashSet<>(prefs.getStringSet(PREF_BLOCKED_APPS_SET, new HashSet<>()));
    }

    private void loadOverLimitApps() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        overLimitSet = new HashSet<>(prefs.getStringSet(PREF_OVER_LIMIT_APPS_SET, new HashSet<>()));
    }

    private void bringMyAppToFront(int attemptCount, String blockedPackage) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("block_triggered", true);
            intent.putExtra("attempt_count", attemptCount);
            intent.putExtra("blocked_package_name", blockedPackage);
            startActivity(intent);
            Log.i(TAG, "Intent to bring MainActivity to front sent with extras: trigger=true, count=" + attemptCount + ", pkg=" + blockedPackage);
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
    public void onInterrupt() { Log.w(TAG, "Accessibility Service Interrupted"); }
    @Override
    public void onDestroy() { super.onDestroy(); Log.w(TAG, "------- Accessibility Service Destroyed -------"); }
}