package com.screencap.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class FrameScreenshotSettingsActivity extends AppCompatActivity {
    private SwitchCompat mEnableFrameSwitch;
    private MaterialCardView mColorSelectCard;
    private MaterialCardView mImageQualityCard;
    private MaterialTextView mColorSelectText;
    private MaterialTextView mImageQualityText;
    private Slider mImageQualitySlider;
    private List<String> mColorOptions;
    private int mSelectedColorIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_screenshot_settings);

        initViews();
        initData();
        setupActionBar();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        mEnableFrameSwitch = findViewById(R.id.enable_frame_switch);
        mColorSelectCard = findViewById(R.id.color_select_card);
        mImageQualityCard = findViewById(R.id.image_quality_card);
        mColorSelectText = findViewById(R.id.color_select_text);
        mImageQualityText = findViewById(R.id.image_quality_text);
        mImageQualitySlider = findViewById(R.id.image_quality_slider);
        
        // 设置 Toolbar 的返回按钮监听
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void initData() {
        // 初始化颜色选项
        mColorOptions = new ArrayList<>();
        mColorOptions.add(getString(R.string.frame_color_black));
        mColorOptions.add(getString(R.string.frame_color_white));
        mColorOptions.add(getString(R.string.frame_color_grey));
        mColorOptions.add(getString(R.string.frame_color_purple));
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.frame_screenshot_title);
        }
    }

    private void loadSettings() {
        // 加载使用套壳截屏开关状态
        boolean enableFrame = PreferenceUtil.getEnableFrameScreenshot(this);
        mEnableFrameSwitch.setChecked(enableFrame);
        updateColorSelectCardState(enableFrame);
        updateImageQualityCardState(enableFrame);

        // 加载机身颜色选择
        mSelectedColorIndex = PreferenceUtil.getFrameColorIndex(this);
        updateColorSelectText();
        
        // 加载图像质量设置
        int imageQuality = PreferenceUtil.getFrameImageQuality(this);
        mImageQualitySlider.setValue(imageQuality);
        updateImageQualityText(imageQuality);
    }

    private void setupListeners() {
        // 使用套壳截屏开关监听
        mEnableFrameSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceUtil.saveEnableFrameScreenshot(FrameScreenshotSettingsActivity.this, isChecked);
                updateColorSelectCardState(isChecked);
                updateImageQualityCardState(isChecked);
            }
        });

        // 机身颜色选择卡片点击监听
        mColorSelectCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorSelectDialog();
            }
        });
        
        // 图像质量滑块监听
        mImageQualitySlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                int quality = (int) value;
                PreferenceUtil.saveFrameImageQuality(FrameScreenshotSettingsActivity.this, quality);
                updateImageQualityText(quality);
            }
        });
    }

    private void updateColorSelectCardState(boolean enabled) {
        mColorSelectCard.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        mColorSelectCard.setAlpha(alpha);
    }
    
    private void updateImageQualityCardState(boolean enabled) {
        mImageQualityCard.setEnabled(enabled);
        mImageQualitySlider.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        mImageQualityCard.setAlpha(alpha);
    }
    
    private void updateImageQualityText(int quality) {
        mImageQualityText.setText(String.valueOf(quality));
    }

    private void showColorSelectDialog() {
        // 获取颜色文字View作为锚点，这样菜单会在右侧弹出
        View anchorView = findViewById(R.id.color_select_text);
        
        // 创建PopupMenu，锚定在颜色文字上，使用圆角样式
        PopupMenu popupMenu = new PopupMenu(this, anchorView, Gravity.END, 0, R.style.RoundedPopupMenuStyle);
        popupMenu.getMenuInflater().inflate(R.menu.menu_frame_color, popupMenu.getMenu());
        
        // 设置选中项的勾选标记
        int[] menuIds = {R.id.color_black, R.id.color_white, R.id.color_grey, R.id.color_purple};
        if (mSelectedColorIndex >= 0 && mSelectedColorIndex < menuIds.length) {
            popupMenu.getMenu().findItem(menuIds[mSelectedColorIndex]).setChecked(true);
        }
        
        // 设置菜单项点击监听
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                int position = -1;
                String colorName = "";
                
                if (itemId == R.id.color_black) {
                    position = 0;
                    colorName = mColorOptions.get(0);
                } else if (itemId == R.id.color_white) {
                    position = 1;
                    colorName = mColorOptions.get(1);
                } else if (itemId == R.id.color_grey) {
                    position = 2;
                    colorName = mColorOptions.get(2);
                } else if (itemId == R.id.color_purple) {
                    position = 3;
                    colorName = mColorOptions.get(3);
                }
                
                if (position != -1) {
                    mSelectedColorIndex = position;
                    PreferenceUtil.saveFrameColorIndex(FrameScreenshotSettingsActivity.this, position);
                    updateColorSelectText();
                    return true;
                }
                return false;
            }
        });
        
        popupMenu.show();
    }

    private void updateColorSelectText() {
        if (mSelectedColorIndex >= 0 && mSelectedColorIndex < mColorOptions.size()) {
            mColorSelectText.setText(mColorOptions.get(mSelectedColorIndex));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 返回上一级活动
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
