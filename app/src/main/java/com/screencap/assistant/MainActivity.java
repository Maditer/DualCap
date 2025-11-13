package com.screencap.assistant;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

/**
 * 应用启动入口，负责权限检查和页面路由
 */
public class MainActivity extends BaseActivity {

    private static final int DELAY_TIME = 500; // 短暂延迟，让用户有机会看到启动页

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 检查是否需要从任务管理器中隐藏
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        if (hideFromRecents) {
            // 添加从最近任务中隐藏的标志
            setIntent(getIntent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 使用Handler延迟检查权限，避免页面快速切换导致的闪烁
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPermissionsAndRoute();
            }
        }, DELAY_TIME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 如果是根activity且开启了在任务管理器中隐藏设置，移除任务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isTaskRoot() && PreferenceUtil.getHideFromRecents(this)) {
            finishAndRemoveTask();
        }
    }

    /**
     * 检查权限并根据结果进行页面路由
     */
    private void checkPermissionsAndRoute() {
        // 检查是否需要从任务管理器中隐藏
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        
        if (!hasAllPermissions()) {
            // 权限缺失，跳转到权限引导页
            Intent intent = new Intent(MainActivity.this, PermissionGuideActivity.class);
            if (hideFromRecents) {
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            }
            startActivity(intent);
        } else {
            // 权限已授予，检查服务是否应该启动
            boolean isServiceEnabled = PreferenceUtil.getServiceEnabled(this);
            if (isServiceEnabled) {
                // 启动悬浮窗服务
                Intent serviceIntent = new Intent(MainActivity.this, GestureOverlayService.class);
                startService(serviceIntent);
            }
            
            // 跳转到设置页
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        // 完成后关闭当前Activity
        finish();
    }
}