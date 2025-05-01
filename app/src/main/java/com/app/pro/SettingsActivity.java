
package com.app.pro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;


public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private RadioGroup radioGroupTheme;
    private RadioButton radioLight, radioDark, radioSystem;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_settings);
        }

        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);
        radioSystem = findViewById(R.id.radioSystem);

        if (radioGroupTheme == null || radioLight == null || radioDark == null || radioSystem == null) {
            Log.e(TAG, "onCreate: One or more RadioButtons or RadioGroup not found!");
            finish();
            return;
        }

        prefs = getSharedPreferences(MyApp.PREFS_NAME, Context.MODE_PRIVATE);

        loadCurrentThemeSelection();
        setupThemeSelectionListener();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadCurrentThemeSelection() {
        String currentTheme = prefs.getString(MyApp.KEY_THEME, MyApp.THEME_SYSTEM);
        Log.d(TAG, "Loading theme: " + currentTheme);
        switch (currentTheme) {
            case MyApp.THEME_LIGHT: radioLight.setChecked(true); break;
            case MyApp.THEME_DARK: radioDark.setChecked(true); break;
            default: radioSystem.setChecked(true); break;
        }
    }

    private void setupThemeSelectionListener() {
        radioGroupTheme.setOnCheckedChangeListener(null);
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedThemePref;
            if (checkedId == R.id.radioLight) selectedThemePref = MyApp.THEME_LIGHT;
            else if (checkedId == R.id.radioDark) selectedThemePref = MyApp.THEME_DARK;
            else selectedThemePref = MyApp.THEME_SYSTEM;

            String currentTheme = prefs.getString(MyApp.KEY_THEME, MyApp.THEME_SYSTEM);
            if (!selectedThemePref.equals(currentTheme)) {
                Log.d(TAG, "Theme changed to: " + selectedThemePref);
                prefs.edit().putString(MyApp.KEY_THEME, selectedThemePref).apply();
                MyApp.setTheme(selectedThemePref);
            }
        });
        loadCurrentThemeSelection();
    }
}
