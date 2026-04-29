package com.example.samvaad.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.snackbar.Snackbar;

public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    private VB binding;

    /**
     * Guaranteed non-null binding when accessed between onCreateView and onDestroyView
     */
    protected VB getBinding() {
        if (binding == null) {
            throw new IllegalStateException("Binding should only be accessed between onCreateView and onDestroyView.");
        }
        return binding;
    }

    /**
     * Child must implement this to inflate the layout via ViewBinding
     */
    @NonNull
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToRoot);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
    }

    /**
     * Setup user interface components and listeners
     */
    protected abstract void setupUI();

    /**
     * Unified Branded Loading State UI handling
     * @param isLoading true to show loading overlay, false to hide
     */
    protected void showLoading(boolean isLoading) {
        // Implementation for an overlay progress loader:
        // Could inject a deep indigo translucent overlay containing a branded 
        // Lottie animation into the root decor view or a designated FrameLayout.
    }

    /**
     * Unified Branded Error State UI Handling
     * @param message error message to display
     */
    protected void showError(String message) {
        // Implements a branded high-contrast Teal/Purple Snackbar
        runWithBinding(vb -> {
            Snackbar snackbar = Snackbar.make(vb.getRoot(), message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    /**
     * Check if the binding is currently available and valid.
     */
    protected boolean isBindingAvailable() {
        return binding != null && isAdded() && getView() != null;
    }

    /**
     * Safely executes an action using the view binding only if the fragment is 
     * currently attached and the view is valid.
     */
    protected void runWithBinding(@NonNull java.util.function.Consumer<VB> action) {
        if (isBindingAvailable()) {
            action.accept(binding);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
