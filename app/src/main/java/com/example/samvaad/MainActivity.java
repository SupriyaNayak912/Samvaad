package com.example.samvaad;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_scenarios) {
                selectedFragment = new ScenariosFragment();
            } else if (itemId == R.id.navigation_roadmap) {
                selectedFragment = new RoadmapFragment();
            } else if (itemId == R.id.navigation_session) {
                SessionChoiceBottomSheet bottomSheet = new SessionChoiceBottomSheet(() -> {
                    VaultCategoryBottomSheet catSheet = new VaultCategoryBottomSheet((category, difficulty) -> {
                        switchToVaultTab(category, difficulty);
                    });
                    catSheet.show(getSupportFragmentManager(), "VaultCategory");
                });
                bottomSheet.show(getSupportFragmentManager(), "SessionChoice");
                return false; 
            } else if (itemId == R.id.navigation_stats) {
                selectedFragment = new SessionHistoryFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Initial Auth Check
        checkUserStatus();
        handleIntentRouting();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Hide Bottom Nav and show Login
            bottomNav.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        } else {
            // Show Bottom Nav and show Home
            bottomNav.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    public void navigateToHome() {
        bottomNav.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        bottomNav.setSelectedItemId(R.id.navigation_home);
    }

    public void onLoginSuccess() {
        navigateToHome();
    }

    private String pendingVaultCategory = null;
    private String pendingVaultDifficulty = null;

    public void switchToVaultTab(String category, String difficulty) {
        this.pendingVaultCategory = category;
        this.pendingVaultDifficulty = difficulty;
        bottomNav.setSelectedItemId(R.id.navigation_scenarios);
    }

    public String getPendingVaultCategory() {
        return pendingVaultCategory;
    }

    public String getPendingVaultDifficulty() {
        return pendingVaultDifficulty;
    }

    public void clearPendingVaultConfig() {
        this.pendingVaultCategory = null;
        this.pendingVaultDifficulty = null;
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentRouting();
    }

    private void handleIntentRouting() {
        android.content.Intent intent = getIntent();
        if ("ACTION_SHOW_STATS".equals(intent.getAction())) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setSelectedItemId(R.id.navigation_stats);
            StatsFragment statsFragment = new StatsFragment();
            Bundle bundle = new Bundle();
            
            if (intent.hasExtra("EXTRA_SESSION_ID")) {
                bundle.putString("EXTRA_SESSION_ID", intent.getStringExtra("EXTRA_SESSION_ID"));
            } else if (intent.hasExtra("SESSION_METRICS")) {
                bundle.putParcelable("SESSION_METRICS", intent.getParcelableExtra("SESSION_METRICS"));
            }
            
            statsFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, statsFragment)
                    .commit();
        }
    }
}