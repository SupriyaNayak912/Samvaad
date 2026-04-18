package com.example.samvaad;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.util.Log;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.samvaad.databinding.ActivityLiveSessionBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import android.media.AudioManager;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.content.Context;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import android.annotation.SuppressLint;
import android.media.Image;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.FileOutputOptions;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Quality;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.core.UseCaseGroup;
import com.example.samvaad.SessionMetrics;

public class LiveSessionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, SensorEventListener {

    // ─── ViewBinding ────────────────────────────────────────────────
    private ActivityLiveSessionBinding binding;

    // ─── Session State ───────────────────────────────────────────────
    private List<String> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private boolean chaosEnabled     = false;
    private boolean cameraEnabled    = true;
    private boolean sessionRunning   = false;

    // ─── Telemetry Metrics ──────────────────────────────────────────
    private long sessionStartTime;
    private int  silenceCount        = 0;
    private int  wordCount           = 0;  // cumulative (stub)
    private int  avgWpm              = 0;
    private long chaosRecoveryTime   = 0;
    private long chaosStartTime      = 0;

    // ─── TTS ────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // ─── Audio Monitoring ────────────────────────────────────────────
    private AudioRecord audioRecord;
    private ExecutorService audioExecutor;
    private volatile boolean monitoringAudio  = false;
    private static final int SAMPLE_RATE      = 44100;
    private static final int SILENCE_DB       = -48;      // more sensitive to low speech
    private static final long SILENCE_TIMEOUT = 5500L;     // 5.5 s silence → next question
    private long silenceStartMs               = -1;
    private int fillerWordsCount              = 0;
    private long speechBurstStartMs           = -1;

    // ─── Analytics Metrics ───────────────────────────────────────────
    private int totalFaceChecks = 0;
    private int successfulFaceChecks = 0;
    private int chaosDistractionCount = 0;
    private ArrayList<Float> amplitudeTimeline = new ArrayList<>();
    private long lastTimelineUpdateMs = 0;

    // ─── Timers & Handlers ───────────────────────────────────────────
    private Handler mainHandler    = new Handler(Looper.getMainLooper());
    private CountDownTimer questionTimer;
    private static final int QUESTION_DURATION_SEC = 90;  // 90 s per question

    // Chaos Handler
    private Handler chaosHandler   = new Handler(Looper.getMainLooper());
    private Runnable chaosRunnable;

    // WPM stub interval (every 10 s)
    private Handler wpmHandler     = new Handler(Looper.getMainLooper());
    private Runnable wpmRunnable;

    // Breathing countdown
    private CountDownTimer breathTimer;

    // Gestures
    private android.view.GestureDetector gestureDetector;

    // Telemetry border colors
    private static final int COLOR_ORANGE = 0x80FF8C00; // 50% orange — fast WPM
    private static final int COLOR_BLUE   = 0x803B82F6; // 50% blue  — mumbling

    // ─── Camera, ML Kit, and Validations ────────────────────────────────
    private FaceDetector faceDetector;
    private boolean isCameraReady = false;
    private boolean isMicReady    = false;
    private boolean isFaceReady   = false;
    private long gracePeriodEndMs = -1;
    
    // ─── Accelerometer & Video Capture ──────────────────────────────
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double currentPitch = 0.0;
    private boolean isOrientationReady = false;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private String currentVideoPath;
    private long lastDropCheckMs = 0;

    // ────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupGestures();

        loadScenarioFromIntent();
        initTts();
        setupPreFlight();
        initSensors();
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(android.view.MotionEvent e) {
                if (sessionRunning && binding.viewFlipper.getDisplayedChild() == 1) {
                    showGestureToast("⏸ Pausing Session");
                    // Implement pause logic
                    triggerBreathingExercise();
                }
                return true;
            }

            @Override
            public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                if (sessionRunning && binding.viewFlipper.getDisplayedChild() == 1) {
                    if (e1.getY() - e2.getY() > 100 && Math.abs(velocityY) > 100) {
                        showGestureToast("⏩ Skipping Question");
                        advanceQuestion();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(android.view.MotionEvent e) {
                if (sessionRunning && binding.viewFlipper.getDisplayedChild() == 1) {
                    showGestureToast("⏹ Emergency Stop");
                    confirmEndSession();
                }
            }
        });

        // Attach touch listener to the root layout of phase 2
        binding.viewFlipper.getChildAt(1).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void showGestureToast(String text) {
        binding.tvGestureText.setText(text);
        binding.toastGestureBadge.setAlpha(0f);
        binding.toastGestureBadge.setVisibility(View.VISIBLE);
        binding.toastGestureBadge.animate().alpha(1f).setDuration(200).withEndAction(() -> {
            binding.toastGestureBadge.postDelayed(() -> {
                binding.toastGestureBadge.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    binding.toastGestureBadge.setVisibility(View.GONE);
                });
            }, 1000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioMonitor();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (questionTimer != null) questionTimer.cancel();
        if (breathTimer != null) breathTimer.cancel();
        chaosHandler.removeCallbacksAndMessages(null);
        wpmHandler.removeCallbacksAndMessages(null);
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  INTENT — load StringArrayList Extra
    // ────────────────────────────────────────────────────────────────

    private void loadScenarioFromIntent() {
        ArrayList<String> extraQuestions = getIntent().getStringArrayListExtra("EXTRA_QUESTIONS");
        if (extraQuestions != null && !extraQuestions.isEmpty()) {
            questions.addAll(extraQuestions);
            binding.tvScenarioChip.setText("Custom · Session");
        } else {
            // Fallback: generic questions
            questions.add("Tell me about yourself and your professional journey.");
            questions.add("What is your greatest strength, and how have you used it?");
            questions.add("Describe a challenge you faced and how you overcame it.");
            binding.tvScenarioChip.setText("General · Default");
        }

        // Shuffle for randomization
        Collections.shuffle(questions);
    }

    // ────────────────────────────────────────────────────────────────
    //  TTS INIT
    // ────────────────────────────────────────────────────────────────

    private void initTts() {
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.92f);  // Slightly deliberate — coaching tone
            tts.setPitch(1.05f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    mainHandler.post(() -> binding.tvAiSpeaking.setVisibility(View.VISIBLE));
                }
                @Override public void onDone(String utteranceId) {
                    mainHandler.post(() -> {
                        binding.tvAiSpeaking.setVisibility(View.GONE);
                        binding.tvSilenceHint.setVisibility(View.VISIBLE);
                        // Start 4-second grace period before VAD evaluates silence
                        gracePeriodEndMs = System.currentTimeMillis() + 4000L;
                        silenceStartMs = -1;
                    });
                }
                @Override public void onError(String utteranceId) {
                    mainHandler.post(() -> binding.tvAiSpeaking.setVisibility(View.GONE));
                }
            });
            ttsReady = true;
        }
    }

    private void speakQuestion(String text) {
        binding.tvAiSpeaking.setVisibility(View.VISIBLE);
        binding.tvSilenceHint.setVisibility(View.GONE);
        if (ttsReady) {
            Bundle params = new Bundle();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "question_" + currentQuestionIndex);
        }
    }

    // ─── SENSOR CALLBACKS ───────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 1. Calculate Pitch (Golden Angle)
            // double pitch = Math.toDegrees(Math.atan2(event.values[1], event.values[2]));
            currentPitch = Math.toDegrees(Math.atan2(y, z));

            // 2. Gatekeeper Logic (Phase 1)
            if (binding.viewFlipper.getDisplayedChild() == 0) {
                if (currentPitch >= 70 && currentPitch <= 85) {
                    if (!isOrientationReady) {
                        isOrientationReady = true;
                        updatePreFlightChecks();
                    }
                    binding.tvOrientationWarning.setVisibility(View.GONE);
                } else {
                    if (isOrientationReady) {
                        isOrientationReady = false;
                        updatePreFlightChecks();
                    }
                    binding.tvOrientationWarning.setVisibility(View.VISIBLE);
                }
            }

            // 3. Free Fall Protection (Phase 2)
            if (sessionRunning && binding.viewFlipper.getDisplayedChild() == 1) {
                long now = System.currentTimeMillis();
                if (now - lastDropCheckMs > 200) { // Throttle to 5Hz
                    lastDropCheckMs = now;
                    double magnitude = Math.sqrt(x * x + y * y + z * z);
                    if (magnitude < 2.0) {
                        pauseSessionForReset();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void pauseSessionForReset() {
        if (!sessionRunning) return;
        sessionRunning = false;
        if (questionTimer != null) questionTimer.cancel();
        if (activeRecording != null) {
            activeRecording.pause();
        }
        new AlertDialog.Builder(this)
                .setTitle("Drop Detected")
                .setMessage("Device alignment lost. Please reset the phone angle to continue.")
                .setCancelable(false)
                .setPositiveButton("Reset & Resume", (dialog, which) -> {
                    if (currentPitch >= 70 && currentPitch <= 85) {
                        sessionRunning = true;
                        if (activeRecording != null) activeRecording.resume();
                        showQuestion(currentQuestionIndex);
                    } else {
                        Toast.makeText(this, "Still not aligned!", Toast.LENGTH_SHORT).show();
                        pauseSessionForReset(); // recursive until aligned
                    }
                })
                .show();
    }

    // ────────────────────────────────────────────────────────────────
    //  PRE-FLIGHT SETUP (Phase 1)
    // ────────────────────────────────────────────────────────────────

    private void setupPreFlight() {
        // Initialize Face Detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);

        // UI states
        binding.btnBeginScenario.setEnabled(false);
        binding.btnBeginScenario.setAlpha(0.5f);
        binding.tvCheckMic.setTextColor(getColor(R.color.text_secondary));
        binding.tvCheckCamera.setTextColor(getColor(R.color.text_secondary));
        binding.tvCheckFace.setTextColor(getColor(R.color.text_secondary));

        // Start mic & camera immediately for health check
        startMicAmplitudePreview();
        startCameraSetup();

        binding.btnBack.setOnClickListener(v -> finish());

        // Camera Switch
        binding.switchCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.tvAudioOnlyOverlay.setVisibility(View.GONE);
                startCameraSetup();
            } else {
                binding.tvAudioOnlyOverlay.setVisibility(View.VISIBLE);
                isCameraReady = true; // Bypassed
                isFaceReady = true;   // Bypassed
                updatePreFlightChecks();
            }
        });

        // Chaos Switch UI styling (dimming when off)
        binding.switchChaos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            float alpha = isChecked ? 1.0f : 0.5f;
            binding.tvChaosLabel.setAlpha(alpha);
            binding.tvChaosLabel.setText(isChecked ? "Chaos Mode ON" : "Chaos Mode OFF");
        });
        // Trigger initial state
        binding.switchChaos.setChecked(false);
        binding.tvChaosLabel.setText("Chaos Mode OFF");
        binding.tvChaosLabel.setAlpha(0.5f);

        binding.btnBeginScenario.setOnClickListener(v -> {
            chaosEnabled  = binding.switchChaos.isChecked();
            cameraEnabled = binding.switchCamera.isChecked();
            beginSession();
        });

        // Integrated Camera Card Following Logic (Phase 1)
        // Keeps the root-level card pinned to its placeholder in the scroll list
        binding.scrollPreflight.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (view, scrollX, scrollY, oldX, oldY) -> {
            if (binding.viewFlipper.getDisplayedChild() == 0) {
                binding.cardCameraContainer.setTranslationY(-scrollY);
            }
        });

        binding.viewFlipper.setDisplayedChild(0);
    }

    private void updatePreFlightChecks() {
        mainHandler.post(() -> {
            if (isMicReady) {
                binding.tvCheckMic.setText("✔  Microphone Verified");
                binding.tvCheckMic.setTextColor(getColor(R.color.status_green));
            }
            if (isCameraReady) {
                binding.tvCheckCamera.setText("✔  Camera Verified");
                binding.tvCheckCamera.setTextColor(getColor(R.color.status_green));
            }
            if (isFaceReady) {
                binding.tvCheckFace.setText("✔  User Detected in Frame");
                binding.tvCheckFace.setTextColor(getColor(R.color.status_green));
            }
            if (isOrientationReady) {
                binding.tvCheckOrientation.setText("✔  Angle Optimal");
                binding.tvCheckOrientation.setTextColor(getColor(R.color.status_green));
            } else {
                binding.tvCheckOrientation.setText("✘  Adjust phone angle");
                binding.tvCheckOrientation.setTextColor(getColor(R.color.status_red));
            }

            if (isMicReady && isCameraReady && isFaceReady && isOrientationReady) {
                binding.btnBeginScenario.setEnabled(true);
                binding.btnBeginScenario.setAlpha(1.0f);
            } else {
                binding.btnBeginScenario.setEnabled(false);
                binding.btnBeginScenario.setAlpha(0.5f);
            }
        });
    }

    private void startCameraSetup() {
        if (!binding.switchCamera.isChecked()) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        faceDetector.process(image)
                                .addOnSuccessListener(faces -> {
                                    if (!isCameraReady) {
                                        isCameraReady = true;
                                        updatePreFlightChecks();
                                    }
                                    if (!faces.isEmpty() && !isFaceReady) {
                                        isFaceReady = true;
                                        updatePreFlightChecks();
                                    }
                                    
                                    if (sessionRunning) {
                                        totalFaceChecks++;
                                        if (!faces.isEmpty()) successfulFaceChecks++;
                                    }
                                })
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                
                // Add VideoCapture Setup
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, videoCapture);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ────────────────────────────────────────────────────────────────
    //  SESSION START — transition to Phase 2
    // ────────────────────────────────────────────────────────────────

    private void beginSession() {
        enableImmersiveMode();
        sessionRunning    = true;
        sessionStartTime  = System.currentTimeMillis();
        currentQuestionIndex = 0;
        
        // Take Audio Focus to suppress background music
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(focusChange -> {}, 
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        }

        // Animate the Camera to a Picture-in-Picture at the Top Left (Professional Meeting Style)
        androidx.cardview.widget.CardView cameraCard = binding.cardCameraContainer;
        if (cameraCard != null) {
            float targetScale = 0.32f; 
            
            // In Top-Left mode, we pivot at 0,0
            cameraCard.setPivotX(0f);
            cameraCard.setPivotY(0f);
            
            cameraCard.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationX(16f)  // 16dp from left
                    .translationY(16f)  // 16dp from top
                    .setDuration(800)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withStartAction(() -> {
                        cameraCard.setRadius(200f); // Make it a circle/near-circle bubble
                        binding.cardCameraContainer.setTranslationY(0); // Reset pre-flight scroll translation
                    })
                    .start();
        }

        // Flip to Phase 2
        binding.viewFlipper.setDisplayedChild(1);
        binding.pbQuestionTimer.setVisibility(View.VISIBLE);

        // Update Chaos mode indicator
        if (chaosEnabled) {
            binding.tvChaosIndicator.setText("🔥 Chaos ON");
            binding.tvChaosIndicator.setTextColor(getColor(R.color.status_orange));
            scheduleChaosEvent();
        }

        // Wire Phase 2 buttons
        binding.btnBreathe.setOnClickListener(v -> triggerBreathingExercise());
        binding.btnEndSession.setOnClickListener(v -> confirmEndSession());
        binding.btnNextQuestion.setOnClickListener(v -> manualSkipQuestion());

        startAudioMonitor();
        startWpmTracking();
        
        // Start Video Recording
        if (cameraEnabled && videoCapture != null) {
            startRecording();
        }
        
        showQuestion(currentQuestionIndex);
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File videoFile = new File(getCacheDir(), "recording_" + timeStamp + ".mp4");
        currentVideoPath = videoFile.getAbsolutePath();
        Log.d("SamvaadVideo", "Recording started: " + currentVideoPath);

        FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(videoFile).build();

        activeRecording = videoCapture.getOutput()
                .prepareRecording(this, fileOutputOptions)
                .withAudioEnabled() 
                .start(ContextCompat.getMainExecutor(this), event -> {
                    if (event instanceof VideoRecordEvent.Start) {
                        // Started
                    } else if (event instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                        if (finalizeEvent.hasError()) {
                            // Handle error
                        }
                    }
                });
    }

    private void stopRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  QUESTION DISPLAY & TIMER
    // ────────────────────────────────────────────────────────────────

    private void showQuestion(int index) {
        if (index >= questions.size()) {
            endSession();
            return;
        }

        String question = questions.get(index);
        binding.tvQuestionText.setText(question);
        binding.tvQuestionCounter.setText("Question " + (index + 1) + " of " + questions.size());
        binding.tvSilenceHint.setVisibility(View.GONE);
        binding.btnNextQuestion.setEnabled(true);

        // Entrance animation
        binding.cardQuestionVault.setAlpha(0f);
        binding.cardQuestionVault.animate().alpha(1f).setDuration(400).start();

        // AI speaks the question
        speakQuestion(question);

        // Reset & start question countdown timer
        if (questionTimer != null) questionTimer.cancel();
        binding.pbQuestionTimer.setMax(QUESTION_DURATION_SEC);
        binding.pbQuestionTimer.setProgress(QUESTION_DURATION_SEC);

        questionTimer = new CountDownTimer(QUESTION_DURATION_SEC * 1000L, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                int remaining = (int) (millisUntilFinished / 1000);
                binding.pbQuestionTimer.setProgress(remaining);
                binding.tvSessionTimer.setText(formatTime((System.currentTimeMillis() - sessionStartTime) / 1000));
            }
            @Override public void onFinish() {
                // Auto-advance on timeout
                silenceCount++;
                advanceQuestion();
            }
        }.start();
    }

    private void manualSkipQuestion() {
        binding.btnNextQuestion.setEnabled(false);
        advanceQuestion();
    }

    private void advanceQuestion() {
        // Play subtle ding
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100);
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {}

        // Telegraph the skip so the user isn't jarred
        binding.tvSilenceHint.setVisibility(View.VISIBLE);
        binding.tvSilenceHint.setTextColor(getColor(R.color.status_orange));
        binding.tvSilenceHint.setText("Advancing...");
        
        mainHandler.postDelayed(() -> {
            binding.tvSilenceHint.setTextColor(getColor(R.color.text_secondary));
            currentQuestionIndex++;
            binding.tvSilenceCount.setText(silenceCount + " Pauses");
            showQuestion(currentQuestionIndex);
        }, 1200);
    }

    private String formatTime(long totalSeconds) {
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    // ────────────────────────────────────────────────────────────────
    //  AUDIO MONITORING — Amplitude + VAD (Silence Detection)
    // ────────────────────────────────────────────────────────────────

    private void startMicAmplitudePreview() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1001);
            return;
        }

        // Lightweight preview for pre-flight health bar (no VAD logic)
        audioExecutor = Executors.newSingleThreadExecutor();
        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "Microphone initialization failed", Toast.LENGTH_SHORT).show();
                return;
            }
            audioRecord.startRecording();
            monitoringAudio = true;

            audioExecutor.submit(() -> {
                short[] buffer = new short[bufferSize];
                while (monitoringAudio) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        double rms = calculateRms(buffer, read);
                        double db  = rmsToDb(rms);
                        int    amp = (int) Math.min(100, Math.max(0, (db + 60) * 1.67)); // map -60..0 dB → 0..100
                        mainHandler.post(() -> {
                            binding.pbMicAmplitude.setProgress(amp);
                            
                            // Mic Ready Validation
                            if (amp > 15 && !isMicReady) {
                                isMicReady = true;
                                updatePreFlightChecks();
                            }
                            
                            if (!sessionRunning) return;
                            binding.pbLiveAudio.setProgress(amp);
                            checkSilence(db);
                            checkMumbling(db);
                            
                            // Capture audio amplitude timeline every 1 second
                            if (System.currentTimeMillis() - lastTimelineUpdateMs > 1000) {
                                lastTimelineUpdateMs = System.currentTimeMillis();
                                amplitudeTimeline.add((float) db);
                            }
                        });
                    }
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAudioMonitor() {
        // Already running from pre-flight — VAD checks start after TTS finishes (onDone)
        monitoringAudio = true;
    }

    private void stopAudioMonitor() {
        monitoringAudio = false;
        if (audioRecord != null) {
            try { audioRecord.stop(); audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (audioExecutor != null) audioExecutor.shutdownNow();
    }

    // ── Silence Detection (VAD Endpointing) ─────────────────────────
    private void checkSilence(double db) {
        if (!sessionRunning || binding.tvAiSpeaking.getVisibility() == View.VISIBLE) {
            silenceStartMs = -1; // don't detect silence while AI is speaking
            return;
        }

        // Grace Period Check: ignore silence for 4 seconds after TTS finishes
        if (System.currentTimeMillis() < gracePeriodEndMs) return;

        if (db < -42) {
            // Evaluates Speech Bursts for Filler Words
            if (speechBurstStartMs > 0) {
                long burstDuration = System.currentTimeMillis() - speechBurstStartMs;
                if (burstDuration > 50 && burstDuration < 1500) {
                    // Short burst surrounded by silence = "Umm" or "Like"
                    fillerWordsCount++;
                    binding.tvFillerWords.setText(fillerWordsCount + " 'Umm's");
                    if (fillerWordsCount > 5) {
                        binding.tvFillerWords.setTextColor(getColor(R.color.status_orange));
                    }
                }
                speechBurstStartMs = -1; // Reset burst tracker
            }

            if (silenceStartMs < 0) silenceStartMs = System.currentTimeMillis();
            else if (System.currentTimeMillis() - silenceStartMs >= SILENCE_TIMEOUT) {
                silenceStartMs = -1;
                silenceCount++;
                advanceQuestion();
            }
        } else {
            silenceStartMs = -1; // reset on any speech
            
            // Track burst spikes for filler words
            if (db > -38) { 
                if (speechBurstStartMs < 0) speechBurstStartMs = System.currentTimeMillis();
            } else {
                speechBurstStartMs = -1; // drop sustained talking threshold
            }
        }
    }

    // ── Mumbling Detection (low dB) → Blue border ───────────────────
    private void checkMumbling(double db) {
        if (!sessionRunning) return;
        if (db < -50 && db > SILENCE_DB) {
            setTelemetryBorder(COLOR_BLUE);
        } else {
            setTelemetryBorder(0x00000000);
        }
    }

    // ── Audio Math ───────────────────────────────────────────────────
    private double calculateRms(short[] buffer, int read) {
        long sum = 0;
        for (int i = 0; i < read; i++) sum += (long) buffer[i] * buffer[i];
        return Math.sqrt((double) sum / read);
    }

    private double rmsToDb(double rms) {
        if (rms <= 0) return -100;
        return 20 * Math.log10(rms / 32767.0);
    }

    // ────────────────────────────────────────────────────────────────
    //  PACE TRACKING — WPM Stub
    // ────────────────────────────────────────────────────────────────

    private void startWpmTracking() {
        wpmRunnable = new Runnable() {
            private int prevWordCount = 0;
            @Override public void run() {
                // In production: hook into SpeechRecognizer's partial results
                // Stub: simulate WPM between 80–200 for now
                int newWords = new Random().nextInt(30);
                wordCount += newWords;
                int elapsedMin = (int) Math.max(1, (System.currentTimeMillis() - sessionStartTime) / 60000);
                avgWpm = wordCount / elapsedMin;

                binding.tvWpm.setText(avgWpm + " WPM");

                if (avgWpm > 160) {
                    pulseCardOrange();
                    binding.tvWpm.setTextColor(getColor(R.color.status_orange));
                } else {
                    binding.tvWpm.setTextColor(getColor(R.color.text_secondary));
                }

                if (sessionRunning) wpmHandler.postDelayed(this, 10_000);
            }
        };
        wpmHandler.postDelayed(wpmRunnable, 10_000);
    }

    private void pulseCardOrange() {
        android.animation.ValueAnimator colorAnimation = android.animation.ValueAnimator.ofArgb(
            android.graphics.Color.TRANSPARENT, 0x40FF8C00, android.graphics.Color.TRANSPARENT);
        colorAnimation.setDuration(1500);
        colorAnimation.addUpdateListener(animator -> {
            binding.cardQuestionVault.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf((int) animator.getAnimatedValue()));
        });
        colorAnimation.start();
    }

    // ────────────────────────────────────────────────────────────────
    //  TELEMETRY BORDER ANIMATION
    // ────────────────────────────────────────────────────────────────

    private void setTelemetryBorder(int color) {
        View border = binding.viewTelemetryBorder;
        border.setBackgroundColor(color);
        if (color == 0x00000000) {
            border.animate().alpha(0f).setDuration(600).start();
        } else {
            border.animate().alpha(1f).setDuration(300).start();
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  CHAOS ENGINE
    // ────────────────────────────────────────────────────────────────

    private void scheduleChaosEvent() {
        // Trigger a random distraction between 15–45 seconds from now
        long delay = 15_000 + new Random().nextInt(30_000);
        chaosRunnable = () -> {
            if (!sessionRunning || !chaosEnabled) return;
            playChaosDistraction();
            // Reschedule
            scheduleChaosEvent();
        };
        chaosHandler.postDelayed(chaosRunnable, delay);
    }

    private void playChaosDistraction() {
        chaosStartTime = System.currentTimeMillis();
        
        // 1. Heartbeat Vibration Effect
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Heartbeat pattern: Wait 0, pulse 50ms, rest 100ms, pulse 50ms, rest 800ms
                long[] timings = {0, 50, 100, 50, 800, 50, 100, 50, 800};
                int[] amplitudes = {0, 100, 0, 255, 0, 100, 0, 255, 0};
                VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                vibrator.vibrate(effect);
            } else {
                long[] pattern = {0, 50, 100, 50, 800, 50, 100, 50, 800};
                vibrator.vibrate(pattern, -1); 
            }
        }
        
        // 2. Auditory Chaos with native alarm
        try {
            android.net.Uri defaultRingtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            MediaPlayer mp = MediaPlayer.create(this, defaultRingtoneUri);
            if (mp != null) {
                mp.setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mp.setOnCompletionListener(media -> media.release());
                mp.start();
                mainHandler.postDelayed(() -> {
                    try { if (mp.isPlaying()) { mp.stop(); mp.release(); } } catch (Exception e) {}
                }, 3000); // Blast for 3 seconds only
            }
        } catch (Exception e) {}

        // 3. TTS Interruptions to break concentration
        if (ttsReady && tts != null) {
            String[] distractions = {
                    "Can you speak up?",
                    "Wait, let me stop you there.",
                    "Are you still there?",
                    "I didn't quite catch that. Keep going."
            };
            String distraction = distractions[new Random().nextInt(distractions.length)];
            tts.speak(distraction, TextToSpeech.QUEUE_FLUSH, null, "ChaosID");
        }

        // 3. UI Disruption: Glitching timer bar and Screen Flash
        android.animation.ObjectAnimator glitch = android.animation.ObjectAnimator.ofFloat(binding.pbQuestionTimer, "translationY", 0f, 20f, -20f, 15f, -15f, 0f);
        glitch.setDuration(400);
        glitch.start();
        
        // 4. Red Visceral Flash Overlay
        mainHandler.post(() -> {
            binding.viewChaosFlash.setAlpha(1f);
            binding.viewChaosFlash.animate().alpha(0f).setDuration(800).start();
        });
        
        // Flash chaos indicator
        binding.tvChaosIndicator.setText("⚡ Chaos Triggered!");
        binding.tvChaosIndicator.setTextColor(getColor(R.color.status_red));
        mainHandler.postDelayed(() -> {
            binding.tvChaosIndicator.setText("🔥 Chaos ON");
            binding.tvChaosIndicator.setTextColor(getColor(R.color.status_orange));
            chaosRecoveryTime += System.currentTimeMillis() - chaosStartTime;
            chaosDistractionCount++;
        }, 3000);
    }

    // ────────────────────────────────────────────────────────────────
    //  BREATHING EXERCISE (10s circular animation)
    // ────────────────────────────────────────────────────────────────

    private void triggerBreathingExercise() {
        if (tts != null) tts.stop();
        if (questionTimer != null) questionTimer.cancel();

        binding.cardQuestionVault.setVisibility(View.INVISIBLE);
        binding.flBreathing.setVisibility(View.VISIBLE);

        // Concentric ring pulse animation
        animateBreathingRings();

        breathTimer = new CountDownTimer(10_000, 1000) {
            int phase = 0;
            @Override public void onTick(long millisUntilFinished) {
                // In 4 → hold 4 → out 4 pattern (approx)
                phase++;
            }
            @Override public void onFinish() {
                binding.flBreathing.setVisibility(View.GONE);
                binding.cardQuestionVault.setVisibility(View.VISIBLE);
                // Resume question
                showQuestion(currentQuestionIndex);
            }
        }.start();
    }

    private void animateBreathingRings() {
        ObjectAnimator outerExpand = ObjectAnimator.ofFloat(binding.ringBreathOuter, "scaleX", 1f, 1.5f);
        ObjectAnimator outerExpandY = ObjectAnimator.ofFloat(binding.ringBreathOuter, "scaleY", 1f, 1.5f);
        ObjectAnimator outerContract = ObjectAnimator.ofFloat(binding.ringBreathOuter, "scaleX", 1.5f, 1f);
        ObjectAnimator outerContractY = ObjectAnimator.ofFloat(binding.ringBreathOuter, "scaleY", 1.5f, 1f);

        AnimatorSet expand = new AnimatorSet();
        expand.playTogether(outerExpand, outerExpandY);
        expand.setDuration(4000);

        AnimatorSet contract = new AnimatorSet();
        contract.playTogether(outerContract, outerContractY);
        contract.setDuration(4000);

        AnimatorSet breathing = new AnimatorSet();
        breathing.playSequentially(expand, contract);
        breathing.start();
    }

    // ────────────────────────────────────────────────────────────────
    //  SESSION END — Bundle & Handoff to Stats
    // ────────────────────────────────────────────────────────────────

    private void confirmEndSession() {
        new AlertDialog.Builder(this)
                .setTitle("End Session?")
                .setMessage("Your results will be saved and sent to your Stats dashboard.")
                .setPositiveButton("End Session", (d, w) -> endSession())
                .setNegativeButton("Continue", null)
                .show();
    }

    private void endSession() {
        sessionRunning = false;
        stopAudioMonitor();
        stopRecording();
        if (questionTimer != null) questionTimer.cancel();
        if (tts != null) tts.stop();
        chaosHandler.removeCallbacksAndMessages(null);
        wpmHandler.removeCallbacksAndMessages(null);

        // Stop Camera to save battery/resources
        try {
            androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this).get().unbindAll();
        } catch (Exception e) {}
        binding.cardCameraContainer.setVisibility(View.GONE);

        long totalTimeSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000;
        String durationStr = formatTime(totalTimeSeconds);

        // Populate Results Phase UI
        binding.tvResultsDuration.setText("Duration: " + durationStr);
        binding.tvResultsWpm.setText("Pacing: " + avgWpm + " WPM");
        binding.tvResultsPauses.setText("Pauses (Timeouts): " + silenceCount);
        binding.tvResultsFillers.setText("Filler Words ('Umm'): " + fillerWordsCount);

        // Switch to Phase 3 (Results)
        binding.viewFlipper.setDisplayedChild(2);

        // Wire Return Home -> Analytics Engine
        binding.btnReturnHome.setOnClickListener(v -> {
            float postureStability = totalFaceChecks > 0 ? ((float) successfulFaceChecks / totalFaceChecks) : 1f;
            long avgRecoveryMs = chaosDistractionCount > 0 ? (chaosRecoveryTime / chaosDistractionCount) : 0;
            
            SessionMetrics metrics = new SessionMetrics();
            metrics.avgWpm = avgWpm;
            metrics.chaosDistractionCount = chaosDistractionCount;
            metrics.recoveryTimeMs = avgRecoveryMs;
            metrics.postureStability = postureStability;
            metrics.fillerWordCount = fillerWordsCount;
            metrics.amplitudeTimeline = amplitudeTimeline;
            metrics.videoFilePath = (currentVideoPath != null) ? currentVideoPath : "";
            Log.d("SamvaadVideo", "Session Ended. Video Path: " + metrics.videoFilePath);
            metrics.scenarioTitle = binding.tvScenarioChip.getText().toString();
            metrics.timestamp = System.currentTimeMillis();
            metrics.durationSeconds = totalTimeSeconds;
            
            // Compute Global Score to persist
            float paceScoreVal = 100f;
            if (avgWpm < 130) paceScoreVal -= ((130 - avgWpm) / 5f) * 2f;
            else if (avgWpm > 150) paceScoreVal -= ((avgWpm - 150) / 5f) * 2f;
            paceScoreVal = Math.max(0, Math.min(100, paceScoreVal));

            float resilienceScoreVal = 100f;
            if (avgRecoveryMs > 3000) resilienceScoreVal -= (chaosDistractionCount * 10);
            resilienceScoreVal = Math.max(0, Math.min(100, resilienceScoreVal));

            float presenceScoreVal = postureStability * 100f;
            float globalScoreVal = (paceScoreVal * 0.3f) + (resilienceScoreVal * 0.4f) + (presenceScoreVal * 0.3f);
            
            metrics.overallScore = globalScoreVal;
            metrics.paceScore = paceScoreVal;
            metrics.clarityScore = presenceScoreVal;
            
            // Push to Mock Database (In-Memory)
            metrics.id = MockDatabase.sessionHistory.size() + 1;
            MockDatabase.sessionHistory.add(metrics);

            // Trigger Background Task (Exp: Notifications & WorkManager)
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SessionWorker.class).build();
            WorkManager.getInstance(this).enqueue(syncRequest);

            // Push to Firebase Session Logs (Legacy Support)
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                java.util.Map<String, Object> log = new java.util.HashMap<>();
                log.put("uid", user.getUid());
                log.put("scenario_title", metrics.scenarioTitle);
                log.put("timestamp", metrics.timestamp);
                log.put("duration_total_seconds", totalTimeSeconds);
                log.put("global_score", globalScoreVal);
                log.put("avg_wpm", avgWpm);
                log.put("chaos_count", chaosDistractionCount);
                log.put("filler_words", fillerWordsCount);
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("session_logs")
                        .add(log);
            }
            
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("SESSION_METRICS", metrics);
            intent.setAction("ACTION_SHOW_STATS");
            startActivity(intent);
            finish();
        });
    }

    // ────────────────────────────────────────────────────────────────
    //  IMMERSIVE MODE
    // ────────────────────────────────────────────────────────────────

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startMicAmplitudePreview();
            } else {
                Toast.makeText(this, "Microphone permission is required for the session.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
