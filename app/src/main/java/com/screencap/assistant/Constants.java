package com.screencap.assistant;

import android.view.Gravity;

/**
 * 应用常量定义类
 */
public class Constants {

    // SharedPreferences文件名
    public static final String PREFS_NAME = "dual_screen_screenshot_prefs";
    
    // SharedPreferences键
    public static final String KEY_SERVICE_ENABLED = "service_enabled";
    public static final String KEY_FEATURE_LIST = "feature_list";
    public static final String KEY_OVERLAY_HEIGHT = "overlay_height";
    public static final String KEY_SCREENSHOT_DELAY = "screenshot_delay";
    public static final String KEY_HIDE_FROM_RECENTS = "hide_from_recents";
    public static final String KEY_SOUND_EFFECT_ENABLED = "sound_effect_enabled";
    public static final String KEY_FRAME_SCREENSHOT_ENABLED = "frame_screenshot_enabled"; // 套壳截屏开关
    public static final String KEY_FRAME_COLOR_INDEX = "frame_color_index"; // 机身颜色选择
    public static final String KEY_FRAME_IMAGE_QUALITY = "frame_image_quality"; // 套壳截屏图像质量
    
    // 默认值
    public static final boolean DEFAULT_SERVICE_ENABLED = false;
    public static final int DEFAULT_OVERLAY_HEIGHT = 24; // 默认24dp
    public static final int MIN_OVERLAY_HEIGHT = 5;      // 最小5dp
    public static final int MAX_OVERLAY_HEIGHT = 50;     // 最大50dp
    public static final int DEFAULT_SCREENSHOT_DELAY = 329; // 默认329ms
    public static final boolean DEFAULT_HIDE_FROM_RECENTS = false;
    public static final boolean DEFAULT_SOUND_EFFECT_ENABLED = true; // 默认开启截屏音效
    public static final boolean DEFAULT_FRAME_SCREENSHOT_ENABLED = false; // 默认关闭套壳截屏
    public static final int DEFAULT_FRAME_COLOR_INDEX = 0; // 默认黑色机身
    public static final int DEFAULT_FRAME_IMAGE_QUALITY = 10; // 默认图像质量10(对应PNG格式100%)
    public static final int MIN_FRAME_IMAGE_QUALITY = 6; // 最小图像质量6
    public static final int MAX_FRAME_IMAGE_QUALITY = 10; // 最大图像质量10
    public static final int MIN_SCREENSHOT_DELAY = 0;        // 最小0ms
    public static final int MAX_SCREENSHOT_DELAY = 1000;     // 最大1000ms
    
    // 手势触发最小距离（dp）
    public static final int MIN_GESTURE_DISTANCE = 40;
    
    // 功能类型常量 - 整数类型
    public static final int FEATURE_NONE = 0;
    public static final int FEATURE_SINGLE_SCREEN = 1;
    public static final int FEATURE_DOUBLE_SCREEN = 2;
    public static final int FEATURE_BOTH = 3;
    public static final int FEATURE_HOME = 4;
    // 兼容新的命名习惯
    public static final int FEATURE_MAIN = FEATURE_SINGLE_SCREEN;
    public static final int FEATURE_SUB = FEATURE_DOUBLE_SCREEN;
    
    // 功能类型 - 字符串类型（兼容旧代码）
    public static final String FEATURE_MAIN_STR = "main";
    public static final String FEATURE_SUB_STR = "sub";
    public static final String FEATURE_BOTH_STR = "both";
    
    // 广播动作
    public static final String ACTION_PREVIEW_MODE = "com.dualscreen.ACTION_PREVIEW_MODE";
    public static final String ACTION_RELOAD_CONFIG = "com.dualscreen.ACTION_RELOAD_CONFIG";
    public static final String ACTION_CAPTURE_MAIN = "com.dualscreen.ACTION_CAPTURE_MAIN";
    public static final String ACTION_CAPTURE_SUB = "com.dualscreen.ACTION_CAPTURE_SUB";
    public static final String ACTION_CAPTURE_BOTH = "com.dualscreen.ACTION_CAPTURE_BOTH";
    public static final String ACTION_GO_HOME = "com.dualscreen.ACTION_GO_HOME";
    public static final String ACTION_START_SERVICE = "com.dualscreen.action.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.dualscreen.action.STOP_SERVICE";
    public static final String ACTION_CAPTURE_SCREEN = "com.dualscreen.action.CAPTURE_SCREEN";
    
    // 广播Extra
    public static final String EXTRA_PREVIEW_ENABLED = "preview_enabled";
    public static final String EXTRA_SCREEN_TYPE = "com.dualscreen.extra.SCREEN_TYPE";
    public static final String EXTRA_OVERLAY_HEIGHT = "com.dualscreen.extra.OVERLAY_HEIGHT";
    
    // 文件保存相关
    public static final String SCREENSHOT_DIR = "ThorScreenshots";
    public static final String SAVE_DIR = "ThorScreenshot"; // 兼容代码中的使用
    public static final String SCREENSHOT_PREFIX = "screenshot_";
    public static final String SCREENSHOT_SUFFIX = ".png";
    
    // 透明度常量
    public static final float PREVIEW_MODE_ALPHA = 0.3f;
    public static final float NORMAL_MODE_ALPHA = 0.8f;
    
    // 额外的Extra键
    public static final String EXTRA_SCREENSHOT_TYPE = "com.dualscreen.extra.SCREENSHOT_TYPE";
    
    // 悬浮窗相关常量
    public static final int OVERLAY_WIDTH = 200;
    public static final int DEFAULT_OVERLAY_GRAVITY = Gravity.LEFT | Gravity.TOP;
    public static final int DEFAULT_OVERLAY_X = 50;
    public static final int DEFAULT_OVERLAY_Y = 200;
}
