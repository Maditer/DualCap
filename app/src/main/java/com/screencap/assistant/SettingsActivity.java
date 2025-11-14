package com.screencap.assistant;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 设置页面，用于配置应用的核心功能
 */
public class SettingsActivity extends BaseActivity {

    private SwitchCompat mServiceSwitch;
    private RecyclerView mFeatureList;
    private Slider mOverlayHeightSlider;
    private TextView mOverlayHeightText;
    private Slider mScreenshotDelaySlider;
    private TextView mScreenshotDelayText;
    private SwitchCompat mHideFromRecentsSwitch;
    private SwitchCompat mSoundEffectSwitch;
    private CardView mFrameScreenshotCard;
    private TextView mFrameScreenshotStatus;
    private FeatureAdapter mAdapter;
    private List<FeatureItem> mFeatureItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 检查是否需要从任务管理器中隐藏
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        if (hideFromRecents) {
            // 添加从最近任务中隐藏的标志
            setIntent(getIntent().addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        }
        
        super.onCreate(savedInstanceState);
        
        // 首次启动时检查权限
        if (!hasAllPermissions()) {
            // 权限缺失，跳转到权限引导页
            Intent intent = new Intent(this, PermissionGuideActivity.class);
            if (hideFromRecents) {
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            }
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_settings);

        initView();
        initData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 检查服务状态并更新UI
        boolean isEnabled = PreferenceUtil.getServiceEnabled(this);
        mServiceSwitch.setChecked(isEnabled);
        updateUIState(isEnabled);
        
        // 更新套壳截屏状态显示
        updateFrameScreenshotStatus();
        
        // 如果服务开启，通知悬浮窗服务进入预览模式
        if (isEnabled) {
            sendPreviewModeBroadcast(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 退出预览模式
        sendPreviewModeBroadcast(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 如果开启了在任务管理器中隐藏设置且是根activity，移除任务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && PreferenceUtil.getHideFromRecents(this)) {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果开启了在任务管理器中隐藏设置且是根activity，移除任务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && PreferenceUtil.getHideFromRecents(this)) {
            finishAndRemoveTask();
        }
    }

    private void initView() {
        mServiceSwitch = findViewById(R.id.service_switch);
        mFeatureList = findViewById(R.id.feature_list);
        mOverlayHeightSlider = findViewById(R.id.overlay_height_slider);
        mOverlayHeightText = findViewById(R.id.overlay_height_text);
        mScreenshotDelaySlider = findViewById(R.id.screenshot_delay_slider);
        mScreenshotDelayText = findViewById(R.id.screenshot_delay_text);
        mHideFromRecentsSwitch = findViewById(R.id.hide_from_recents_switch);
        mSoundEffectSwitch = findViewById(R.id.sound_effect_switch);
        mFrameScreenshotCard = findViewById(R.id.card_frame_screenshot);
        mFrameScreenshotStatus = findViewById(R.id.frame_screenshot_status);

        mFeatureList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initData() {
        // 从SharedPreferences加载功能列表，如果没有则创建默认列表
        mFeatureItems = PreferenceUtil.getFeatureList(this);
        if (mFeatureItems == null) {
            mFeatureItems = createDefaultFeatureList();
            PreferenceUtil.saveFeatureList(this, mFeatureItems);
        } else {
            // 检查是否已有"回到桌面"功能，如果没有则添加到列表末尾
            boolean hasHomeFeature = false;
            for (FeatureItem item : mFeatureItems) {
                if (item.getType() == Constants.FEATURE_HOME) {
                    hasHomeFeature = true;
                    break;
                }
            }
            if (!hasHomeFeature) {
                // 添加新功能到列表末尾，默认关闭
                mFeatureItems.add(new FeatureItem(String.valueOf(mFeatureItems.size() + 1), "回到桌面", Constants.FEATURE_HOME, false, R.color.capture_home));
                PreferenceUtil.saveFeatureList(this, mFeatureItems);
            }
        }

        mAdapter = new FeatureAdapter(mFeatureItems);
        mFeatureList.setAdapter(mAdapter);

        // 添加 Item 间距
        mFeatureList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view, 
                                      RecyclerView parent, RecyclerView.State state) {
                outRect.bottom = (int) (8 * getResources().getDisplayMetrics().density);
            }
        });

        // 设置拖拽排序
        setupItemTouchHelper();

        // 设置悬浮窗高度滑块 - 分段式，每格1dp
        int savedHeight = PreferenceUtil.getOverlayHeight(this);
        mOverlayHeightSlider.setValue(savedHeight);
        mOverlayHeightText.setText(String.format("%ddp", savedHeight));
        mOverlayHeightSlider.setValueFrom(Constants.MIN_OVERLAY_HEIGHT);
        mOverlayHeightSlider.setValueTo(Constants.MAX_OVERLAY_HEIGHT);
        mOverlayHeightSlider.setStepSize(1); // 每次调整1dp

        // 设置双屏截屏间隔滑块 - 最小值300ms，最大值350ms，每档1ms
        int savedDelay = PreferenceUtil.getScreenshotDelay(this);
        // 如果保存的值超出新范围，重置为中间值325ms
        if (savedDelay < 300 || savedDelay > 350) {
            savedDelay = 325;
            PreferenceUtil.saveScreenshotDelay(this, savedDelay);
        }
        mScreenshotDelaySlider.setValue(savedDelay);
        mScreenshotDelayText.setText(String.format("%dms", savedDelay));
        mScreenshotDelaySlider.setValueFrom(300);
        mScreenshotDelaySlider.setValueTo(350);
        mScreenshotDelaySlider.setStepSize(1); // 每次调整1ms

        // 设置在任务管理器中隐藏开关
        boolean hideFromRecents = PreferenceUtil.getHideFromRecents(this);
        mHideFromRecentsSwitch.setChecked(hideFromRecents);
        // 设置截屏音效开关
        boolean soundEffectEnabled = PreferenceUtil.getSoundEffectEnabled(this);
        mSoundEffectSwitch.setChecked(soundEffectEnabled);
    }

    private void setupListeners() {
        // 服务总开关监听
        mServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 保存状态
                PreferenceUtil.saveServiceEnabled(SettingsActivity.this, isChecked);
                
                // 更新UI状态
                updateUIState(isChecked);
                
                // 启动或停止手势悬浮窗服务
                Intent intent = new Intent(SettingsActivity.this, GestureOverlayService.class);
                if (isChecked) {
                    startService(intent);
                    // 延迟发送预览模式广播，确保服务已启动
                    mServiceSwitch.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendPreviewModeBroadcast(true);
                            sendReloadConfigBroadcast();
                        }
                    }, 200);
                } else {
                    stopService(intent);
                }
            }
        });

        // 悬浮窗高度滑块监听
        mOverlayHeightSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                int height = Math.round(value);
                mOverlayHeightText.setText(String.format("%ddp", height));
                
                // 保存高度
                PreferenceUtil.saveOverlayHeight(SettingsActivity.this, height);
                
                // 通知服务重载配置
                sendReloadConfigBroadcast();
            }
        });

        // 双屏截屏间隔滑块监听
        mScreenshotDelaySlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                int delay = Math.round(value);
                mScreenshotDelayText.setText(String.format("%dms", delay));
                
                // 保存间隔
                PreferenceUtil.saveScreenshotDelay(SettingsActivity.this, delay);
            }
        });

        // 在任务管理器中隐藏开关监听
        mHideFromRecentsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 保存状态
                PreferenceUtil.saveHideFromRecents(SettingsActivity.this, isChecked);
                
                // 重新打开应用以应用新的设置
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    if (isChecked) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    }
                    startActivity(intent);
                    finishAffinity(); // 关闭所有相关的Activity
                }
            }
        });
        // 截屏音效开关监听
        mSoundEffectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 保存状态
                PreferenceUtil.saveSoundEffectEnabled(SettingsActivity.this, isChecked);
            }
        });

        // 套壳截屏设置卡片点击事件
        mFrameScreenshotCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, FrameScreenshotSettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 更新UI状态，根据服务是否开启来启用或禁用UI元素
     */
    private void updateUIState(boolean isServiceEnabled) {
        // 控制RecyclerView中所有item的可用性
        if (mAdapter != null) {
            mFeatureList.setAlpha(isServiceEnabled ? 1.0f : 0.5f);
        }
        mOverlayHeightSlider.setEnabled(isServiceEnabled);
        mOverlayHeightSlider.setAlpha(isServiceEnabled ? 1.0f : 0.5f);
        mScreenshotDelaySlider.setEnabled(isServiceEnabled);
        mScreenshotDelaySlider.setAlpha(isServiceEnabled ? 1.0f : 0.5f);
    }



    /**
     * 创建默认的功能列表
     */
    private List<FeatureItem> createDefaultFeatureList() {
        List<FeatureItem> items = new ArrayList<>();
        items.add(new FeatureItem("1", "截主屏", Constants.FEATURE_MAIN, true, R.color.capture_main));
        items.add(new FeatureItem("2", "截副屏", Constants.FEATURE_SUB, true, R.color.capture_sub));
        items.add(new FeatureItem("3", "同时截屏", Constants.FEATURE_BOTH, true, R.color.capture_both));
        items.add(new FeatureItem("4", "回到桌面", Constants.FEATURE_HOME, false, R.color.capture_home));
        return items;
    }

    /**
     * 设置拖拽排序功能
     */
    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, 
                                  RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // 交换位置
                Collections.swap(mFeatureItems, fromPosition, toPosition);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                
                // 保存新的顺序
                PreferenceUtil.saveFeatureList(SettingsActivity.this, mFeatureItems);
                sendReloadConfigBroadcast();
                
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // 不需要滑动删除功能
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // 禁用长按拖拽，改用拖拽手柄
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.5f);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mFeatureList);
        
        // 将ItemTouchHelper传递给Adapter
        mAdapter.setItemTouchHelper(itemTouchHelper);
    }

    /**
     * 发送预览模式广播
     */
    private void sendPreviewModeBroadcast(boolean isPreviewEnabled) {
        Intent intent = new Intent(Constants.ACTION_PREVIEW_MODE);
        intent.putExtra(Constants.EXTRA_PREVIEW_ENABLED, isPreviewEnabled);
        sendBroadcast(intent);
    }

    /**
     * 发送重载配置广播
     */
    private void sendReloadConfigBroadcast() {
        Intent intent = new Intent(Constants.ACTION_RELOAD_CONFIG);
        sendBroadcast(intent);
    }

    /**
     * 更新套壳截屏状态显示
     */
    private void updateFrameScreenshotStatus() {
        boolean isEnabled = PreferenceUtil.getEnableFrameScreenshot(this);
        if (mFrameScreenshotStatus != null) {
            mFrameScreenshotStatus.setText(isEnabled ? "已开启" : "未开启");
        }
    }

    /**
     * 功能列表适配器
     */
    private class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {

        private List<FeatureItem> mItems;
        private ItemTouchHelper mItemTouchHelper;

        FeatureAdapter(List<FeatureItem> items) {
            this.mItems = items;
        }

        void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
            mItemTouchHelper = itemTouchHelper;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feature, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FeatureItem item = mItems.get(position);
            holder.featureName.setText(item.getName());
            holder.featureSwitch.setChecked(item.isEnabled());
            
            // 根据功能类型设置对应的图标
            int iconResId;
            switch (item.getType()) {
                case Constants.FEATURE_MAIN:
                    iconResId = R.drawable.ic_feature_main;
                    break;
                case Constants.FEATURE_SUB:
                    iconResId = R.drawable.ic_feature_sub;
                    break;
                case Constants.FEATURE_BOTH:
                    iconResId = R.drawable.ic_feature_both;
                    break;
                case Constants.FEATURE_HOME:
                    iconResId = R.drawable.ic_feature_home;
                    break;
                default:
                    iconResId = R.drawable.ic_feature_main;
            }
            holder.featureIcon.setImageResource(iconResId);

            // 移除之前的监听器，避免重复触发
            holder.featureSwitch.setOnCheckedChangeListener(null);
            holder.featureSwitch.setChecked(item.isEnabled());
            
            // 开关状态改变监听
            holder.featureSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setEnabled(isChecked);
                    PreferenceUtil.saveFeatureList(SettingsActivity.this, mItems);
                    // 发送重载配置广播，确保悬浮窗能及时更新功能区域宽度
                    sendReloadConfigBroadcast();
                }
            });

            // 设置拖拽手柄 - 按下时开始拖拽
            holder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mItemTouchHelper != null) {
                            mItemTouchHelper.startDrag(holder);
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView featureName;
            SwitchCompat featureSwitch;
            ImageView dragHandle;
            ImageView featureIcon;

            ViewHolder(View itemView) {
                super(itemView);
                featureName = itemView.findViewById(R.id.feature_name);
                featureSwitch = itemView.findViewById(R.id.feature_switch);
                dragHandle = itemView.findViewById(R.id.drag_handle);
                featureIcon = itemView.findViewById(R.id.feature_icon);
            }
        }
    }
}