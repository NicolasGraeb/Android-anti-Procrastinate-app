package com.app.pro; // Twój poprawny pakiet

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings; // Import dla Settings
import android.text.Editable;
import android.text.TextUtils; // Import dla TextUtils
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View; // Import dla View
import android.widget.EditText;
import android.widget.Toast; // Import dla Toast

import com.google.android.material.snackbar.Snackbar; // Import dla Snackbar

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppBlockedStateChangedListener {

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
        checkAndPromptAccessibility();

        installedApps = new ArrayList<>();
        filteredApps = new ArrayList<>();

        setupRecyclerView();
        setupSearch();
        loadInstalledApps();
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
        }
        saveBlockedAppsSet();

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
        adapter.setOnAppBlockedStateChangedListener(this);
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
                        loadedMasterApps.add(new AppInfo(appName, appIcon, packageName, isBlocked));
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

    private void checkAndPromptAccessibility() {
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Accessibility Service is NOT enabled. Prompting user.");
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, "Włącz usługę dostępności, aby blokowanie działało.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("USTAWIENIA", view -> {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            try {
                                startActivity(intent);
                                Toast.makeText(this, "Znajdź 'ProcrastinateApp Blocker' i włącz usługę.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Could not open Accessibility Settings", e);
                                Toast.makeText(this, "Nie można otworzyć ustawień dostępności.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            } else {
                Toast.makeText(this, "Włącz usługę dostępności w Ustawieniach, aby blokowanie działało.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.i(TAG, "Accessibility Service is enabled.");
        }
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