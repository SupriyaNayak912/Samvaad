package com.example.samvaad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class CreateScenarioBottomSheet extends BottomSheetDialogFragment {

    public interface OnScenarioCreatedListener {
        void onScenarioCreated(Scenario scenario);
    }

    private OnScenarioCreatedListener listener;
    private final List<EditText> questionFields = new ArrayList<>();

    public void setOnScenarioCreatedListener(OnScenarioCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_scenario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etTitle    = view.findViewById(R.id.et_scenario_title);
        TextInputEditText etCategory = view.findViewById(R.id.et_scenario_category);
        LinearLayout llQuestions     = view.findViewById(R.id.ll_questions);

        // Add first question field by default
        addQuestionField(llQuestions);

        // Add question button
        view.findViewById(R.id.btn_add_question).setOnClickListener(v ->
                addQuestionField(llQuestions));

        // Save button
        view.findViewById(R.id.btn_save_scenario).setOnClickListener(v -> {
            String title    = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            List<String> questions = new ArrayList<>();
            for (EditText field : questionFields) {
                String q = field.getText() != null ? field.getText().toString().trim() : "";
                if (!q.isEmpty()) questions.add(q);
            }

            if (questions.isEmpty()) {
                Toast.makeText(getContext(), "Add at least one question", Toast.LENGTH_SHORT).show();
                return;
            }

            Scenario scenario = new Scenario(
                    null, title, category.isEmpty() ? "Custom" : category,
                    "Beginner", questions.size(), 0f
            );
            scenario.setQuestions(questions);

            if (listener != null) listener.onScenarioCreated(scenario);
            dismiss();
        });
    }

    private void addQuestionField(LinearLayout container) {
        int index = questionFields.size() + 1;
        EditText field = new EditText(requireContext());
        field.setHint("Question " + index);
        field.setTextColor(getResources().getColor(R.color.text_primary, null));
        field.setHintTextColor(getResources().getColor(R.color.text_secondary, null));
        field.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getResources().getColor(R.color.primary_purple, null)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        field.setLayoutParams(params);

        questionFields.add(field);
        container.addView(field);
    }
}
