package com.screencap.assistant;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * 显示相关工具类
 */
public class DisplayUtil {

    private static final String TAG = "DisplayUtil";

    /**
     * dp转换为px
     */
    public static int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

    /**
     * px转换为dp
     */
    public static int pxToDp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(px / scale);
    }

    /**
     * 获取主屏Display对象
     */
    public static Display getMainDisplay(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        if (displays != null && displays.length > 0) {
            return displays[0];
        }
        return null;
    }

    /**
     * 根据displayId获取Display对象
     * @param context Context对象
     * @param displayId 显示屏ID
     * @return 对应ID的Display对象
     */
    public static Display getDisplayById(Context context, int displayId) {
        // 使用DisplayManager获取所有显示器
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        for (Display display : displays) {
            if (display.getDisplayId() == displayId) {
                return display;
            }
        }
        return null;
    }

    /**
     * 获取副屏Display对象
     * 遍历所有Display,找到非主屏的第一个显示器
     */
    public static Display getSecondaryDisplay(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        
        if (displays == null || displays.length <= 1) {
            return null;
        }
        
        // 打印所有Display信息用于调试
        for (Display display : displays) {
            Log.d(TAG, "Display ID: " + display.getDisplayId() + 
                    ", Name: " + display.getName() +
                    ", State: " + display.getState());
        }
        
        // 遍历所有显示器,找到第一个非DEFAULT_DISPLAY的显示器
        for (Display display : displays) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                Log.i(TAG, "找到副屏 - Display ID: " + display.getDisplayId() + 
                        ", Name: " + display.getName());
                return display;
            }
        }
        
        return null;
    }

    /**
     * 获取主屏ID
     */
    public static int getMainDisplayId(Context context) {
        Display display = getMainDisplay(context);
        return display != null ? display.getDisplayId() : Display.DEFAULT_DISPLAY;
    }

    /**
     * 获取副屏ID
     * 返回真实的副屏ID(例如4),而不是数组索引
     */
    public static int getSecondaryDisplayId(Context context) {
        Display display = getSecondaryDisplay(context);
        int displayId = display != null ? display.getDisplayId() : -1;
        Log.i(TAG, "副屏ID: " + displayId);
        return displayId;
    }
}