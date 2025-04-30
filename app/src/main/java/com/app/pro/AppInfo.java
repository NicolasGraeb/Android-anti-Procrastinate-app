package com.app.pro;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String name;
    private Drawable icon;
    private String packageName;
    private boolean isBlocked;


    public AppInfo(String name, Drawable icon, String packageName, boolean isBlocked) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
        this.isBlocked = isBlocked;
    }

    // Gettery
    public String getName() { return name; }
    public Drawable getIcon() { return icon; }
    public String getPackageName() { return packageName; }
    public boolean isBlocked() { return isBlocked; }

    // Setter
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}