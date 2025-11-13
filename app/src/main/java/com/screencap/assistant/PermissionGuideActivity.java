package com.screencap.assistant;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限引导页，展示并引导用户开启所需权限
 */
public class PermissionGuideActivity extends BaseActivity {

    private static final String TAG = "PermissionGuideActivity";
    
    private RecyclerView mPermissionList;
    private PermissionAdapter mAdapter;
    private List<PermissionItem> mPermissionItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 检查是否需要从任务管理器中隐藏
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        if (hideFromRecents) {
            // 添加从最近任务中隐藏的标志
            setIntent(getIntent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_guide);

        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 更新权限状态显示
        updatePermissionStatus();
        
        // 如果所有权限已授予，跳转到设置页面
        if (hasAllPermissions()) {
            navigateToSettings();
        }
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
     * 跳转到设置页面
     */
    private void navigateToSettings() {
        // 首次授权完成，默认开启服务
        if (!PreferenceUtil.hasSetServiceEnabled(this)) {
            PreferenceUtil.saveServiceEnabled(this, true);
        }
        
        // 如果服务已开启，启动悬浮窗服务
        if (PreferenceUtil.getServiceEnabled(this)) {
            Intent serviceIntent = new Intent(this, GestureOverlayService.class);
            startService(serviceIntent);
        }
        
        // 跳转到设置页面
        Intent intent = new Intent(this, SettingsActivity.class);
        // 检查是否需要从任务管理器中隐藏
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        if (hideFromRecents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        startActivity(intent);
        finish();
    }

    private void initView() {
        mPermissionList = findViewById(R.id.permission_list);
        mPermissionList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initData() {
        mPermissionItems = new ArrayList<>();
        
        // 添加悬浮窗权限
        mPermissionItems.add(new PermissionItem(
                "悬浮窗权限",
                "用于在副屏底部创建手势监听区域",
                PermissionType.OVERLAY
        ));
        
        // 添加无障碍服务权限
        mPermissionItems.add(new PermissionItem(
                "无障碍服务",
                "用于执行截图操作",
                PermissionType.ACCESSIBILITY
        ));
        
        mAdapter = new PermissionAdapter(mPermissionItems);
        mPermissionList.setAdapter(mAdapter);
    }

    /**
     * 更新权限状态
     */
    private void updatePermissionStatus() {
        Log.d(TAG, "Updating permission status in UI");
        for (PermissionItem item : mPermissionItems) {
            boolean previousState = item.isGranted;
            if (item.type == PermissionType.OVERLAY) {
                item.isGranted = hasOverlayPermission();
            } else if (item.type == PermissionType.ACCESSIBILITY) {
                item.isGranted = isAccessibilityServiceEnabled();
            }
            
            if (previousState != item.isGranted) {
                Log.d(TAG, item.title + " permission changed: " + previousState + " -> " + item.isGranted);
            }
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 权限类型枚举
     */
    private enum PermissionType {
        OVERLAY,       // 悬浮窗权限
        ACCESSIBILITY  // 无障碍服务权限
    }

    /**
     * 权限项数据类
     */
    private class PermissionItem {
        String title;
        String description;
        PermissionType type;
        boolean isGranted;

        PermissionItem(String title, String description, PermissionType type) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.isGranted = false;
        }
    }

    /**
     * 权限列表适配器
     */
    private class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.ViewHolder> {

        private List<PermissionItem> mItems;

        PermissionAdapter(List<PermissionItem> items) {
            this.mItems = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_permission, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PermissionItem item = mItems.get(position);
            holder.title.setText(item.title);
            holder.description.setText(item.description);
            holder.status.setText(item.isGranted ? "已授予" : "未授予");
            holder.status.setTextColor(item.isGranted 
                    ? getResources().getColor(R.color.teal_700) 
                    : getResources().getColor(R.color.secondary_text));

            // 点击项跳转到对应的权限设置
            holder.itemView.setOnClickListener(v -> {
                if (item.type == PermissionType.OVERLAY) {
                    requestOverlayPermission();
                } else if (item.type == PermissionType.ACCESSIBILITY) {
                    requestAccessibilityPermission();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView description;
            TextView status;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.permission_title);
                description = itemView.findViewById(R.id.permission_description);
                status = itemView.findViewById(R.id.permission_status);
            }
        }
    }
}