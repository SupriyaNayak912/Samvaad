package com.example.samvaad;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import java.util.concurrent.ExecutionException;

public class SessionFragment extends Fragment {

    private static final int PERMISSION_CODE = 101;
    private PreviewView previewView;
    private View videoOffOverlay;
    private MaterialButton btnStart, btnToggleVideo, btnToggleAudio;
    private TextView tvDbLevel, tvStatusBadge;
    private ProgressBar pbAudioLevel;

    private boolean isRecording = false;
    private boolean isVideoOn = true;
    private boolean isAudioOn = true;

    private AudioRecord audioRecord;
    private int bufferSize;
    private Thread audioThread;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session, container, false);

        previewView = view.findViewById(R.id.previewView);
        videoOffOverlay = view.findViewById(R.id.video_off_overlay);
        btnStart = view.findViewById(R.id.btn_start);
        btnToggleVideo = view.findViewById(R.id.btn_toggle_video);
        btnToggleAudio = view.findViewById(R.id.btn_toggle_audio);
        tvDbLevel = view.findViewById(R.id.tv_db_level);
        pbAudioLevel = view.findViewById(R.id.pb_audio_level);
        tvStatusBadge = view.findViewById(R.id.tv_status_badge);

        if (checkPermissions()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
        }

        btnStart.setOnClickListener(v -> toggleSession());
        btnToggleVideo.setOnClickListener(v -> toggleVideo());
        btnToggleAudio.setOnClickListener(v -> toggleAudio());

        return view;
    }

    private void toggleVideo() {
        isVideoOn = !isVideoOn;
        if (isVideoOn) {
            btnToggleVideo.setText("Video ON");
            btnToggleVideo.setIconResource(android.R.drawable.ic_menu_camera);
            videoOffOverlay.setVisibility(View.GONE);
            startCamera();
        } else {
            btnToggleVideo.setText("Video OFF");
            btnToggleVideo.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            videoOffOverlay.setVisibility(View.VISIBLE);
            stopCamera();
        }
    }

    private void toggleAudio() {
        isAudioOn = !isAudioOn;
        if (isAudioOn) {
            btnToggleAudio.setText("Audio ON");
            btnToggleAudio.setIconResource(android.R.drawable.ic_btn_speak_now);
            if (isRecording) startAudioMonitoring();
        } else {
            btnToggleAudio.setText("Audio OFF");
            btnToggleAudio.setIconResource(android.R.drawable.ic_lock_silent_mode);
            stopAudioMonitoring();
            tvDbLevel.setText("0 dB");
            pbAudioLevel.setProgress(0);
        }
    }

    private void toggleSession() {
        if (!isRecording) {
            startSession();
        } else {
            stopSession();
        }
    }

    private void startSession() {
        isRecording = true;
        btnStart.setText("Stop Session");
        btnStart.setIconResource(android.R.drawable.ic_media_pause);
        btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_red));
        tvStatusBadge.setText("Live");
        tvStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_red));

        if (isAudioOn) startAudioMonitoring();
        FirestoreHelper.logActivity("SessionScreen", "Practice Started", 0);
    }

    private void stopSession() {
        isRecording = false;
        btnStart.setText("Start Session");
        btnStart.setIconResource(android.R.drawable.ic_media_play);
        btnStart.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_purple));
        tvStatusBadge.setText("Ready");
        tvStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_green));

        stopAudioMonitoring();
        FirestoreHelper.logActivity("SessionScreen", "Practice Stopped", 0);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Permissions are required for the session", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        if (!isVideoOn) return;
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

    private void stopCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startAudioMonitoring() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;

        bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        audioRecord.startRecording();
        audioThread = new Thread(() -> {
            short[] buffer = new short[bufferSize];
            while (isRecording && isAudioOn) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                if (read > 0) {
                    double amplitude = 0;
                    for (short s : buffer) {
                        amplitude += Math.abs(s);
                    }
                    double avgAmplitude = amplitude / read;
                    double db = 20 * Math.log10(avgAmplitude);
                    if (Double.isInfinite(db) || Double.isNaN(db)) db = 0;

                    final int finalDb = (int) db;
                    mainHandler.post(() -> {
                        tvDbLevel.setText(finalDb + " dB");
                        pbAudioLevel.setProgress(Math.min(finalDb, 100));
                    });
                }
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        });
        audioThread.start();
    }

    private void stopAudioMonitoring() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAudioMonitoring();
    }
}