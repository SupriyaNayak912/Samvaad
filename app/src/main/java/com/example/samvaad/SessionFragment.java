package com.example.samvaad;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class SessionFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private PreviewView previewView;
    private MaterialButton btnStart;
    private boolean isRecording = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session, container, false);
        previewView = view.findViewById(R.id.previewView);
        btnStart = view.findViewById(R.id.btn_start);

        if (checkPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnStart.setOnClickListener(v -> toggleSession());

        return view;
    }

    private void toggleSession() {
        if (!isRecording) {
            // Start Session
            isRecording = true;
            btnStart.setText("Stop");
            btnStart.setIconResource(android.R.drawable.ic_media_pause);
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_red));
            
            // Log to Firestore (Exp 10/11)
            FirestoreHelper.logActivity("SessionScreen", "Practice Started", 0);
            Toast.makeText(getContext(), "Audit Trail Started", Toast.LENGTH_SHORT).show();
        } else {
            // Stop Session
            isRecording = false;
            btnStart.setText("Start");
            btnStart.setIconResource(android.R.drawable.ic_media_play);
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_blue));
            
            FirestoreHelper.logActivity("SessionScreen", "Practice Stopped", 5000); // Dummy duration
            Toast.makeText(getContext(), "Session Logged to Cloud", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission is required for proctoring", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}