package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.samvaad.databinding.FragmentSignupBinding;
import com.example.samvaad.ui.base.BaseFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupFragment extends BaseFragment<FragmentSignupBinding> {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @NonNull
    @Override
    protected FragmentSignupBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToRoot) {
        return FragmentSignupBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Button Listeners
        getBinding().btnSignup.setOnClickListener(v -> registerUser());

        // Google Sign-Up Listener
        getBinding().btnGoogleSignup.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        getBinding().tvLogin.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        });
    }

    // --- EMAIL REGISTRATION LOGIC ---
    private void registerUser() {
        String name = getBinding().etName.getText().toString().trim();
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            getBinding().etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            getBinding().etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            getBinding().etPassword.setError("Password must be at least 6 characters");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> runWithBinding(binding -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            
                            // Chain the Firestore save after profile update
                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        saveUserToFirestore(name, email, firebaseUser.getUid());
                                    });
                        }
                    } else {
                        showError("Registration failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }));
    }

    // --- GOOGLE REGISTRATION LOGIC ---
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                showError("Google sign up failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> runWithBinding(binding -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getDisplayName(), user.getEmail(), user.getUid());
                        }
                    } else {
                        showError("Authentication Failed.");
                    }
                }));
    }

    // Helper to save data
    private void saveUserToFirestore(String name, String email, String uid) {
        User newUser = new User(name, email, "Future Leader / Tech Aspirant");
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> runWithBinding(binding -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).onLoginSuccess();
                    }
                }))
                .addOnFailureListener(e -> showError("Failed to save user data."));
    }
}