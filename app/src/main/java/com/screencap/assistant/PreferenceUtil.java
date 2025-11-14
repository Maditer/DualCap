package com.screencap.assistant;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * SharedPreferences工具类，用于简化数据存储操作
 */
public class PreferenceUtil {

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存服务启用状态
     */
    public static void saveServiceEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(Constants.KEY_SERVICE_ENABLED, enabled)
                .apply();
    }

    /**
     * 获取服务启用状态
     */
    public static boolean getServiceEnabled(Context context) {
        return getPreferences(context).getBoolean(
                Constants.KEY_SERVICE_ENABLED, Constants.DEFAULT_SERVICE_ENABLED);
    }

    /**
     * 检查是否曾经设置过服务开关（用于判断是否首次授权）
     */
    public static boolean hasSetServiceEnabled(Context context) {
        return getPreferences(context).contains(Constants.KEY_SERVICE_ENABLED);
    }

    /**
     * 保存功能列表
     */
    public static void saveFeatureList(Context context, List<FeatureItem> featureList) {
        Gson gson = new Gson();
        String json = gson.toJson(featureList);
        getPreferences(context).edit()
                .putString(Constants.KEY_FEATURE_LIST, json)
                .apply();
    }

    /**
     * 获取功能列表
     */
    public static List<FeatureItem> getFeatureList(Context context) {
        String json = getPreferences(context).getString(Constants.KEY_FEATURE_LIST, null);
        if (json == null) {
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<FeatureItem>>() {}.getType();
        List<FeatureItem> featureItems = gson.fromJson(json, type);
        
        // 确保每个功能项的颜色资源ID与功能类型正确对应
        for (FeatureItem item : featureItems) {
            switch (item.getType()) {
                case Constants.FEATURE_MAIN:
                    item.setColorResId(R.color.capture_main);
                    break;
                case Constants.FEATURE_SUB:
                    item.setColorResId(R.color.capture_sub);
                    break;
                case Constants.FEATURE_BOTH:
                    item.setColorResId(R.color.capture_both);
                    break;
                case Constants.FEATURE_HOME:
                    item.setColorResId(R.color.capture_home);
                    break;
            }
        }
        
        return featureItems;
    }

    /**
     * 保存悬浮窗高度
     */
    public static void saveOverlayHeight(Context context, int height) {
        // 限制在有效范围内
        height = Math.max(Constants.MIN_OVERLAY_HEIGHT, 
                Math.min(Constants.MAX_OVERLAY_HEIGHT, height));
        getPreferences(context).edit()
                .putInt(Constants.KEY_OVERLAY_HEIGHT, height)
                .apply();
    }

    /**
     * 获取悬浮窗高度
     */
    public static int getOverlayHeight(Context context) {
        return getPreferences(context).getInt(
                Constants.KEY_OVERLAY_HEIGHT, Constants.DEFAULT_OVERLAY_HEIGHT);
    }

    /**
     * 保存双屏截屏间隔
     */
    public static void saveScreenshotDelay(Context context, int delayMs) {
        // 限制在有效范围内
        delayMs = Math.max(Constants.MIN_SCREENSHOT_DELAY, 
                Math.min(Constants.MAX_SCREENSHOT_DELAY, delayMs));
        getPreferences(context).edit()
                .putInt(Constants.KEY_SCREENSHOT_DELAY, delayMs)
                .apply();
    }

    /**
     * 获取双屏截屏间隔
     */
    public static int getScreenshotDelay(Context context) {
        return getPreferences(context).getInt(
                Constants.KEY_SCREENSHOT_DELAY, Constants.DEFAULT_SCREENSHOT_DELAY);
    }

    /**
     * 保存在任务管理器中隐藏设置
     */
    public static void saveHideFromRecents(Context context, boolean hide) {
        getPreferences(context).edit()
                .putBoolean(Constants.KEY_HIDE_FROM_RECENTS, hide)
                .apply();
    }

    /**
     * 获取在任务管理器中隐藏设置
     */
    public static boolean getHideFromRecents(Context context) {
        return getPreferences(context).getBoolean(
                Constants.KEY_HIDE_FROM_RECENTS, Constants.DEFAULT_HIDE_FROM_RECENTS);
    }

    /**
     * 保存截屏音效开关状态
     */
    public static void saveSoundEffectEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(Constants.KEY_SOUND_EFFECT_ENABLED, enabled)
                .apply();
    }

    /**
     * 获取截屏音效开关状态
     */
    public static boolean getSoundEffectEnabled(Context context) {
        return getPreferences(context).getBoolean(
                Constants.KEY_SOUND_EFFECT_ENABLED, Constants.DEFAULT_SOUND_EFFECT_ENABLED);
    }

    /**
     * 保存套壳截屏开关状态
     */
    public static void saveEnableFrameScreenshot(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(Constants.KEY_FRAME_SCREENSHOT_ENABLED, enabled)
                .apply();
    }

    /**
     * 获取套壳截屏开关状态
     */
    public static boolean getEnableFrameScreenshot(Context context) {
        return getPreferences(context).getBoolean(
                Constants.KEY_FRAME_SCREENSHOT_ENABLED, Constants.DEFAULT_FRAME_SCREENSHOT_ENABLED);
    }

    /**
     * 保存机身颜色选择
     */
    public static void saveFrameColorIndex(Context context, int colorIndex) {
        getPreferences(context).edit()
                .putInt(Constants.KEY_FRAME_COLOR_INDEX, colorIndex)
                .apply();
    }

    /**
     * 获取机身颜色选择
     */
    public static int getFrameColorIndex(Context context) {
        return getPreferences(context).getInt(
                Constants.KEY_FRAME_COLOR_INDEX, Constants.DEFAULT_FRAME_COLOR_INDEX);
    }

    /**
     * 保存套壳截屏图像质量
     */
    public static void saveFrameImageQuality(Context context, int quality) {
        // 限制在有效范围内
        quality = Math.max(Constants.MIN_FRAME_IMAGE_QUALITY, 
                Math.min(Constants.MAX_FRAME_IMAGE_QUALITY, quality));
        getPreferences(context).edit()
                .putInt(Constants.KEY_FRAME_IMAGE_QUALITY, quality)
                .apply();
    }

    /**
     * 获取套壳截屏图像质量
     */
    public static int getFrameImageQuality(Context context) {
        return getPreferences(context).getInt(
                Constants.KEY_FRAME_IMAGE_QUALITY, Constants.DEFAULT_FRAME_IMAGE_QUALITY);
    }
}