
package com.app.pro;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UsageCheckWorker extends Worker {

    private static final String TAG = "UsageCheckWorker";
    private static final String PREFS_NAME = "settings_prefs";
    private static final String KEY_LIMIT_PREFIX = "limit_";
    public static final String PREF_OVER_LIMIT_APPS_SET = "over_limit_packages_set";
    private static final String PREF_BLOCKED_APPS_SET = "blocked_packages_set";

    public UsageCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: Starting usage check...");
        Context context = getApplicationContext();

        if (!hasUsageStatsPermission(context)) {
            Log.w(TAG, "doWork: Usage Stats permission not granted. Cannot check usage.");
            return Result.success();
        }

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            Log.e(TAG, "doWork: UsageStatsManager not available.");
            return Result.failure();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> appsOverLimit = new HashSet<>();
        Set<String> currentManualBlocks = new HashSet<>(prefs.getStringSet(PREF_BLOCKED_APPS_SET, new HashSet<>()));
        boolean manualBlocksChanged = false;

        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Log.i(TAG, "doWork: No usage stats found for today.");
            prefs.edit().putStringSet(PREF_OVER_LIMIT_APPS_SET, appsOverLimit).apply();
            return Result.success();
        }

        Log.d(TAG, "doWork: Found " + usageStatsList.size() + " usage stats entries.");

        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_LIMIT_PREFIX)) {
                String packageName = key.substring(KEY_LIMIT_PREFIX.length());
                int limitMinutes = prefs.getInt(key, -1);

                if (limitMinutes >= 0) {
                    long limitMillis = TimeUnit.MINUTES.toMillis(limitMinutes);
                    long usageMillis = 0;

                    for (UsageStats stats : usageStatsList) {
                        if (stats.getPackageName().equals(packageName)) {
                            usageMillis = stats.getTotalTimeInForeground();
                            break;
                        }
                    }

                    Log.d(TAG, "Checking limit for " + packageName + ": Usage=" + usageMillis + "ms, Limit=" + limitMillis + "ms (" + limitMinutes + "min)");

                    if (usageMillis > limitMillis) {
                        Log.i(TAG, "Limit EXCEEDED for: " + packageName);
                        appsOverLimit.add(packageName);
                        if (currentManualBlocks.add(packageName)) {
                            manualBlocksChanged = true;
                            Log.i(TAG, "Added " + packageName + " to manual block list due to limit.");
                        }
                    }
                }
            }
        }

        Log.i(TAG, "doWork: Apps over limit: " + appsOverLimit.toString());
        prefs.edit().putStringSet(PREF_OVER_LIMIT_APPS_SET, appsOverLimit).apply();

        if (manualBlocksChanged) {
            Log.i(TAG, "doWork: Saving updated manual block list: " + currentManualBlocks.toString());
            prefs.edit().putStringSet(PREF_BLOCKED_APPS_SET, currentManualBlocks).apply();
        }

        return Result.success();
    }

    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
