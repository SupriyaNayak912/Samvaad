package com.example.samvaad;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private PreviewView previewView;
    private MaterialButton btnStart;
    private TextView tvQuestionCategory, tvQuestionText, tvTimer;
    private boolean isRecording = false;

    private int secondsElapsed = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session, container, false);
        previewView = view.findViewById(R.id.previewView);
        btnStart = view.findViewById(R.id.btn_start);
        tvQuestionCategory = view.findViewById(R.id.tv_question_category);
        tvQuestionText = view.findViewById(R.id.tv_question_text);
        tvTimer = view.findViewById(R.id.tv_timer);

        if (checkPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnStart.setOnClickListener(v -> toggleSession());

        Bundle args = getArguments();
        if (args != null && args.containsKey("scenario_title")) {
            String title = args.getString("scenario_title");
            String cat = args.getString("scenario_category");
            String diff = args.getString("scenario_difficulty");
            tvQuestionCategory.setText(cat + " • " + diff);
            tvQuestionText.setText(title);
        } else {
            fetchRandomQuestion();
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    secondsElapsed++;
                    int minutes = secondsElapsed / 60;
                    int seconds = secondsElapsed % 60;
                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        return view;
    }

    private void fetchRandomQuestion() {
        RetrofitClient.getApiService().getRandomQuestion().enqueue(new Callback<Scenario>() {
            @Override
            public void onResponse(Call<Scenario> call, Response<Scenario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Scenario q = response.body();
                    tvQuestionCategory.setText(q.getCategory() + " • " + q.getDifficulty());
                    tvQuestionText.setText(q.getTitle());
                } else {
                    tvQuestionText.setText("Tell me about yourself.");
                }
            }

            @Override
            public void onFailure(Call<Scenario> call, Throwable t) {
                tvQuestionText.setText("What are your biggest strengths and weaknesses?");
            }
        });
    }

    private void toggleSession() {
        if (!isRecording) {
            // Start Session
            isRecording = true;
            btnStart.setText("Stop Recording");
            btnStart.setIconResource(android.R.drawable.ic_media_pause);
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_red));
            
            secondsElapsed = 0;
            tvTimer.setText("00:00");
            timerHandler.postDelayed(timerRunnable, 1000);

            FirestoreHelper.logActivity("SessionScreen", "Practice Started", 0);
            Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
        } else {
            // Stop Session
            isRecording = false;
            btnStart.setText("Start New Session");
            btnStart.setIconResource(android.R.drawable.ic_media_play);
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_blue));
            
            timerHandler.removeCallbacks(timerRunnable);

            FirestoreHelper.logActivity("SessionScreen", "Practice Stopped", secondsElapsed * 1000);
            Toast.makeText(getContext(), "Session Logged. Duration: " + secondsElapsed + "s", Toast.LENGTH_LONG).show();

            // Allow them to start a new question if they click start again
            if (getArguments() == null || !getArguments().containsKey("scenario_title")) {
                fetchRandomQuestion();
            }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}