package com.screencap.assistant;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FrameColorOptionsDialog extends Dialog {
    private Context mContext;
    private List<String> mColorOptions;
    private int mSelectedIndex;
    private OnColorSelectedListener mListener;

    public interface OnColorSelectedListener {
        void onColorSelected(int position);
    }

    public FrameColorOptionsDialog(@NonNull Context context, List<String> colorOptions, int selectedIndex) {
        super(context, R.style.BottomSheetDialogStyle);
        mContext = context;
        mColorOptions = colorOptions;
        mSelectedIndex = selectedIndex;
        init();
    }

    private void init() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_frame_color_options, null);
        setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.color_options_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ColorOptionsAdapter adapter = new ColorOptionsAdapter();
        recyclerView.setAdapter(adapter);

        // 设置对话框宽度为屏幕宽度
        ViewGroup.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) layoutParams);
        getWindow().setGravity(android.view.Gravity.BOTTOM);
        getWindow().setWindowAnimations(R.style.BottomSheetDialogAnimation);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        mListener = listener;
    }

    private class ColorOptionsAdapter extends RecyclerView.Adapter<ColorOptionsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_frame_color_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String colorOption = mColorOptions.get(position);
            holder.colorOptionText.setText(colorOption);

            // 设置选中状态
            if (position == mSelectedIndex) {
                holder.colorOptionText.setTextColor(mContext.getResources().getColor(R.color.primary_color));
                holder.colorOptionText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
            } else {
                holder.colorOptionText.setTextColor(mContext.getResources().getColor(R.color.primary_text));
                holder.colorOptionText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            // 设置点击事件
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onColorSelected(position);
                    }
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mColorOptions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView colorOptionText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                colorOptionText = itemView.findViewById(R.id.color_option_text);
            }
        }
    }
}
