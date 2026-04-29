package com.example.samvaad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LevelUpBottomSheet extends BottomSheetDialogFragment {

    private final LevelSystem.Level level;
    private final Runnable onDismiss;

    public LevelUpBottomSheet(LevelSystem.Level level, Runnable onDismiss) {
        this.level = level;
        this.onDismiss = onDismiss;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_level_up_celebration, container, false);
        
        TextView tvName = v.findViewById(R.id.tv_level_name);
        TextView tvDesc = v.findViewById(R.id.tv_level_desc);
        
        tvName.setText("Unlocked: " + level.title);
        tvDesc.setText("New Goal: " + level.goal);
        
        v.findViewById(R.id.btn_continue).setOnClickListener(view -> {
            dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        return v;
    }
}
