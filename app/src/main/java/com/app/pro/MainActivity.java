package com.app.pro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppSettingsChangedListener {

    private RecyclerView recyclerView;
    private EditText editTextSearch;
    private AppAdapter adapter;
    private List<AppInfo> installedApps;
    private List<AppInfo> filteredApps;
    private Toolbar toolbar;
    private static final String TAG = "MainActivity";

    private SharedPreferences prefs;
    private Set<String> blockedPackagesSet;
    private static final String PREF_BLOCKED_APPS_SET = "blocked_packages_set";
    private static final String PREFS_NAME = "settings_prefs";
    private static final String KEY_LIMIT_PREFIX = "limit_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Activity starting.");

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerView = findViewById(R.id.recyclerViewApps);

        if (recyclerView == null || editTextSearch == null || toolbar == null) {
            Log.e(TAG, "onCreate: Critical view not found!");
            finish();
            return;
        }
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadBlockedAppsSet();
        checkAndPromptSpecialPermissions();

        installedApps = new ArrayList<>();
        filteredApps = new ArrayList<>();

        setupRecyclerView();
        setupSearch();
        loadInstalledApps();

        handleIntentForDialog(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(TAG, ">>> onNewIntent called. Intent Action: " + intent.getAction() + ", Extras: " + intent.getExtras());
        setIntent(intent);
        handleIntentForDialog(intent);
    }


    private void handleIntentForDialog(Intent intent) {

        Log.d(TAG, ">>> handleIntentForDialog called. Checking Intent: " + intent);
        if (intent != null) {

            boolean triggered = intent.getBooleanExtra("block_triggered", false);
            int count = intent.getIntExtra("attempt_count", -1);
            String blockedPackage = intent.getStringExtra("blocked_package_name");
            Log.d(TAG, "handleIntentForDialog: Intent extras read - block_triggered=" + triggered + ", attempt_count=" + count + ", blocked_package=" + blockedPackage);


            if (triggered) {
                Log.d(TAG, "handleIntentForDialog: Block WAS triggered! Calling showBlockAttemptDialog...");
                showBlockAttemptDialog(count, blockedPackage);

                intent.removeExtra("block_triggered");
            } else {
                Log.d(TAG, "handleIntentForDialog: 'block_triggered' extra was false or missing.");
            }
        } else {
            Log.d(TAG, "handleIntentForDialog: Intent was null.");
        }
    }

    // Na początku metody showBlockAttemptDialog:
    private void showBlockAttemptDialog(int attemptCount, String blockedPackage) {
        // Loguj wejście do metody i przekazane parametry
        Log.d(TAG, ">>> showBlockAttemptDialog called. Count: " + attemptCount + ", Pkg: " + blockedPackage);
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showBlockAttemptDialog: Activity is finishing/destroyed, cannot show dialog.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_app_blocked);

        String message;
        if (blockedPackage != null && !blockedPackage.isEmpty()) {
            message = getString(R.string.dialog_message_app_blocked, blockedPackage, attemptCount);
        } else {
            message = getString(R.string.dialog_message_app_blocked_no_pkg, attemptCount);
        }

        builder.setMessage(message);
        builder.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG, "showBlockAttemptDialog: dialog.show() called.");
    }

    private void loadBlockedAppsSet() {
        blockedPackagesSet = new HashSet<>(prefs.getStringSet(PREF_BLOCKED_APPS_SET, new HashSet<>()));
        Log.d(TAG, "loadBlockedAppsSet: Loaded " + blockedPackagesSet.size() + " blocked packages.");
    }

    private void saveBlockedAppsSet() {
        prefs.edit().putStringSet(PREF_BLOCKED_APPS_SET, blockedPackagesSet).apply();
        Log.d(TAG, "saveBlockedAppsSet: Saved " + blockedPackagesSet.size() + " blocked packages.");
    }

    @Override
    public void onBlockedStateChanged(String packageName, boolean isBlocked) {
        Log.d(TAG, "onBlockedStateChanged: Package " + packageName + ", isBlocked: " + isBlocked);

        if (isBlocked) {
            blockedPackagesSet.add(packageName);
        } else {
            blockedPackagesSet.remove(packageName);
            Log.i(TAG, "Checkbox unchecked for " + packageName + ". Resetting limit and removing from over-limit set.");
            prefs.edit().putInt(KEY_LIMIT_PREFIX + packageName, -1).apply();
            Set<String> currentOverLimitSet = new HashSet<>(prefs.getStringSet(UsageCheckWorker.PREF_OVER_LIMIT_APPS_SET, new HashSet<>()));
            if (currentOverLimitSet.remove(packageName)) {
                prefs.edit().putStringSet(UsageCheckWorker.PREF_OVER_LIMIT_APPS_SET, currentOverLimitSet).apply();
                Log.i(TAG, "Removed " + packageName + " from over-limit set because checkbox was unchecked.");
            }
        }
        saveBlockedAppsSet();
    }

    @Override
    public void onLimitChanged(String packageName, int totalMinutes) {
        Log.d(TAG, "onLimitChanged: Package " + packageName + ", totalMinutes: " + totalMinutes);
        prefs.edit().putInt(KEY_LIMIT_PREFIX + packageName, totalMinutes).apply();

        if (totalMinutes < 0) {
            Log.d(TAG, "Limit removed via settings for " + packageName + ". Removing from over-limit set.");
            Set<String> currentOverLimitSet = new HashSet<>(prefs.getStringSet(UsageCheckWorker.PREF_OVER_LIMIT_APPS_SET, new HashSet<>()));
            if (currentOverLimitSet.remove(packageName)) {
                prefs.edit().putStringSet(UsageCheckWorker.PREF_OVER_LIMIT_APPS_SET, currentOverLimitSet).apply();
                Log.i(TAG, "Removed " + packageName + " from over-limit set via onLimitChanged.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter(this, filteredApps);
        adapter.setOnAppSettingsChangedListener(this);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "setupRecyclerView: RecyclerView setup complete with listener.");
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        Log.d(TAG, "setupSearch: TextWatcher added.");
    }

    private void filterApps(String query) {
        String normalizedQuery = query.toLowerCase(Locale.getDefault()).trim();
        List<AppInfo> newlyFiltered = new ArrayList<>();

        if (normalizedQuery.isEmpty()) {
            newlyFiltered.addAll(installedApps);
        } else {
            for (AppInfo app : installedApps) {
                if (app.getName().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    newlyFiltered.add(app);
                }
            }
        }

        filteredApps.clear();
        filteredApps.addAll(newlyFiltered);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "filterApps: Adapter is null!");
        }
        Log.d(TAG, "filterApps: Filter applied. Showing " + filteredApps.size() + " apps.");
    }

    private void loadInstalledApps() {
        Log.d(TAG, "loadInstalledApps: Starting background load...");
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            final List<AppInfo> loadedMasterApps = new ArrayList<>();

            for (ApplicationInfo packageInfo : packages) {
                if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    try {
                        String appName = pm.getApplicationLabel(packageInfo).toString();
                        Drawable appIcon = pm.getApplicationIcon(packageInfo);
                        String packageName = packageInfo.packageName;
                        boolean isBlocked = blockedPackagesSet.contains(packageName);
                        int limitMinutes = prefs.getInt(KEY_LIMIT_PREFIX + packageName, -1);
                        loadedMasterApps.add(new AppInfo(appName, appIcon, packageName, isBlocked, limitMinutes));
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading app info for " + (packageInfo!=null? packageInfo.packageName : "unknown") + ": " + e.getMessage());
                    }
                }
            }
            Collections.sort(loadedMasterApps, (a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));
            Log.d(TAG, "loadInstalledApps: Background load finished. " + loadedMasterApps.size() + " user apps loaded.");

            runOnUiThread(() -> {
                installedApps.clear();
                installedApps.addAll(loadedMasterApps);
                filterApps(editTextSearch.getText().toString());
                Log.d(TAG, "loadInstalledApps: Lists updated on UI thread.");
            });
        }).start();
    }

    private void checkAndPromptSpecialPermissions() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean usageStatsEnabled = hasUsageStatsPermission();
        String message = "";

        if (!accessibilityEnabled && !usageStatsEnabled) {
            message = "Włącz usługę dostępności i dostęp do statystyk użycia, aby aplikacja działała poprawnie.";
        } else if (!accessibilityEnabled) {
            message = "Włącz usługę dostępności, aby blokowanie działało.";
        } else if (!usageStatsEnabled) {
            message = "Włącz dostęp do statystyk użycia, aby limit czasowy działał.";
        }

        if (!accessibilityEnabled || !usageStatsEnabled) {
            Log.w(TAG, "Special permission(s) missing. Prompting user. Accessibility: " + accessibilityEnabled + ", UsageStats: " + usageStatsEnabled);
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("USTAWIENIA", view -> {
                    if (!isAccessibilityServiceEnabled()) {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        try {
                            startActivity(intent);
                            Toast.makeText(this, "Znajdź 'ProcrastinateApp Blocker' i włącz usługę.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) { Log.e(TAG, "Could not open Accessibility Settings", e); }
                    } else {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        try {
                            startActivity(intent);
                            Toast.makeText(this, "Znajdź 'ProcrastinateApp' i włącz dostęp.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) { Log.e(TAG, "Could not open Usage Access Settings", e); }
                    }
                });
                snackbar.show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.i(TAG, "Both Accessibility Service and Usage Stats access are enabled.");
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) { return false; }
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String serviceId = getPackageName() + "/" + AppBlockerService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding accessibility setting: " + e.getMessage());
            return false;
        }
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String accessibilityService = colonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(serviceId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}