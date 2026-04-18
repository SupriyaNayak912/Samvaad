package com.example.samvaad;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Global Application class.
 * Forces dark mode unconditionally so Theme.Samvaad always renders
 * the DayNight parent in its dark-mode variant, giving us the correct
 * M3 color tokens (colorSecondaryContainer etc.) while our own
 * colour overrides (deep indigo bg, purple primary, teal secondary)
 * sit on top.
 */
public class SamvaadApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Lock the entire app to dark mode — never allow the OS light mode
        // to switch us to a white theme.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
