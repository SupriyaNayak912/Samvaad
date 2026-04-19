package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.samvaad.databinding.FragmentProfileBinding;
import com.example.samvaad.ui.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    @NonNull
    @Override
    protected FragmentProfileBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToRoot) {
        return FragmentProfileBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            fetchUserProfile(firebaseUser.getUid());
            loadActivityHeatmap();
        }

        // Logout Button
        getBinding().btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Edit Profile Button
        getBinding().btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Target Role Clickable
        getBinding().tvRole.setOnClickListener(v -> showEditRoleDialog());
    }

    private void fetchUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> runWithBinding(binding -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            binding.tvName.setText(currentUser.getName());
                            binding.tvEmail.setText(currentUser.getEmail());
                            String role = currentUser.getRole();
                            binding.tvRole.setText( (role == null || role.isEmpty()) ? "Tap to set target role" : role);
                        }
                    }
                }));
    }

    private void loadActivityHeatmap() {
        SessionRepository.getSessions(new SessionRepository.ListCallback() {
            @Override
            public void onSuccess(List<SessionMetrics> sessions) {
                runWithBinding(binding -> {
                    List<Long> timestamps = new ArrayList<>();
                    for (SessionMetrics s : sessions) {
                        timestamps.add(s.timestamp);
                    }
                    binding.heatmapView.setSessionTimestamps(timestamps);
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Silently fail or show smallToast
            }
        });
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_edit_name);
        etName.setText(currentUser.getName());

        new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog)
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateProfileName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditRoleDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_role, null);
        EditText etRole = dialogView.findViewById(R.id.et_edit_role);
        etRole.setText(currentUser.getRole());

        new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog)
                .setTitle("Target Career Role")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newRole = etRole.getText().toString().trim();
                    if (!newRole.isEmpty()) {
                        updateProfileRole(newRole);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfileName(String newName) {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) return;

        db.collection("users").document(fUser.getUid())
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    currentUser.setName(newName);
                    getBinding().tvName.setText(newName);
                });
    }

    private void updateProfileRole(String newRole) {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) return;

        db.collection("users").document(fUser.getUid())
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    currentUser.setRole(newRole);
                    getBinding().tvRole.setText(newRole);
                });
    }
}