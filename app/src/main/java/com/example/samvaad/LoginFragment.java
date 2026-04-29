package com.example.samvaad;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.samvaad.databinding.FragmentLoginBinding;
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
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @NonNull
    @Override
    protected FragmentLoginBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToRoot) {
        return FragmentLoginBinding.inflate(inflater, container, attachToRoot);
    }

    @Override
    protected void setupUI() {
        mAuth = FirebaseAuth.getInstance();

        // ── Entrance animations ──────────────────────────────────────────
        Animation slideUpLogo = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        Animation slideUpCard = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_card);
        Animation pulse       = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);

        getBinding().ringOuter.startAnimation(slideUpLogo);
        getBinding().ringMid.startAnimation(slideUpLogo);
        getBinding().ivLogo.startAnimation(slideUpLogo);
        getBinding().tvAppName.startAnimation(slideUpLogo);
        getBinding().tvTagline.startAnimation(slideUpLogo);
        getBinding().cardLogin.startAnimation(slideUpCard);

        // Continuous pulse on logo glow
        getBinding().ivLogo.startAnimation(pulse);
        // ────────────────────────────────────────────────────────────────

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Google Login
        getBinding().btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        // Email Login
        getBinding().btnLogin.setOnClickListener(v -> loginWithEmail());

        // Navigate to Signup
        getBinding().tvSignup.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SignupFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void loginWithEmail() {
        String email = getBinding().etEmail.getText().toString().trim();
        String password = getBinding().etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            getBinding().etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            getBinding().etPassword.setError("Password is required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> runWithBinding(binding -> {
                    if (task.isSuccessful()) {
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        showError("Login failed: " + (task.getException() != null ? task.getException().getMessage() : ""));
                    }
                }));
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    showError("Google Sign-In failed: No account data.");
                }
            } catch (ApiException e) {
                // If the user cancelled, e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                showError("Google sign in failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> runWithBinding(binding -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Ensure user exists in Firestore for Google Login too
                            db.collection("users").document(firebaseUser.getUid()).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (!documentSnapshot.exists()) {
                                            User newUser = new User(firebaseUser.getDisplayName(), firebaseUser.getEmail(), "Engineering Student");
                                            db.collection("users").document(firebaseUser.getUid()).set(newUser);
                                        }
                                        updateUI(firebaseUser);
                                    });
                        }
                    } else {
                        showError("Authentication Failed.");
                    }
                }));
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onLoginSuccess();
            }
        }
    }
}