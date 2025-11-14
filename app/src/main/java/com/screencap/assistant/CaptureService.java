package com.screencap.assistant;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 无障碍截图服务
 */
public class CaptureService extends AccessibilityService {

    private static final String TAG = "CaptureService";
    private static final String CHANNEL_ID = "screenshot_service";
    private boolean mIsServiceReady = false;
    
    private final BroadcastReceiver mCaptureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "========== 收到广播 ==========");
            String action = intent.getAction();
            Log.d(TAG, "广播 Action = " + action);
            if (action == null) {
                Log.e(TAG, "Action 为 null！");
                return;
            }
            
            if (Constants.ACTION_CAPTURE_MAIN.equals(action)) {
                Log.d(TAG, "触发主屏截图");
                captureScreenshot(Constants.FEATURE_MAIN);
            } else if (Constants.ACTION_CAPTURE_SUB.equals(action)) {
                Log.d(TAG, "触发副屏截图");
                captureScreenshot(Constants.FEATURE_SUB);
            } else if (Constants.ACTION_CAPTURE_BOTH.equals(action)) {
                Log.d(TAG, "触发双屏截图");
                captureScreenshot(Constants.FEATURE_BOTH);
            } else if (Constants.ACTION_GO_HOME.equals(action)) {
                Log.d(TAG, "触发副屏回到桌面");
                goToHomeScreenOnSecondaryDisplay();
            } else {
                Log.e(TAG, "未知的 Action: " + action);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========== CaptureService onCreate ==========");
        Log.d(TAG, "Android SDK 版本: " + Build.VERSION.SDK_INT);
        createNotificationChannel();
        startForeground(1, createNotification());
        
        // 注册广播接收器
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(Constants.ACTION_CAPTURE_MAIN);
        filter.addAction(Constants.ACTION_CAPTURE_SUB);
        filter.addAction(Constants.ACTION_CAPTURE_BOTH);
        filter.addAction(Constants.ACTION_GO_HOME);
        
        // Android 14+ (API 34+) 需要指定 RECEIVER_NOT_EXPORTED 标志
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "使用 RECEIVER_NOT_EXPORTED 注册广播接收器");
            registerReceiver(mCaptureReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            Log.d(TAG, "使用普通方式注册广播接收器");
            registerReceiver(mCaptureReceiver, filter);
        }
        Log.d(TAG, "广播接收器注册成功，监听: " + Constants.ACTION_CAPTURE_MAIN + ", " + 
                Constants.ACTION_CAPTURE_SUB + ", " + Constants.ACTION_CAPTURE_BOTH);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mCaptureReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 无障碍服务事件处理
    }

    @Override
    public void onInterrupt() {
        // 服务中断处理
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mIsServiceReady = true;
        Log.i(TAG, "无障碍服务已连接并准备就绪");
        
        // 发送广播通知应用无障碍服务已连接
        Intent broadcastIntent = new Intent("com.dualscreen.ACCESSIBILITY_SERVICE_CONNECTED");
        sendBroadcast(broadcastIntent);
        Log.d(TAG, "Sent accessibility service connected broadcast");
    }

    /**
     * 执行副屏回到桌面操作
     */
    private void goToHomeScreenOnSecondaryDisplay() {
        int secondaryDisplayId = DisplayUtil.getSecondaryDisplayId(this);
        if (secondaryDisplayId == -1) {
            showNotification("未检测到副屏", null);
            return;
        }

        // 获取回到桌面的Intent
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        homeIntent.setComponent(new ComponentName("com.android.launcher3", "com.android.launcher3.secondarydisplay.SecondaryDisplayLauncher"));

        // 设置启动到副屏
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(secondaryDisplayId);

        // 执行回到桌面操作
        try {
            startActivity(homeIntent, options.toBundle());
            showNotification("副屏已回到桌面", null);
        } catch (Exception e) {
            Log.e(TAG, "回到桌面失败: " + e.getMessage(), e);
            showNotification("回到桌面失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行截图操作
     */
    private void captureScreenshot(int type) {
        Log.d(TAG, "========== 开始执行截图 ==========");
        Log.d(TAG, "截图类型: " + type);
        Log.d(TAG, "服务就绪状态: " + mIsServiceReady);
        
        if (!mIsServiceReady) {
            Log.e(TAG, "截图服务未就绪！");
            showNotification("截图服务未就绪", null);
            return;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(TAG, "Android 版本过低，需要 Android 11+");
            showNotification("当前Android版本不支持截图功能，需要Android 11+", null);
            return;
        }
        
        Log.d(TAG, "创建截图线程...");
        new Thread(() -> {
            try {
                switch (type) {
                    case Constants.FEATURE_MAIN:
                        Log.d(TAG, "执行主屏截图");
                        captureMainScreen();
                        break;
                    case Constants.FEATURE_SUB:
                        Log.d(TAG, "执行副屏截图");
                        captureSubScreen();
                        break;
                    case Constants.FEATURE_BOTH:
                        Log.d(TAG, "执行双屏截图");
                        captureBothScreens();
                        break;
                    default:
                        Log.e(TAG, "未知的截图类型: " + type);
                }
            } catch (Exception e) {
                Log.e(TAG, "截图失败: " + e.getMessage(), e);
                showNotification("截图失败: " + e.getMessage(), null);
            }
        }).start();
    }

    /**
     * 截取主屏
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void captureMainScreen() throws Exception {
        Log.i(TAG, "开始截取主屏");
        Bitmap mainScreenBitmap = takeScreenshotOfDisplay(Display.DEFAULT_DISPLAY);
        if (mainScreenBitmap != null) {
            File savedFile = saveBitmap(mainScreenBitmap, "main");
            showNotification("主屏已截取", savedFile);
        } else {
            showNotification("主屏截取失败", null);
        }
    }

    /**
     * 截取副屏
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void captureSubScreen() throws Exception {
        Log.i(TAG, "开始截取副屏");
        int secondaryDisplayId = DisplayUtil.getSecondaryDisplayId(this);
        if (secondaryDisplayId == -1) {
            showNotification("未检测到副屏", null);
            return;
        }
        
        Log.i(TAG, "副屏Display ID: " + secondaryDisplayId);
        Bitmap subScreenBitmap = takeScreenshotOfDisplay(secondaryDisplayId);
        if (subScreenBitmap != null) {
            File savedFile = saveBitmap(subScreenBitmap, "sub");
            showNotification("副屏已截取", savedFile);
        } else {
            showNotification("副屏截取失败", null);
        }
    }

    /**
     * 同时截取两个屏幕
     * 由于截图API连续调用有时间限制,需要在两次截图之间添加延迟
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void captureBothScreens() throws Exception {
        Log.i(TAG, "开始同时截取双屏");
        
        // 第一步: 截取主屏
        Log.i(TAG, "步骤1: 截取主屏 (Display ID: 0)");
        Bitmap mainScreenBitmap = takeScreenshotOfDisplay(Display.DEFAULT_DISPLAY);
        if (mainScreenBitmap == null) {
            showNotification("截取主屏失败", null);
            return;
        }
        Log.i(TAG, "主屏截取成功: " + mainScreenBitmap.getWidth() + "x" + mainScreenBitmap.getHeight());

        // 第二步: 等待配置的间隔时间,避免API调用过快
        int delayMs = PreferenceUtil.getScreenshotDelay(this);
        Log.i(TAG, "步骤2: 等待" + delayMs + "ms后截取副屏");
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Log.e(TAG, "延迟被中断: " + e.getMessage());
        }
        
        // 第三步: 获取副屏ID
        int secondaryDisplayId = DisplayUtil.getSecondaryDisplayId(this);
        Log.i(TAG, "副屏Display ID: " + secondaryDisplayId);
        
        if (secondaryDisplayId == -1) {
            File savedFile = saveBitmap(mainScreenBitmap, "main");
            showNotification("未检测到副屏，仅保存主屏截图", savedFile);
            return;
        }

        // 第四步: 截取副屏
        Log.i(TAG, "步骤3: 截取副屏 (Display ID: " + secondaryDisplayId + ")");
        Bitmap subScreenBitmap = takeScreenshotOfDisplay(secondaryDisplayId);
        if (subScreenBitmap == null) {
            Log.e(TAG, "副屏截图失败,仅保存主屏");
            File savedFile = saveBitmap(mainScreenBitmap, "main");
            showNotification("副屏截取失败，仅保存主屏截图", savedFile);
            return;
        }
        Log.i(TAG, "副屏截取成功: " + subScreenBitmap.getWidth() + "x" + subScreenBitmap.getHeight());

        // 第五步: 拼接两张截图
        Log.i(TAG, "步骤4: 开始拼接双屏截图");
        Bitmap combinedBitmap = null;
        boolean useFrame = PreferenceUtil.getEnableFrameScreenshot(this);
        
        if (useFrame) {
            combinedBitmap = combineBitmapsWithFrame(mainScreenBitmap, subScreenBitmap);
        } else {
            combinedBitmap = combineBitmapsVertically(mainScreenBitmap, subScreenBitmap);
        }
        
        if (combinedBitmap != null) {
            File savedFile;
            if (useFrame) {
                // 套壳截图使用带质量参数的保存方法
                savedFile = saveBitmapWithQuality(combinedBitmap, "both");
            } else {
                // 普通双屏截图使用默认PNG格式
                savedFile = saveBitmap(combinedBitmap, "both");
            }
            showNotification("双屏已截取", savedFile);
            Log.i(TAG, "双屏截图完成: " + combinedBitmap.getWidth() + "x" + combinedBitmap.getHeight());
            // 释放临时位图
            mainScreenBitmap.recycle();
            subScreenBitmap.recycle();
        } else {
            Log.e(TAG, "位图拼接失败");
            showNotification("拼接失败", null);
        }
    }

    /**
     * 使用AccessibilityService的takeScreenshot API截取指定Display
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private Bitmap takeScreenshotOfDisplay(int displayId) {
        final Bitmap[] resultBitmap = {null};
        final Object lock = new Object();
        
        try {
            // 使用AccessibilityService.takeScreenshot API
            takeScreenshot(displayId, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult screenshotResult) {
                    try {
                        resultBitmap[0] = Bitmap.wrapHardwareBuffer(
                                screenshotResult.getHardwareBuffer(),
                                screenshotResult.getColorSpace()
                        );
                        Log.i(TAG, "截图成功，Display ID: " + displayId);
                    } catch (Exception e) {
                        Log.e(TAG, "处理截图结果失败: " + e.getMessage(), e);
                    } finally {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    Log.e(TAG, "截图失败，错误码: " + errorCode);
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            });
            
            // 等待截图完成（最多等待5秒）
            synchronized (lock) {
                lock.wait(5000);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "截图异常: " + e.getMessage(), e);
        }
        
        return resultBitmap[0];
    }

    /**
     * 垂直合并两个位图 - 主屏在上，副屏在下，左右居中对齐
     */
    private Bitmap combineBitmapsVertically(Bitmap mainScreen, Bitmap subScreen) {
        try {
            Log.i(TAG, "开始拼接位图");
            Log.i(TAG, "主屏位图: " + mainScreen.getWidth() + "x" + mainScreen.getHeight() + 
                    ", Config: " + mainScreen.getConfig());
            Log.i(TAG, "副屏位图: " + subScreen.getWidth() + "x" + subScreen.getHeight() + 
                    ", Config: " + subScreen.getConfig());
            
            // 如果位图是HARDWARE格式,需要转换为ARGB_8888才能创建可变位图
            Bitmap mainBitmap = mainScreen;
            Bitmap subBitmap = subScreen;
            
            if (mainScreen.getConfig() == Bitmap.Config.HARDWARE) {
                Log.i(TAG, "主屏位图是HARDWARE格式,转换为ARGB_8888");
                mainBitmap = mainScreen.copy(Bitmap.Config.ARGB_8888, false);
            }
            
            if (subScreen.getConfig() == Bitmap.Config.HARDWARE) {
                Log.i(TAG, "副屏位图是HARDWARE格式,转换为ARGB_8888");
                subBitmap = subScreen.copy(Bitmap.Config.ARGB_8888, false);
            }
            
            // 计算合并后的位图尺寸
            int width = Math.max(mainBitmap.getWidth(), subBitmap.getWidth());
            int height = mainBitmap.getHeight() + subBitmap.getHeight();
            Log.i(TAG, "目标拼接尺寸: " + width + "x" + height);
            
            // 创建新的位图 - 使用ARGB_8888格式
            Bitmap combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            
            // 绘制两个位图到新位图上
            Canvas canvas = new Canvas(combined);
            
            // 主屏在上，居中对齐
            int mainX = (width - mainBitmap.getWidth()) / 2;
            canvas.drawBitmap(mainBitmap, mainX, 0, null);
            Log.i(TAG, "主屏已绘制到位置: (" + mainX + ", 0)");
            
            // 副屏在下，居中对齐
            int subX = (width - subBitmap.getWidth()) / 2;
            canvas.drawBitmap(subBitmap, subX, mainBitmap.getHeight(), null);
            Log.i(TAG, "副屏已绘制到位置: (" + subX + ", " + mainBitmap.getHeight() + ")");
            
            Log.i(TAG, "位图拼接成功: " + width + "x" + height);
            return combined;
        } catch (Exception e) {
            Log.e(TAG, "合并位图失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 套壳拼接两个位图 - 按照指定位置和尺寸组合
     */
    private Bitmap combineBitmapsWithFrame(Bitmap mainScreen, Bitmap subScreen) {
        try {
            Log.i(TAG, "开始套壳拼接位图");
            Log.i(TAG, "主屏位图: " + mainScreen.getWidth() + "x" + mainScreen.getHeight());
            Log.i(TAG, "副屏位图: " + subScreen.getWidth() + "x" + subScreen.getHeight());
            
            // 如果位图是HARDWARE格式,需要转换为ARGB_8888才能创建可变位图
            Bitmap mainBitmap = mainScreen;
            Bitmap subBitmap = subScreen;
            
            if (mainScreen.getConfig() == Bitmap.Config.HARDWARE) {
                mainBitmap = mainScreen.copy(Bitmap.Config.ARGB_8888, false);
            }
            
            if (subScreen.getConfig() == Bitmap.Config.HARDWARE) {
                subBitmap = subScreen.copy(Bitmap.Config.ARGB_8888, false);
            }
            
            // 创建2400x2900的空画布
            Bitmap combined = Bitmap.createBitmap(2400, 2900, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(combined);
            
            // 缩放主屏图像到1920x1080
            Bitmap scaledMainBitmap = Bitmap.createScaledBitmap(mainBitmap, 1920, 1080, true);
            // 绘制主屏图像 - 位置:x=240,y=180
            canvas.drawBitmap(scaledMainBitmap, 240, 180, null);
            Log.i(TAG, "主屏已缩放并绘制到位置: (240, 180)");
            scaledMainBitmap.recycle();
            
            // 缩放副屏图像到1090x950
            Bitmap scaledSubBitmap = Bitmap.createScaledBitmap(subBitmap, 1090, 950, true);
            canvas.drawBitmap(scaledSubBitmap, 655, 1538, null);
            Log.i(TAG, "副屏已缩放并绘制到位置: (655, 1538)");
            scaledSubBitmap.recycle();
            
            // 根据选择的机身颜色覆盖机身图片
            int colorIndex = PreferenceUtil.getFrameColorIndex(this);
            int frameResId = R.drawable.black; // 默认黑色
            
            switch (colorIndex) {
                case 0: // 黑色
                    frameResId = R.drawable.black;
                    break;
                case 1: // 白色
                    frameResId = R.drawable.white;
                    break;
                case 2: // 灰彩
                    frameResId = R.drawable.grey;
                    break;
                case 3: // 紫透
                    frameResId = R.drawable.purple;
                    break;
            }
            
            // 绘制机身图片
            // 图片已放在drawable-nodpi文件夹，不会被系统自动缩放
            Bitmap frameBitmap = android.graphics.BitmapFactory.decodeResource(getResources(), frameResId);
            
            // 确保机身图片尺寸正确（应该是2400x2900）
            Log.i(TAG, "机身图片加载尺寸: " + frameBitmap.getWidth() + "x" + frameBitmap.getHeight());
            if (frameBitmap.getWidth() == 2400 && frameBitmap.getHeight() == 2900) {
                canvas.drawBitmap(frameBitmap, 0, 0, null);
                Log.i(TAG, "机身图片已绘制: " + frameResId + " (原始尺寸: 2400x2900)");
            } else {
                Log.w(TAG, "机身图片尺寸不匹配: " + frameBitmap.getWidth() + "x" + frameBitmap.getHeight() + ", 期望: 2400x2900");
                // 如果尺寸不对，强制缩放到2400x2900
                Bitmap scaledFrameBitmap = Bitmap.createScaledBitmap(frameBitmap, 2400, 2900, true);
                canvas.drawBitmap(scaledFrameBitmap, 0, 0, null);
                scaledFrameBitmap.recycle();
            }
            frameBitmap.recycle();
            
            Log.i(TAG, "套壳拼接成功: 2400x2900");
            return combined;
        } catch (Exception e) {
            Log.e(TAG, "套壳拼接失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 保存位图到文件
     * @return 保存的文件对象,失败返回null
     */
    private File saveBitmap(Bitmap bitmap, String suffix) {
        File savedFile = null;
        try {
            // 使用 Pictures 目录下的自定义文件夹
            File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES);
            File directory = new File(picturesDir, Constants.SCREENSHOT_DIR);
            
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 生成文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "screenshot_" + suffix + "_" + timeStamp + ".png";
            File file = new File(directory, fileName);
            
            // 保存位图
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                
                Log.i(TAG, "截图已保存: " + file.getAbsolutePath());
                
                // 更新媒体库
                updateMediaLibrary(file);
                
                savedFile = file;
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "保存截图失败: " + e.getMessage(), e);
            showNotification("保存失败", null);
        } finally {
            // 释放位图资源
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        return savedFile;
    }
    
    /**
     * 保存套壳截图到文件,使用用户设置的图像质量
     * @return 保存的文件对象,失败返回null
     */
    private File saveBitmapWithQuality(Bitmap bitmap, String suffix) {
        File savedFile = null;
        try {
            // 使用 Pictures 目录下的自定义文件夹
            File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES);
            File directory = new File(picturesDir, Constants.SCREENSHOT_DIR);
            
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 获取用户设置的图像质量 (6-10)
            int imageQuality = PreferenceUtil.getFrameImageQuality(this);
            // 将6-10的范围映射到60-100的JPEG质量值
            int quality = imageQuality * 10;
            
            // 生成文件名 - 使用JPEG格式以支持质量调节
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "screenshot_" + suffix + "_" + timeStamp + ".jpg";
            File file = new File(directory, fileName);
            
            // 保存位图
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                // 使用JPEG格式,quality参数才有效(60-100)
                // PNG格式的quality参数无效,因为PNG是无损压缩
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
                
                Log.i(TAG, "套壳截图已保存: " + file.getAbsolutePath() + ", 格式: JPEG, 质量: " + quality);
                
                // 更新媒体库
                updateMediaLibrary(file);
                
                savedFile = file;
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "保存套壳截图失败: " + e.getMessage(), e);
            showNotification("保存失败", null);
        } finally {
            // 释放位图资源
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        return savedFile;
    }

    /**
     * 更新媒体库，使截图在相册中可见
     */
    private void updateMediaLibrary(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    /**
     * 创建前台服务通知通道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "双屏截图服务",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("用于执行双屏截图操作");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("双屏截图助手")
                    .setContentText("截图服务正在运行")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .build();
        } else {
            // 兼容旧版本
            return new Notification.Builder(this)
                    .setContentTitle("双屏截图助手")
                    .setContentText("截图服务正在运行")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .build();
        }
    }

    /**
     * 显示截图通知
     * @param message 提示消息
     * @param screenshotFile 截图文件(未使用,仅为保持接口一致)
     */
    private void showNotification(final String message, final File screenshotFile) {
        // 使用Handler确保在主线程显示Toast
        // 这是在后台服务显示Toast的关键!
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                    toast.show();
                    Log.i(TAG, "Toast显示: " + message);
                } catch (Exception e) {
                    Log.e(TAG, "Toast显示失败: " + e.getMessage(), e);
                }
            }
        });
    }
}
