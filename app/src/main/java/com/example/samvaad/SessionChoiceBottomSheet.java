package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SessionChoiceBottomSheet extends BottomSheetDialogFragment {

    private final SessionChoiceListener listener;

    public interface SessionChoiceListener {
        void onVaultSelected();
    }

    public SessionChoiceBottomSheet(SessionChoiceListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_session_choice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.card_smart_session).setOnClickListener(v -> {
            dismiss();
            startActivity(new Intent(requireContext(), SmartSessionSetupActivity.class));
        });

        view.findViewById(R.id.card_vault_session).setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onVaultSelected();
            }
        });
    }
}
