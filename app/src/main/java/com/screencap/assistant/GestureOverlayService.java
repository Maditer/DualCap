package com.screencap.assistant;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 手势悬浮窗服务，用于监听用户手势并触发相应的截图功能
 * 悬浮窗显示在副屏底部，根据启用的功能数量动态分区
 */
public class GestureOverlayService extends Service {

    private static final String TAG = "GestureOverlay_DEBUG";
    private WindowManager mWindowManager;
    private LinearLayout mOverlayView;
    private WindowManager.LayoutParams mParams;
    private boolean mIsPreviewMode = false;
    private List<FeatureItem> mFeatureItems;
    private int mOverlayHeight;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private float mStartY = 0;
    private float mStartX = 0;
    
    // 手势反馈图标相关
    private AppCompatImageView mFeedbackIcon;
    private WindowManager.LayoutParams mIconParams;
    private boolean mIconShowing = false;
    private int mCurrentSectionIndex = -1;
    private boolean mGestureTriggered = false;
    private AnimatorSet mCurrentAnimation;
    private MediaPlayer mMediaPlayer;

    private final BroadcastReceiver mConfigReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_PREVIEW_MODE.equals(intent.getAction())) {
                mIsPreviewMode = intent.getBooleanExtra(Constants.EXTRA_PREVIEW_ENABLED, false);
                updatePreviewMode();
            } else if (Constants.ACTION_RELOAD_CONFIG.equals(intent.getAction())) {
                loadConfig();
                updateOverlayHeight();
                reloadFeatureViews();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========== GestureOverlayService onCreate ==========");
        
        // 加载配置
        loadConfig();
        
        // 注册广播接收器
        registerBroadcastReceiver();
        
        // 初始化悬浮窗（在副屏上）
        initOverlayView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 服务已启动
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 取消注册广播接收器
        unregisterReceiver(mConfigReceiver);
        
        // 释放MediaPlayer资源
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        
        // 移除反馈图标
        if (mFeedbackIcon != null && mWindowManager != null) {
            try {
                mWindowManager.removeView(mFeedbackIcon);
            } catch (Exception e) {
                // 忽略
            }
            mFeedbackIcon = null;
        }
        
        // 移除悬浮窗
        if (mOverlayView != null && mWindowManager != null) {
            mWindowManager.removeView(mOverlayView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        mFeatureItems = PreferenceUtil.getFeatureList(this);
        mOverlayHeight = PreferenceUtil.getOverlayHeight(this);
    }

    /**
     * 注册广播接收器
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_PREVIEW_MODE);
        filter.addAction(Constants.ACTION_RELOAD_CONFIG);
        registerReceiver(mConfigReceiver, filter);
    }

    /**
     * 初始化悬浮窗视图 - 必须在副屏上显示
     */
    private void initOverlayView() {
        try {
            // 获取副屏Display
            Display secondaryDisplay = DisplayUtil.getSecondaryDisplay(this);
            if (secondaryDisplay == null) {
                Toast.makeText(this, "未检测到副屏，无法启动服务", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
            
            // 使用副屏的Context创建WindowManager
            Context displayContext = createDisplayContext(secondaryDisplay);
            mWindowManager = (WindowManager) displayContext.getSystemService(WINDOW_SERVICE);
            
            // 创建悬浮窗参数 - 底部占满整个宽度
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    DisplayUtil.dpToPx(this, mOverlayHeight),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            
            // 设置悬浮窗位置：底部居中，填充宽度
            mParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            mParams.x = 0;
            mParams.y = 0;
            
            // 创建LinearLayout作为容器，水平分布
            mOverlayView = new LinearLayout(displayContext);
            mOverlayView.setOrientation(LinearLayout.HORIZONTAL);
            mOverlayView.setWeightSum(1.0f);
            
            // 设置触摸监听器
            mOverlayView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mStartY = event.getY();
                            mStartX = event.getX();
                            mGestureTriggered = false;
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            float deltaY = mStartY - event.getY();
                            int triggerThreshold = DisplayUtil.dpToPx(GestureOverlayService.this, 40);
                            
                            // 计算当前所在的区域
                            int sectionIndex = calculateSectionIndex(mStartX);
                            
                            if (deltaY > triggerThreshold) {
                                // 满足触发条件，显示图标
                                if (!mGestureTriggered) {
                                    mGestureTriggered = true;
                                    mCurrentSectionIndex = sectionIndex;
                                    showFeedbackIcon(sectionIndex);
                                }
                            } else {
                                // 不满足触发条件，隐藏图标
                                if (mGestureTriggered) {
                                    mGestureTriggered = false;
                                    hideFeedbackIconReverse();
                                }
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            float finalDeltaY = mStartY - event.getY();
                            
                            // 判断是否为向上滑动超过阈值
                            if (finalDeltaY > DisplayUtil.dpToPx(GestureOverlayService.this, 40)) {
                                // 触发功能，图标缩放消失
                                if (mGestureTriggered) {
                                    hideFeedbackIconWithScale();
                                }
                                handleGesture(mStartX);
                            } else {
                                // 未触发，图标反向消失
                                if (mGestureTriggered) {
                                    hideFeedbackIconReverse();
                                }
                            }
                            
                            mGestureTriggered = false;
                            mCurrentSectionIndex = -1;
                            return true;
                    }
                    return false;
                }
            });
            
            // 添加悬浮窗到WindowManager
            mWindowManager.addView(mOverlayView, mParams);
            
            // 加载功能区域
            reloadFeatureViews();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "创建悬浮窗失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    /**
     * 更新悬浮窗高度
     */
    private void updateOverlayHeight() {
        if (mWindowManager != null && mOverlayView != null && mParams != null) {
            mParams.height = DisplayUtil.dpToPx(this, mOverlayHeight);
            mWindowManager.updateViewLayout(mOverlayView, mParams);
        }
    }

    /**
     * 重新加载功能视图 - 根据启用的功能数量动态计算每个功能的宽度
     */
    private void reloadFeatureViews() {
        if (mOverlayView == null) return;
        
        // 清空现有视图
        mOverlayView.removeAllViews();
        
        // 筛选出已启用的功能
        List<FeatureItem> enabledFeatures = new ArrayList<>();
        if (mFeatureItems != null) {
            for (FeatureItem item : mFeatureItems) {
                if (item.isEnabled()) {
                    enabledFeatures.add(item);
                }
            }
        }
        
        // 如果没有启用的功能，隐藏整个悬浮窗
        if (enabledFeatures.isEmpty()) {
            mOverlayView.setVisibility(View.GONE);
            return;
        }
        
        // 显示悬浮窗
        mOverlayView.setVisibility(View.VISIBLE);
        
        // 获取屏幕宽度
        int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        int featureCount = enabledFeatures.size();
        // 计算每个功能的宽度：屏幕宽度 / 当前已开启的功能数量
        int featureWidth = screenWidth / featureCount;
        
        // 为每个启用的功能创建一个视图，直接设置精确的像素宽度
        for (int i = 0; i < enabledFeatures.size(); i++) {
            FeatureItem item = enabledFeatures.get(i);
            View featureView = new View(mOverlayView.getContext());
            
            // 直接使用像素宽度，而不是权重
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    featureWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            featureView.setLayoutParams(params);
            
            // 设置背景色（预览模式下显示，正常模式下透明）
            if (mIsPreviewMode) {
                featureView.setBackgroundColor(getResources().getColor(item.getColorResId()));
                featureView.setAlpha(1.0f); // 完全不透明
            } else {
                featureView.setBackgroundColor(Color.TRANSPARENT);
            }
            
            mOverlayView.addView(featureView);
        }
    }
    
    /**
     * 更新预览模式
     */
    private void updatePreviewMode() {
        if (mOverlayView == null) return;
        
        // 筛选出已启用的功能
        List<FeatureItem> enabledFeatures = new ArrayList<>();
        if (mFeatureItems != null) {
            for (FeatureItem item : mFeatureItems) {
                if (item.isEnabled()) {
                    enabledFeatures.add(item);
                }
            }
        }
        
        // 如果没有启用的功能，隐藏悬浮窗
        if (enabledFeatures.isEmpty()) {
            mOverlayView.setVisibility(View.GONE);
            return;
        }
        
        // 显示悬浮窗
        mOverlayView.setVisibility(View.VISIBLE);
        
        // 更新每个子视图的颜色
        int childCount = mOverlayView.getChildCount();
        for (int i = 0; i < childCount && i < enabledFeatures.size(); i++) {
            View child = mOverlayView.getChildAt(i);
            if (mIsPreviewMode) {
                child.setBackgroundColor(getResources().getColor(enabledFeatures.get(i).getColorResId()));
                child.setAlpha(1.0f); // 完全不透明
            } else {
                child.setBackgroundColor(Color.TRANSPARENT);
                child.setAlpha(1.0f);
            }
        }
    }

    /**
     * 处理手势操作，触发相应的截图功能
     * @param touchX 触摸的X坐标
     */
    private void handleGesture(float touchX) {
        Log.d(TAG, "========== 处理手势操作 ==========");
        Log.d(TAG, "触摸位置 X = " + touchX);
        
        // 筛选出已启用的功能
        List<FeatureItem> enabledFeatures = new ArrayList<>();
        if (mFeatureItems != null) {
            for (FeatureItem item : mFeatureItems) {
                if (item.isEnabled()) {
                    enabledFeatures.add(item);
                }
            }
        }
        
        if (enabledFeatures.isEmpty()) {
            Log.e(TAG, "没有启用的功能！");
            return;
        }
        
        // 根据触摸位置计算是哪个功能区域
        // 使用屏幕宽度确保与颜色条位置一致
        int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        float sectionWidth = (float) screenWidth / enabledFeatures.size();
        int sectionIndex = (int) (touchX / sectionWidth);
        
        // 确保索引在有效范围内
        if (sectionIndex < 0) sectionIndex = 0;
        if (sectionIndex >= enabledFeatures.size()) sectionIndex = enabledFeatures.size() - 1;
        
        FeatureItem selectedFeature = enabledFeatures.get(sectionIndex);
        Log.d(TAG, "选中功能索引 = " + sectionIndex + ", 类型 = " + selectedFeature.getType());
        
        // 只有截屏功能且开启音效才播放
        if (selectedFeature.getType() != Constants.FEATURE_HOME && PreferenceUtil.getSoundEffectEnabled(this)) {
            playScreenshotSound();
        }

        // 不再显示底部色带的视觉反馈，只有小图标动画

        // 发送广播到CaptureService
        String action = getActionForFeatureType(selectedFeature.getType());
        Log.d(TAG, "准备发送广播，Action = " + action);
        
        // 检查 CaptureService 是否在运行
        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled(this);
        Log.d(TAG, "无障碍服务状态: " + (isAccessibilityEnabled ? "已开启" : "未开启"));
        if (!isAccessibilityEnabled) {
            Log.e(TAG, "警告：无障碍服务未开启，广播可能无法被接收！");
            Toast.makeText(this, "请先开启无障碍服务！", Toast.LENGTH_SHORT).show();
            return;  // 直接返回，不发送广播
        }
        
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setPackage(getPackageName());  // 明确指定包名，确保广播发送到本应用
        sendBroadcast(intent);
        Log.d(TAG, "广播已发送到包: " + getPackageName());
    }
    
    /**
     * 根据功能类型获取对应的广播Action
     */
    private String getActionForFeatureType(int type) {
        switch (type) {
            case Constants.FEATURE_MAIN:
                return Constants.ACTION_CAPTURE_MAIN;
            case Constants.FEATURE_SUB:
                return Constants.ACTION_CAPTURE_SUB;
            case Constants.FEATURE_BOTH:
                return Constants.ACTION_CAPTURE_BOTH;
            case Constants.FEATURE_HOME:
                return Constants.ACTION_GO_HOME;
            default:
                return Constants.ACTION_CAPTURE_MAIN;
        }
    }

    /**
     * 显示视觉反馈
     */
    private void showVisualFeedback(int sectionIndex, FeatureItem feature) {
        if (mOverlayView == null || sectionIndex >= mOverlayView.getChildCount()) {
            return;
        }
        
        final View targetView = mOverlayView.getChildAt(sectionIndex);
        final int originalColor = mIsPreviewMode ? 
                getResources().getColor(feature.getColorResId()) : Color.TRANSPARENT;
        
        // 短暂显示高亮颜色
        targetView.setBackgroundColor(getResources().getColor(feature.getColorResId()));
        targetView.setAlpha(1.0f);
        
        // 300ms后恢复原始状态
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (targetView != null) {
                    targetView.setBackgroundColor(originalColor);
                    targetView.setAlpha(1.0f); // 保持完全不透明
                }
            }
        }, 300);
    }

    /**
     * 计算触摸点所在的区域索引
     */
    private int calculateSectionIndex(float touchX) {
        // 筛选出已启用的功能
        List<FeatureItem> enabledFeatures = new ArrayList<>();
        if (mFeatureItems != null) {
            for (FeatureItem item : mFeatureItems) {
                if (item.isEnabled()) {
                    enabledFeatures.add(item);
                }
            }
        }
        
        if (enabledFeatures.isEmpty()) {
            return -1;
        }
        
        // 根据触摸位置计算是哪个功能区域
        // 使用屏幕宽度确保与颜色条位置一致
        int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        float sectionWidth = (float) screenWidth / enabledFeatures.size();
        int sectionIndex = (int) (touchX / sectionWidth);
        
        // 确保索引在有效范围内
        if (sectionIndex < 0) sectionIndex = 0;
        if (sectionIndex >= enabledFeatures.size()) sectionIndex = enabledFeatures.size() - 1;
        
        return sectionIndex;
    }

    /**
     * 显示手势反馈图标 - 带动画
     */
    private void showFeedbackIcon(int sectionIndex) {
        // 取消当前动画
        if (mCurrentAnimation != null && mCurrentAnimation.isRunning()) {
            mCurrentAnimation.cancel();
        }

        // 筛选出已启用的功能
        List<FeatureItem> enabledFeatures = new ArrayList<>();
        if (mFeatureItems != null) {
            for (FeatureItem item : mFeatureItems) {
                if (item.isEnabled()) {
                    enabledFeatures.add(item);
                }
            }
        }

        if (enabledFeatures.isEmpty() || sectionIndex < 0 || sectionIndex >= enabledFeatures.size()) {
            return;
        }

        FeatureItem selectedFeature = enabledFeatures.get(sectionIndex);

        // 创建图标（如果不存在）
        if (mFeedbackIcon == null) {
            createFeedbackIcon();
        }

        // 设置图标资源
        int iconResId = getIconResourceForFeature(selectedFeature.getType());
        mFeedbackIcon.setImageResource(iconResId);
        // 确保图标tint为不透明，避免默认tint影响透明度
        mFeedbackIcon.setSupportImageTintList(null);
        mFeedbackIcon.setSupportImageTintMode(null);

        // 计算图标位置：在对应区域的上方居中
        // 使用屏幕宽度确保与颜色条位置一致
        int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        float sectionWidth = (float) screenWidth / enabledFeatures.size();
        float sectionCenterX = (sectionIndex + 0.5f) * sectionWidth;

        // 图标窗口大小80dp（包含padding），图标实际大小48dp
        int windowSize = DisplayUtil.dpToPx(this, 80);

        // X坐标：让窗口中心对齐区域中心
        mIconParams.x = (int) (sectionCenterX - windowSize / 2);
        // Y坐标：距离屏幕底部4dp
        mIconParams.y = DisplayUtil.dpToPx(this, 4);
        
        // 如果图标还没添加到窗口，添加它
        if (!mIconShowing) {
            try {
                mWindowManager.addView(mFeedbackIcon, mIconParams);
                mIconShowing = true;
                Log.d(TAG, "图标已添加到窗口");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            // 更新位置
            mWindowManager.updateViewLayout(mFeedbackIcon, mIconParams);
            Log.d(TAG, "更新图标位置");
        }
        
        // 设置初始状态
        Log.d(TAG, "显示图标前 alpha = " + mFeedbackIcon.getAlpha());
        mFeedbackIcon.setAlpha(0f);
        mFeedbackIcon.setTranslationY(DisplayUtil.dpToPx(this, 30)); // 从下方30dp开始
        mFeedbackIcon.setScaleX(1f);
        mFeedbackIcon.setScaleY(1f);
        mFeedbackIcon.setVisibility(View.VISIBLE);
        
        // 创建出现动画：alpha 0->1, translationY 30->0, 贝塞尔曲线（减速）
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mFeedbackIcon, "alpha", 0f, 1f);
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFeedbackIcon, "translationY", DisplayUtil.dpToPx(this, 30), 0f);
        
        mCurrentAnimation = new AnimatorSet();
        mCurrentAnimation.playTogether(alphaAnimator, translateAnimator);
        mCurrentAnimation.setDuration(250); // 250ms
        mCurrentAnimation.setInterpolator(new DecelerateInterpolator(2.0f)); // 贝塞尔曲线，从快到慢
        
        // 添加动画更新监听器，输出透明度变化
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentAlpha = (float) animation.getAnimatedValue();
                Log.d(TAG, "动画执行中 - 当前 alpha: " + currentAlpha);
            }
        });
        
        mCurrentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画结束后显式设置完全不透明，确保没有半透明残留
                mFeedbackIcon.setAlpha(1.0f);
                Log.d(TAG, "动画结束 - 设置 alpha 为 1.0f，当前 alpha: " + mFeedbackIcon.getAlpha());
            }
        });
        
        mCurrentAnimation.start();
        Log.d(TAG, "开始播放出现动画，目标 alpha = 1.0");
    }

    /**
     * 隐藏反馈图标 - 缩放消失（松手触发）
     */
    private void hideFeedbackIconWithScale() {
        if (mFeedbackIcon == null || !mIconShowing) {
            return;
        }
        
        // 取消当前动画
        if (mCurrentAnimation != null && mCurrentAnimation.isRunning()) {
            mCurrentAnimation.cancel();
        }
        
        // 创建缩放+消失动画：scale 1->1.2->1, alpha 1->0
        ObjectAnimator scaleXAnimator1 = ObjectAnimator.ofFloat(mFeedbackIcon, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleYAnimator1 = ObjectAnimator.ofFloat(mFeedbackIcon, "scaleY", 1f, 1.2f);
        ObjectAnimator scaleXAnimator2 = ObjectAnimator.ofFloat(mFeedbackIcon, "scaleX", 1.2f, 1f);
        ObjectAnimator scaleYAnimator2 = ObjectAnimator.ofFloat(mFeedbackIcon, "scaleY", 1.2f, 1f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mFeedbackIcon, "alpha", 1f, 0f);
        
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleXAnimator1, scaleYAnimator1);
        scaleUp.setDuration(100); // 快速放大
        
        AnimatorSet scaleDownAndFade = new AnimatorSet();
        scaleDownAndFade.playTogether(scaleXAnimator2, scaleYAnimator2, alphaAnimator);
        scaleDownAndFade.setDuration(200); // 缩小并消失
        
        mCurrentAnimation = new AnimatorSet();
        mCurrentAnimation.playSequentially(scaleUp, scaleDownAndFade);
        mCurrentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeFeedbackIcon();
            }
        });
        mCurrentAnimation.start();
    }

    /**
     * 隐藏反馈图标 - 反向动画（下滑取消触发）
     */
    private void hideFeedbackIconReverse() {
        if (mFeedbackIcon == null || !mIconShowing) {
            return;
        }
        
        // 取消当前动画
        if (mCurrentAnimation != null && mCurrentAnimation.isRunning()) {
            mCurrentAnimation.cancel();
        }
        
        // 创建反向动画：alpha 1->0, translationY 0->30, 加速
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mFeedbackIcon, "alpha", 1f, 0f);
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFeedbackIcon, "translationY", 
                0f, DisplayUtil.dpToPx(this, 30));
        
        mCurrentAnimation = new AnimatorSet();
        mCurrentAnimation.playTogether(alphaAnimator, translateAnimator);
        mCurrentAnimation.setDuration(200); // 200ms
        mCurrentAnimation.setInterpolator(new AccelerateInterpolator(1.5f)); // 加速
        mCurrentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeFeedbackIcon();
            }
        });
        mCurrentAnimation.start();
    }

    /**
     * 创建反馈图标
     */
    private void createFeedbackIcon() {
        Log.d(TAG, "========== 创建反馈图标 ==========");
        Context displayContext = mOverlayView.getContext();
        // 使用AppCompatImageView确保vector drawable正确加载，与设置页面一致
        mFeedbackIcon = new androidx.appcompat.widget.AppCompatImageView(displayContext);
        
        // 明确设置背景为透明，确保没有背景色影响透明度
        mFeedbackIcon.setBackgroundColor(Color.TRANSPARENT);
        
        // 图标实际大小48dp，但窗口需要更大以容纳缩放动画和位移，防止裁切（扩大到80dp）
        int iconSize = DisplayUtil.dpToPx(this, 48);
        int windowSize = DisplayUtil.dpToPx(this, 80); // 扩大窗口大小，确保动画时不裁切
        
        mIconParams = new WindowManager.LayoutParams(
                windowSize,
                windowSize,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // 允许超出屏幕边界，防止裁切
                PixelFormat.RGBA_8888);  // 强制使用 32 位 ARGB 格式，确保完全不透明
        
        Log.d(TAG, "WindowParams format: " + mIconParams.format);
        
        mIconParams.gravity = Gravity.BOTTOM | Gravity.START;
        mIconParams.x = 0;
        mIconParams.y = 0;
        
        // *** 关键修复：设置窗口 alpha 为完全不透明 ***
        mIconParams.alpha = 1.0f;
        Log.d(TAG, "设置窗口 alpha = " + mIconParams.alpha);
        
        mFeedbackIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // 设置完全不透明 - 这是关键！
        mFeedbackIcon.setAlpha(1.0f);
        Log.d(TAG, "设置图标 alpha = 1.0f，当前 alpha = " + mFeedbackIcon.getAlpha());
        
        // 设置padding确保图标居中显示在窗口中
        int padding = (windowSize - iconSize) / 2;
        mFeedbackIcon.setPadding(padding, padding, padding, padding);
        
        // 禁用硬件加速，确保透明度设置正确生效
        mFeedbackIcon.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        Log.d(TAG, "图标创建完成，alpha = " + mFeedbackIcon.getAlpha());
    }

    /**
     * 移除反馈图标
     */
    private void removeFeedbackIcon() {
        if (mFeedbackIcon != null && mIconShowing) {
            try {
                mFeedbackIcon.setVisibility(View.GONE);
                mWindowManager.removeView(mFeedbackIcon);
                mIconShowing = false;
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }

    /**
     * 根据功能类型获取图标资源ID
     */
    private int getIconResourceForFeature(int featureType) {
        switch (featureType) {
            case Constants.FEATURE_MAIN:
                return R.drawable.ic_feature_main;
            case Constants.FEATURE_SUB:
                return R.drawable.ic_feature_sub;
            case Constants.FEATURE_BOTH:
                return R.drawable.ic_feature_both;
            case Constants.FEATURE_HOME:
                return R.drawable.ic_feature_home;
            default:
                return R.drawable.ic_feature_main;
        }
    }

    /**
     * 播放截屏音效
     */
    private void playScreenshotSound() {
        try {
            // 如果MediaPlayer已存在，先释放
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            
            // 创建MediaPlayer并播放音效
            mMediaPlayer = MediaPlayer.create(this, R.raw.screenshot_sound);
            if (mMediaPlayer != null) {
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 播放完成后释放资源
                        mp.release();
                        mMediaPlayer = null;
                    }
                });
                mMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 播放音效失败不影响截图功能
        }
    }

    /**
     * 检查无障碍服务是否已启用（静态方法版本）
     */
    private static boolean isAccessibilityServiceEnabled(Context context) {
        String packageName = context.getPackageName();
        String expectedServiceName1 = packageName + "/" + "com.screencap.assistant.CaptureService";
        String expectedServiceName2 = packageName + "/.CaptureService";
        
        Log.d("GestureOverlay_DEBUG", "检查无障碍服务");
        Log.d("GestureOverlay_DEBUG", "期望服务名1: " + expectedServiceName1);
        Log.d("GestureOverlay_DEBUG", "期望服务名2: " + expectedServiceName2);
        
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServicesList = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            Log.d("GestureOverlay_DEBUG", "已启用服务列表: " + enabledServicesList);
            
            for (AccessibilityServiceInfo serviceInfo : enabledServicesList) {
                String serviceId = serviceInfo.getId();
                if (serviceId != null) {
                    Log.d("GestureOverlay_DEBUG", "检查服务ID: " + serviceId);
                    
                    if (serviceId.equals(expectedServiceName1) || serviceId.equals(expectedServiceName2)) {
                        Log.d("GestureOverlay_DEBUG", "无障碍服务已启用");
                        return true;
                    }
                }
            }
        }
        
        Log.d("GestureOverlay_DEBUG", "无障碍服务未启用");
        return false;
    }

}