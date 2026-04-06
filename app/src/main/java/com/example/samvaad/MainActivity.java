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
            } else if (itemId == R.id.navigation_session) {
                // If they explicitly tap the Session tab, we can route them back to the old fragment so they see it.
                selectedFragment = new SessionFragment();
            } else if (itemId == R.id.navigation_stats) {
                selectedFragment = new LogsFragment();
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

    public void onLoginSuccess() {
        bottomNav.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        // Optional: Select Home in Bottom Nav
        bottomNav.setSelectedItemId(R.id.navigation_home);
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
            SessionMetrics metrics = intent.getParcelableExtra("SESSION_METRICS");
            bundle.putParcelable("SESSION_METRICS", metrics);
            statsFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, statsFragment)
                    .commit();
        }
    }
}