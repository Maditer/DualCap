package com.screencap.assistant;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;

import com.screencap.assistant.CaptureService;
import java.util.List;

/**
 * 基础Activity类，用于统一处理权限检查
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        // 可以在子类中重写此方法来添加特定的权限检查逻辑
    }

    /**
     * 检查悬浮窗权限是否已授予
     */
    protected boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // Android 6.0 以下版本默认有权限
    }

    /**
     * 检查无障碍服务是否已启用
     * 使用现代AccessibilityManager方式更可靠
     */
    protected boolean isAccessibilityServiceEnabled() {
        final String TAG = "BaseActivity";
        
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            
            if (enabledServices != null && !enabledServices.isEmpty()) {
                String packageName = getPackageName();
                String expectedServiceName1 = packageName + "/" + CaptureService.class.getName();
                String expectedServiceName2 = packageName + "/." + CaptureService.class.getSimpleName();
                
                Log.d(TAG, "Checking accessibility service via AccessibilityManager, total services: " + enabledServices.size());
                Log.d(TAG, "Looking for full: " + expectedServiceName1);
                Log.d(TAG, "Looking for short: " + expectedServiceName2);
                
                for (AccessibilityServiceInfo service : enabledServices) {
                    String serviceId = service.getId();
                    if (serviceId != null) {
                        Log.d(TAG, "Found service: " + serviceId);
                        
                        if (serviceId.equals(expectedServiceName1) || serviceId.equals(expectedServiceName2)) {
                            Log.i(TAG, "✓ Accessibility service is enabled (AccessibilityManager)");
                            return true;
                        }
                    }
                }
            }
        }
        
        Log.w(TAG, "✗ Accessibility service NOT enabled");
        return false;
    }

    /**
     * 检查所有必需权限是否已授予
     */
    protected boolean hasAllPermissions() {
        return hasOverlayPermission() && isAccessibilityServiceEnabled();
    }

    /**
     * 启动悬浮窗权限设置界面
     */
    protected void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }
    }

    /**
     * 启动无障碍服务设置界面
     */
    protected void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}