package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class SessionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session, container, false);

        View btnLaunch = view.findViewById(R.id.btn_launch_preflight);
        if (btnLaunch != null) {
            btnLaunch.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LiveSessionActivity.class);
                ArrayList<String> qs = new ArrayList<>();
                qs.add("Tell me about a time you showed leadership.");
                qs.add("How do you handle conflict with a coworker?");
                qs.add("Why do you want to work here?");
                intent.putStringArrayListExtra("EXTRA_QUESTIONS", qs);
                startActivity(intent);
            });
        }

        return view;
    }
}