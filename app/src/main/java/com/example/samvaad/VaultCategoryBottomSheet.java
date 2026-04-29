package com.example.samvaad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.example.samvaad.databinding.BottomSheetVaultCategoryBinding;

public class VaultCategoryBottomSheet extends BottomSheetDialogFragment {

    private final ConfigChoiceListener listener;
    private BottomSheetVaultCategoryBinding binding;

    public interface ConfigChoiceListener {
        void onConfigSelected(String category, String difficulty);
    }

    public VaultCategoryBottomSheet(ConfigChoiceListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVaultCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnApplyVaultConfig.setOnClickListener(v -> {
            String category = getSelectedText(binding.cgVaultCategory, "HR");
            String difficulty = getSelectedText(binding.cgVaultDifficulty, "Beginner");
            
            // Map the toggle names to match Scenario constants
            if (category.contains("General")) category = "HR";
            
            dismiss();
            listener.onConfigSelected(category, difficulty);
        });
    }

    private String getSelectedText(com.google.android.material.chip.ChipGroup group, String fallback) {
        int id = group.getCheckedChipId();
        if (id != View.NO_ID) {
            Chip chip = group.findViewById(id);
            return chip.getText().toString();
        }
        return fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
