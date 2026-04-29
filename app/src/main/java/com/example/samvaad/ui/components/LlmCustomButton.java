package com.example.samvaad.ui.components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.CornerFamily;

public class LlmCustomButton extends MaterialButton {

    public LlmCustomButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LlmCustomButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LlmCustomButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Enforce strong 16dp rounded corners programmatically
        float cornerRadius = 16f * context.getResources().getDisplayMetrics().density;
        setShapeAppearanceModel(getShapeAppearanceModel().toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, cornerRadius)
                .build());

        // Enforce elevation for Material 3 standard
        setElevation(4f * context.getResources().getDisplayMetrics().density);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animateScale(0.95f); // Subtle indentation on press
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateScale(1.0f);
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animateScale(float targetScale) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", targetScale);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(150);
        animatorSet.setInterpolator(new OvershootInterpolator(1.5f));
        animatorSet.start();
    }
}
