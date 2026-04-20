package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.samvaad.databinding.ActivitySmartSessionSetupBinding;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SmartSessionSetupActivity extends AppCompatActivity {

    private ActivitySmartSessionSetupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySmartSessionSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Handle "Other" role selection visibility
        binding.cgRoles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_role_other)) {
                binding.tilCustomRole.setVisibility(View.VISIBLE);
            } else {
                binding.tilCustomRole.setVisibility(View.GONE);
            }
        });

        binding.btnGenerateSmart.setOnClickListener(v -> {
            String role = getSelectedChipText(binding.cgRoles);
            if (binding.chipRoleOther.isChecked()) {
                role = binding.etCustomRole.getText().toString().trim();
            }
            
            String company = getSelectedChipText(binding.cgCompany);
            String round = getSelectedChipText(binding.cgRound);

            if (role == null || role.isEmpty() || company == null || round == null) {
                Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            generateSession(role, company, round);
        });
    }

    private String getSelectedChipText(com.google.android.material.chip.ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id != View.NO_ID) {
            Chip chip = group.findViewById(id);
            return chip.getText().toString();
        }
        return null;
    }

    private void generateSession(String role, String company, String round) {
        binding.btnGenerateSmart.setEnabled(false);
        binding.btnGenerateSmart.setAlpha(0.5f);
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.tvLoadingTip.setVisibility(View.VISIBLE);

        LlmQuestionGenerator.generateQuestions(role, company, round, "Junior (1-3 yrs)", new LlmQuestionGenerator.QuestionCallback() {
            @Override
            public void onSuccess(List<String> questions) {
                binding.pbLoading.setVisibility(View.GONE);
                binding.tvLoadingTip.setVisibility(View.GONE);
                
                Intent intent = new Intent(SmartSessionSetupActivity.this, LiveSessionActivity.class);
                intent.putStringArrayListExtra("EXTRA_QUESTIONS", new ArrayList<>(questions));
                intent.putExtra("EXTRA_ROLE", role);
                intent.putExtra("EXTRA_MODE", "smart");
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                binding.pbLoading.setVisibility(View.GONE);
                binding.tvLoadingTip.setVisibility(View.GONE);
                binding.btnGenerateSmart.setEnabled(true);
                binding.btnGenerateSmart.setAlpha(1.0f);
                Toast.makeText(SmartSessionSetupActivity.this, "AI failed to generate: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
