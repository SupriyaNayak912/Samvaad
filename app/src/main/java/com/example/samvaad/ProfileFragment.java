package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvName, tvEmail, tvRole, tvStreak;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvRole = view.findViewById(R.id.tv_role);
        tvStreak = view.findViewById(R.id.tv_streak_value);

        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            fetchUserProfile(firebaseUser.getUid());
        }

        // Logout Button
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void fetchUserProfile(String uid) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            tvName.setText(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
            tvEmail.setText(firebaseUser.getEmail());
            tvRole.setText("Learner");
            tvStreak.setText("0 days");
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (user.getName() != null && !user.getName().isEmpty()) {
                                tvName.setText(user.getName());
                            }
                            if (user.getRole() != null) {
                                tvRole.setText(user.getRole());
                            }
                            tvStreak.setText(user.getPracticeStreak() + " days");
                        }
                    }
                });
    }
}