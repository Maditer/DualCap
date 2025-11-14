package com.screencap.assistant;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FrameColorSelectDialog extends Dialog {
    private Context mContext;
    private List<String> mColorList;
    private int mSelectedColorIndex;
    private OnColorSelectListener mOnColorSelectListener;

    public interface OnColorSelectListener {
        void onColorSelect(int position, String colorName);
    }

    public FrameColorSelectDialog(@NonNull Context context) {
        super(context, R.style.BottomSheetDialogStyle);
        this.mContext = context;
        this.mColorList = new ArrayList<>();
        this.mSelectedColorIndex = 0;
        init();
    }

    public FrameColorSelectDialog(@NonNull Context context, List<String> colorList, int selectedIndex) {
        super(context, R.style.BottomSheetDialogStyle);
        this.mContext = context;
        this.mColorList = colorList;
        this.mSelectedColorIndex = selectedIndex;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_frame_color_select, null);
        setContentView(view);

        // 设置对话框宽度为屏幕宽度
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);
        window.setGravity(Gravity.BOTTOM);

        // 初始化颜色列表
        LinearLayout colorContainer = view.findViewById(R.id.color_container);
        for (int i = 0; i < mColorList.size(); i++) {
            TextView textView = new TextView(mContext);
            textView.setText(mColorList.get(i));
            textView.setTextSize(16);
            textView.setTextColor(Color.parseColor("#333333"));
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, 20, 0, 20);
            textView.setClickable(true);
            
            final int position = i;
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedColorIndex = position;
                    if (mOnColorSelectListener != null) {
                        mOnColorSelectListener.onColorSelect(position, mColorList.get(position));
                    }
                    dismiss();
                }
            });

            // 设置选中状态
            if (i == mSelectedColorIndex) {
                textView.setTextColor(mContext.getResources().getColor(R.color.primary_color));
            }

            colorContainer.addView(textView);
        }
    }

    public void setOnColorSelectListener(OnColorSelectListener listener) {
        this.mOnColorSelectListener = listener;
    }
}
