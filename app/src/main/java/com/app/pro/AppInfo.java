package com.app.pro;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String name;
    private Drawable icon;
    private String packageName;
    private boolean isBlocked;
    private boolean isExpanded;
    private int dailyLimitMinutes;

    public AppInfo(String name, Drawable icon, String packageName, boolean isBlocked, int dailyLimitMinutes) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
        this.isBlocked = isBlocked;
        this.isExpanded = false;
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public String getName() { return name; }
    public Drawable getIcon() { return icon; }
    public String getPackageName() { return packageName; }
    public boolean isBlocked() { return isBlocked; }
    public boolean isExpanded() { return isExpanded; }
    public int getDailyLimitMinutes() { return dailyLimitMinutes; }

    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public void setDailyLimitMinutes(int dailyLimitMinutes) { this.dailyLimitMinutes = dailyLimitMinutes; }
}