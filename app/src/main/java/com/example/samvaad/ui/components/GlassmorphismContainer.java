package com.example.samvaad.ui.components;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class GlassmorphismContainer extends ConstraintLayout {

    public GlassmorphismContainer(@NonNull Context context) {
        super(context);
        init();
    }

    public GlassmorphismContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlassmorphismContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 1. Background is intentionally NOT set here.
        //    The surface color is declared via android:background in the layout XML,
        //    allowing each screen to control its own dark/light surface independently.

        // 2. Diffused Shadows — elevated with indigo-tinted spotlight for brand depth.
        setElevation(16f * getResources().getDisplayMetrics().density);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Soft ambient shadow
            setOutlineAmbientShadowColor(0x33000000);
            // Indigo spotlight shadow ties the card to the app palette
            setOutlineSpotShadowColor(0xFF3C1DE0);
        }

        // 3. Clip children to prevent overflow beyond rounded corners in the XML background.
        setClipToOutline(true);
    }
}
