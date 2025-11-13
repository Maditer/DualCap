package com.screencap.assistant;

/**
 * 功能项数据类，用于表示设置页面中的功能列表项
 */
public class FeatureItem {
    private String id;
    private String name;
    private int type;
    private boolean enabled;
    private int colorResId;

    public FeatureItem(String id, String name, int type, boolean enabled, int colorResId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.colorResId = colorResId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getColorResId() {
        return colorResId;
    }

    public void setColorResId(int colorResId) {
        this.colorResId = colorResId;
    }
}