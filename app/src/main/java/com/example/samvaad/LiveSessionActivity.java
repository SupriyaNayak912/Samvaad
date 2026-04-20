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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Toast;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.samvaad.databinding.ActivityLiveSessionBinding;
import java.io.RandomAccessFile;
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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveSessionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, SensorEventListener {

    private static final String TAG = "LiveSessionActivity";

    // ─── ViewBinding ────────────────────────────────────────────────
    private ActivityLiveSessionBinding binding;

    // ─── Session State ───────────────────────────────────────────────
    private List<String> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private boolean chaosEnabled = false;
    private boolean cameraEnabled = true;
    private boolean sessionRunning = false;
    private String targetRole = "General Practice";
    private String scenarioTitle = "Interview Simulator";
    private boolean isDrillMode = false;

    // ─── Telemetry Metrics ──────────────────────────────────────────
    private long sessionStartTime;
    private int silenceCount = 0;
    private int wordCount = 0; // cumulative (stub)
    private int avgWpm = 0;
    private long chaosRecoveryTime = 0;
    private long chaosStartTime = 0;

    // ─── TTS ────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean isTtsReady = false; // Hardened status check

    // ─── Audio Monitoring ────────────────────────────────────────────
    private AudioRecord audioRecord;
    private ExecutorService audioExecutor;
    private volatile boolean monitoringAudio = false;
    private volatile boolean isAiSpeaking = false;
    private static final int SAMPLE_RATE = 44100;
    private static final int SILENCE_DB = -48; // more sensitive to low speech
    private static final long SILENCE_TIMEOUT = 4500L; // 4.5 s silence → next question (Hands-Free)
    private long silenceStartMs = -1;
    private int fillerWordsCount = 0;

    // ── File Transcription (Whisper API) ───────────────────────────
    private File wavFile;
    private RandomAccessFile wavAccessFile;
    private long wavPayloadSize = 0;

    private List<QnAPair> sessionTranscript = new ArrayList<>();
    private int lastSavedQuestionIndex = -1;
    private long speechBurstStartMs = -1;

    // ─── Analytics Metrics ───────────────────────────────────────────
    private int totalFaceChecks = 0;
    private int successfulFaceChecks = 0;
    private int chaosDistractionCount = 0;
    private ArrayList<Float> amplitudeTimeline = new ArrayList<>();
    private long lastTimelineUpdateMs = 0;
    private long lastQuestionLevelSwitchMs = 0;
    private final ArrayList<Float> sessionAmplitudeHistory = new ArrayList<>();

    // ─── Timers & Handlers ───────────────────────────────────────────
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private CountDownTimer questionTimer;
    private int questionDurationSec = 90; // Dynamically adjusted (90s default, 60s drill)

    // Chaos Handler
    private Handler chaosHandler = new Handler(Looper.getMainLooper());
    private Runnable chaosRunnable;

    // WPM stub interval (every 10 s)
    private Handler wpmHandler = new Handler(Looper.getMainLooper());
    private Runnable wpmRunnable;

    private SessionMetrics currentMetrics = new SessionMetrics();

    // Breathing countdown
    private CountDownTimer breathTimer;

    // Gestures
    private android.view.GestureDetector gestureDetector;

    // Telemetry border colors
    private static final int COLOR_ORANGE = 0x80FF8C00; // 50% orange — fast WPM
    private static final int COLOR_BLUE = 0x803B82F6; // 50% blue — mumbling

    // ─── Camera, ML Kit, and Validations ────────────────────────────────
    private FaceDetector faceDetector;
    private boolean isCameraReady = false;
    private boolean isMicReady = false;
    private boolean isFaceReady = false;
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
    // LIFECYCLE
    // ────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupGestures();

        loadScenarioFromIntent();
        resetSessionData();
        initTts();
        setupPreFlight();
        initSensors();
        setupDraggablePiP();
    }

    private void resetSessionData() {
        sessionTranscript.clear();
        amplitudeTimeline.clear();
        sessionAmplitudeHistory.clear();
        silenceCount = 0;
        fillerWordsCount = 0;
        chaosDistractionCount = 0;
        totalFaceChecks = 0;
        successfulFaceChecks = 0;
        currentQuestionIndex = 0;
        isAiSpeaking = false;
        wavPayloadSize = 0;
        if (wavFile != null && wavFile.exists()) {
            wavFile.delete();
        }
    }

    private void setupDraggablePiP() {
        // Initial setup matching the Pre-Flight placeholder
        binding.cardCameraContainer.post(() -> {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            float density = getResources().getDisplayMetrics().density;

            // Pre-flight size: Match the XML placeholder (approx 250dp height)
            ViewGroup.LayoutParams lp = binding.cardCameraContainer.getLayoutParams();
            lp.width = (int) (screenWidth - (48 * density)); // 24dp margins
            lp.height = (int) (250 * density);
            binding.cardCameraContainer.setLayoutParams(lp);

            // Position at original placeholder spot
            binding.cardCameraContainer.setX(24 * density);
            binding.cardCameraContainer.setY(binding.viewCameraPlaceholder.getY() + (56 * density)); // adjust for top
                                                                                                     // padding
        });

        binding.cardCameraContainer.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
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
        gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
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
                    public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX,
                            float velocityY) {
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

    private void initWavFile() {
        try {
            wavFile = new java.io.File(getCacheDir(), "session_audio_" + System.currentTimeMillis() + ".wav");
            if (wavFile.exists())
                wavFile.delete();
            wavAccessFile = new java.io.RandomAccessFile(wavFile, "rw");
            byte[] header = new byte[44];
            wavAccessFile.write(header); // Write placeholder header
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWavHeader() {
        try {
            if (wavAccessFile == null)
                return;
            long totalAudioLen = wavPayloadSize;
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = SAMPLE_RATE;
            int channels = 1;
            long byteRate = 16 * SAMPLE_RATE * channels / 8;

            wavAccessFile.seek(0);
            byte[] header = new byte[44];
            header[0] = 'R';
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f';
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (2 * 16 / 8);
            header[33] = 0;
            header[34] = 16;
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
            wavAccessFile.write(header);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioMonitor();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (questionTimer != null)
            questionTimer.cancel();
        if (breathTimer != null)
            breathTimer.cancel();
        chaosHandler.removeCallbacksAndMessages(null);
        wpmHandler.removeCallbacksAndMessages(null);
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // INTENT — load StringArrayList Extra
    // ────────────────────────────────────────────────────────────────

    private void loadScenarioFromIntent() {
        Intent intent = getIntent();
        Scenario scenario = (Scenario) intent.getSerializableExtra("scenario");
        ArrayList<String> extraQuestions = intent.getStringArrayListExtra("EXTRA_QUESTIONS");

        if (scenario != null && scenario.getQuestions() != null && !scenario.getQuestions().isEmpty()) {
            questions.clear();
            questions.addAll(scenario.getQuestions());
            binding.tvScenarioChip.setText(scenario.getCategory() + " · " + scenario.getDifficulty());
        } else if (extraQuestions != null && !extraQuestions.isEmpty()) {
            questions.clear();
            questions.addAll(extraQuestions);
            String role = intent.getStringExtra("EXTRA_ROLE");
            if (role != null && !role.isEmpty()) {
                binding.tvScenarioChip.setText(role + " Interview");
            } else {
                binding.tvScenarioChip.setText("Role-Based Interview");
            }
        } else {
            // Fallback: generic questions
            questions.clear();
            questions.add("Tell me about yourself and your professional journey.");
            questions.add("What is your greatest strength, and how have you used it?");
            questions.add("Describe a challenge you faced and how you overcame it.");
            binding.tvScenarioChip.setText("General · Default");
        }

        isDrillMode = intent.getBooleanExtra("isDrillMode", false);
        if (scenario != null) {
            targetRole = scenario.getCategory() != null ? scenario.getCategory() : "General Practice";
            scenarioTitle = scenario.getTitle() != null ? scenario.getTitle() : "Interview Prep";
        } else {
            targetRole = intent.getStringExtra("EXTRA_ROLE");
            if (targetRole == null) targetRole = "General Practice";
            scenarioTitle = "Dynamic Session";
        }

        if (isDrillMode) {
            questionDurationSec = 60; // 1-minute drill
            binding.tvScenarioChip.setText("🔥 1-Min Drill");
            targetRole = "Daily Drill";
            scenarioTitle = "Elevator Pitch";
        } else {
            // Only shuffle for real sessions, Drills have exactly 1 fixed Elevator Pitch
            // question
            Collections.shuffle(questions);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // TTS INIT
    // ────────────────────────────────────────────────────────────────

    private void initTts() {
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // ── Humanized Voice Selection ─────────────────────────────────
            // Find the best available high-quality network voice
            android.speech.tts.Voice bestVoice = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                java.util.Set<android.speech.tts.Voice> voices = tts.getVoices();
                if (voices != null) {
                    int bestScore = -1;
                    for (android.speech.tts.Voice v : voices) {
                        if (!v.isNetworkConnectionRequired())
                            continue; // prefer network (higher quality)
                        String name = v.getName().toLowerCase();
                        int score = 0;
                        // Prioritise human-sounding locales + quality
                        if (name.contains("en-gb") || name.contains("en_gb"))
                            score += 8;
                        else if (name.contains("en-au") || name.contains("en_au"))
                            score += 6;
                        else if (name.contains("en-in") || name.contains("en_in"))
                            score += 4;
                        else if (name.contains("en"))
                            score += 2;
                        score += (v.getQuality() / 100); // 0-5 bonus
                        if (score > bestScore) {
                            bestScore = score;
                            bestVoice = v;
                        }
                    }
                    // Fallback: best non-network en voice
                    if (bestVoice == null) {
                        for (android.speech.tts.Voice v : voices) {
                            String name = v.getName().toLowerCase();
                            if (name.contains("en") && v.getQuality() >= android.speech.tts.Voice.QUALITY_NORMAL) {
                                bestVoice = v;
                                break;
                            }
                        }
                    }
                }
            }
            if (bestVoice != null) {
                tts.setVoice(bestVoice);
            } else {
                tts.setLanguage(Locale.UK); // fallback locale with natural intonation
            }
            tts.setSpeechRate(0.88f); // slightly slower = more natural interviewer cadence
            tts.setPitch(0.97f); // slightly lower pitch = warmer, less robotic
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    isAiSpeaking = true;
                    mainHandler.post(() -> binding.tvAiSpeaking.setVisibility(View.VISIBLE));
                }

                @Override
                public void onError(String utteranceId, int error) {
                    isAiSpeaking = false;
                }

                @Override
                public void onDone(String utteranceId) {
                    isAiSpeaking = false;
                    mainHandler.post(() -> {
                        binding.tvAiSpeaking.setVisibility(View.GONE);
                        binding.llSoundWave.setVisibility(View.GONE);
                        stopSoundWaveAnimation();
                        binding.tvSilenceHint.setVisibility(View.VISIBLE);
                        binding.tvSilenceHint.setText("⏳ Listening...");
                        // Start 4-second grace period before VAD evaluates silence
                        gracePeriodEndMs = System.currentTimeMillis() + 4000L;
                        silenceStartMs = -1;
                        // Show inline Next button after AI finishes speaking
                        binding.btnNextQuestion.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    isAiSpeaking = false;
                    mainHandler.post(() -> binding.tvAiSpeaking.setVisibility(View.GONE));
                }
            });
            isTtsReady = true;
            mainHandler.post(this::updatePreFlightChecks);

            // Wire Inline Next Button click → manual skip
            binding.btnNextQuestion.setOnClickListener(v -> {
                if (sessionRunning) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    binding.btnNextQuestion.setEnabled(false);
                    binding.btnNextQuestion.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                        binding.btnNextQuestion.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    }).start();
                    // Save current transcript before advancing
                    saveCurrentAnswer();
                    advanceQuestion();
                }
            });
        }
    }

    private void speakQuestion(String text) {
        binding.tvAiSpeaking.setVisibility(View.VISIBLE);
        binding.llSoundWave.setVisibility(View.VISIBLE);
        startSoundWaveAnimation();
        binding.tvSilenceHint.setVisibility(View.GONE);
        binding.btnNextQuestion.setVisibility(View.INVISIBLE);
        binding.btnNextQuestion.setEnabled(true);
        
        if (isTtsReady && tts != null) {
            Bundle params = new Bundle();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // High-fidelity speech attributes
                android.media.AudioAttributes attr = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                tts.setAudioAttributes(attr);
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "question_" + currentQuestionIndex);
        } else {
            // TTS not ready yet (Speech Bug fix) -> Wait and retry
            Log.d(TAG, "TTS not ready, retrying speakQuestion...");
            mainHandler.postDelayed(() -> speakQuestion(text), 800);
        }
    }

    private List<ObjectAnimator> waveAnimators = new ArrayList<>();

    private void startSoundWaveAnimation() {
        stopSoundWaveAnimation();
        for (int i = 0; i < binding.llSoundWave.getChildCount(); i++) {
            View bar = binding.llSoundWave.getChildAt(i);
            ObjectAnimator anim = ObjectAnimator.ofFloat(bar, "scaleY", 0.5f, 1.5f);
            anim.setDuration(300 + (i * 100));
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.start();
            waveAnimators.add(anim);
        }
    }

    private void stopSoundWaveAnimation() {
        for (ObjectAnimator anim : waveAnimators)
            anim.cancel();
        waveAnimators.clear();
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void pauseSessionForReset() {
        if (!sessionRunning)
            return;
        sessionRunning = false;
        if (questionTimer != null)
            questionTimer.cancel();
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
                        if (activeRecording != null)
                            activeRecording.resume();
                        showQuestion(currentQuestionIndex);
                    } else {
                        Toast.makeText(this, "Still not aligned!", Toast.LENGTH_SHORT).show();
                        pauseSessionForReset(); // recursive until aligned
                    }
                })
                .show();
    }

    // ────────────────────────────────────────────────────────────────
    // PRE-FLIGHT SETUP (Phase 1)
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
                isFaceReady = true; // Bypassed
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
            chaosEnabled = binding.switchChaos.isChecked();
            cameraEnabled = binding.switchCamera.isChecked();
            beginSession();
        });

        // Integrated Camera Card Following Logic (Phase 1)
        // Keeps the root-level card pinned to its placeholder in the scroll list
        binding.scrollPreflight.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener) (view, scrollX, scrollY, oldX, oldY) -> {
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

            if (isMicReady && isCameraReady && isFaceReady && isOrientationReady && isTtsReady) {
                binding.btnBeginScenario.setEnabled(true);
                binding.btnBeginScenario.setAlpha(1.0f);
            } else {
                binding.btnBeginScenario.setEnabled(false);
                binding.btnBeginScenario.setAlpha(0.5f);
            }
        });
    }

    private void startCameraSetup() {
        if (!binding.switchCamera.isChecked())
            return;

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
                    @SuppressLint("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage,
                                imageProxy.getImageInfo().getRotationDegrees());
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
                                        if (!faces.isEmpty())
                                            successfulFaceChecks++;
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
                        .setQualitySelector(
                                QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
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
    // SESSION START — transition to Phase 2
    // ────────────────────────────────────────────────────────────────

    private void beginSession() {
        enableImmersiveMode();
        sessionRunning = true;
        sessionStartTime = System.currentTimeMillis();
        currentQuestionIndex = 0;
        initWavFile();

        // Immediate Firestore Persistence (The 'That Very Second' feature)
        currentMetrics.targetRole = targetRole;
        currentMetrics.scenarioTitle = scenarioTitle;
        currentMetrics.sessionMode = isDrillMode ? "drill" : "standard";
        SessionRepository.startNewSession(currentMetrics, new SessionRepository.SessionCallback() {
            @Override public void onSuccess(String id) { 
                Log.d(TAG, "Draft session created: " + id);
                currentMetrics.firestoreId = id; 
            }
            @Override public void onFailure(Exception e) { Log.e(TAG, "Draft failed", e); }
        });

        // Take Audio Focus to suppress background music
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(focusChange -> {
            },
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        }

        // Animate the Camera to a Picture-in-Picture at the Bottom Right
        androidx.cardview.widget.CardView cameraCard = binding.cardCameraContainer;
        if (cameraCard != null) {
            float targetScale = 0.45f; // Increasde size (from 0.32f) for better visibility

            cameraCard.setPivotX(0f);
            cameraCard.setPivotY(0f);

            cameraCard.post(() -> {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                float finalWidth = cameraCard.getWidth() * targetScale;
                float finalHeight = cameraCard.getHeight() * targetScale;

                // Position bottom-right
                float targetX = screenWidth - finalWidth - 48f - cameraCard.getLeft();
                float targetY = screenHeight - finalHeight - 240f - cameraCard.getTop();

                cameraCard.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .translationX(targetX)
                        .translationY(targetY)
                        .setDuration(800)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withStartAction(() -> {
                            cameraCard.setRadius(24 * getResources().getDisplayMetrics().density); // subtle rounded
                                                                                                   // corners
                            binding.cardCameraContainer.setTranslationY(0);
                        })
                        .start();
            });
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

        // Hide Next button for "Exact Simulation" feel (advances via silence)
        binding.btnNextQuestion.setVisibility(View.GONE);

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
    // QUESTION DISPLAY & TIMER
    // ────────────────────────────────────────────────────────────────

    private void showQuestion(int index) {
        if (index >= questions.size()) {
            endSession();
            return;
        }

        String question = questions.get(index);
        binding.tvQuestionText.setText(question);
        // Progress Update
        int progressPercent = (int) (((float) (index + 1) / questions.size()) * 100);
        binding.cpbQuestionProgress.setProgressWithAnimation(progressPercent, 800L);
        binding.tvQuestionIndexSmall.setText((index + 1) + "/" + questions.size());

        // Entrance animation
        binding.cardQuestionVault.setAlpha(0f);
        binding.cardQuestionVault.animate().alpha(1f).setDuration(400).start();

        // AI speaks the question
        speakQuestion(question);

        // Reset & start question countdown timer
        if (questionTimer != null)
            questionTimer.cancel();
        binding.pbQuestionTimer.setMax(questionDurationSec);
        binding.pbQuestionTimer.setProgress(questionDurationSec);

        questionTimer = new CountDownTimer(questionDurationSec * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int remaining = (int) (millisUntilFinished / 1000);
                binding.pbQuestionTimer.setProgress(remaining);
                binding.tvSessionTimer.setText(formatTime((System.currentTimeMillis() - sessionStartTime) / 1000));
            }

            @Override
            public void onFinish() {
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
        // Essential Logic Fix: Save current response BEFORE moving next!
        saveCurrentAnswer();

        // Play subtle ding
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_NOTIFICATION, 100);
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception e) {
        }

        // Telegraph the skip so the user isn't jarred
        binding.tvSilenceHint.setVisibility(View.VISIBLE);
        binding.tvSilenceHint.setTextColor(getColor(R.color.status_orange));
        binding.tvSilenceHint.setText("Advancing...");

        mainHandler.postDelayed(() -> {
            binding.tvSilenceHint.setTextColor(getColor(R.color.text_secondary));
            currentQuestionIndex++;
            lastQuestionLevelSwitchMs = System.currentTimeMillis(); // Track switch time
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
    // AUDIO MONITORING — Amplitude + VAD (Silence Detection)
    // ────────────────────────────────────────────────────────────────

    private void startMicAmplitudePreview() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.RECORD_AUDIO }, 1001);
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
                        if (sessionRunning && wavAccessFile != null && !isAiSpeaking) {
                            try {
                                byte[] byteBuffer = new byte[read * 2];
                                for (int i = 0; i < read; i++) {
                                    byteBuffer[i * 2] = (byte) (buffer[i] & 0xFF);
                                    byteBuffer[i * 2 + 1] = (byte) ((buffer[i] >> 8) & 0xFF);
                                }
                                wavAccessFile.write(byteBuffer);
                                wavPayloadSize += byteBuffer.length;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        double rms = calculateRms(buffer, read);
                        double db = rmsToDb(rms);
                        int amp = (int) Math.min(100, Math.max(0, (db + 60) * 1.67)); // map -60..0 dB → 0..100
                        mainHandler.post(() -> {
                            binding.pbMicAmplitude.setProgress(amp);

                            // Mic Ready Validation
                            if (amp > 15 && !isMicReady) {
                                isMicReady = true;
                                updatePreFlightChecks();
                            }

                            if (!sessionRunning)
                                return;
                            binding.pbLiveAudio.setProgress(amp);
                            checkSilence(db);
                            checkMumbling(db);

                            // Capture audio amplitude timeline every 1 second
                            if (System.currentTimeMillis() - lastTimelineUpdateMs > 1000) {
                                lastTimelineUpdateMs = System.currentTimeMillis();
                                amplitudeTimeline.add((float) db);
                                sessionAmplitudeHistory.add((float) db);
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
        // Already running from pre-flight — VAD checks start after TTS finishes
        // (onDone)
        monitoringAudio = true;
    }

    private void stopAudioMonitor() {
        monitoringAudio = false;
        try {
            updateWavHeader();
            if (wavAccessFile != null)
                wavAccessFile.close();
            wavAccessFile = null;
        } catch (Exception ignored) {
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception ignored) {
            }
            audioRecord = null;
        }
        if (audioExecutor != null)
            audioExecutor.shutdownNow();
    }

    // ── Silence Detection (VAD Endpointing) ─────────────────────────
    private void checkSilence(double db) {
        if (!sessionRunning || binding.tvAiSpeaking.getVisibility() == View.VISIBLE) {
            silenceStartMs = -1; // don't detect silence while AI is speaking
            return;
        }

        // Grace Period Check: ignore silence for 4 seconds after TTS finishes
        if (System.currentTimeMillis() < gracePeriodEndMs)
            return;

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

            if (silenceStartMs < 0)
                silenceStartMs = System.currentTimeMillis();
            else if (System.currentTimeMillis() - silenceStartMs >= SILENCE_TIMEOUT) {
                silenceStartMs = -1;
                silenceCount++;

                // We no longer add directly here.
                // advanceQuestion() will call saveCurrentAnswer() safely.
                advanceQuestion();
            }
        } else {
            silenceStartMs = -1; // reset on any speech

            // Track burst spikes for filler words
            if (db > -38) {
                if (speechBurstStartMs < 0)
                    speechBurstStartMs = System.currentTimeMillis();
            } else {
                speechBurstStartMs = -1; // drop sustained talking threshold
            }
        }
    }

    // ── Mumbling Detection (low dB) → Blue border ───────────────────
    private void checkMumbling(double db) {
        if (!sessionRunning)
            return;
        if (db < -50 && db > SILENCE_DB) {
            setTelemetryBorder(COLOR_BLUE);
        } else {
            setTelemetryBorder(0x00000000);
        }
    }

    // ── Audio Math ───────────────────────────────────────────────────
    private double calculateRms(short[] buffer, int read) {
        long sum = 0;
        for (int i = 0; i < read; i++)
            sum += (long) buffer[i] * buffer[i];
        return Math.sqrt((double) sum / read);
    }

    private double rmsToDb(double rms) {
        if (rms <= 0)
            return -100;
        return 20 * Math.log10(rms / 32767.0);
    }

    // ────────────────────────────────────────────────────────────────
    // PACE TRACKING — WPM Stub
    // ────────────────────────────────────────────────────────────────

    private void startWpmTracking() {
        wpmRunnable = new Runnable() {
            private int prevWordCount = 0;

            @Override
            public void run() {
                // In production: hook into SpeechRecognizer's partial results
                // Stub: simulate WPM between 80–200 for now
                int newWords = new Random().nextInt(30);
                wordCount += newWords;
                int elapsedMin = (int) Math.max(1, (System.currentTimeMillis() - sessionStartTime) / 60000);
                avgWpm = wordCount / elapsedMin;

                binding.tvWpm.setText(avgWpm + " WPM");

                // Dynamic coloring for premium feedback (Live Coaching)
                if (avgWpm > 175) { // Rushing
                    binding.tvWpm.setTextColor(getColor(R.color.status_red));
                    pulseCardOrange(); // Visual stress indicator
                } else if (avgWpm > 130) { // Golden Zone
                    binding.tvWpm.setTextColor(getColor(R.color.status_green));
                } else { // Deliberate
                    binding.tvWpm.setTextColor(getColor(R.color.text_secondary));
                }

                if (sessionRunning)
                    wpmHandler.postDelayed(this, 10_000);
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
    // TELEMETRY BORDER ANIMATION
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
    // CHAOS ENGINE
    // ────────────────────────────────────────────────────────────────

    private void scheduleChaosEvent() {
        // Trigger a random distraction between 15–45 seconds from now
        long delay = 15_000 + new Random().nextInt(30_000);
        chaosRunnable = () -> {
            if (!sessionRunning || !chaosEnabled)
                return;

            // Contextual check: don't interrupt right after a question switch (within 5s)
            long timeSinceSwitch = System.currentTimeMillis() - lastQuestionLevelSwitchMs;
            if (silenceStartMs < 0 && timeSinceSwitch > 5000) {
                playChaosDistraction();
                scheduleChaosEvent();
            } else {
                // Retry in 3 seconds
                chaosHandler.postDelayed(chaosRunnable, 3000);
            }
        };
        chaosHandler.postDelayed(chaosRunnable, delay);
    }

    private void playChaosDistraction() {
        chaosStartTime = System.currentTimeMillis();

        // 1. Flash chaos indicator UI
        mainHandler.post(() -> {
            binding.tvChaosIndicator.setText("⚡ Interruption!");
            binding.tvChaosIndicator.setTextColor(getColor(R.color.status_red));
        });

        mainHandler.postDelayed(() -> {
            binding.tvChaosIndicator.setText("🔥 Chaos ON");
            binding.tvChaosIndicator.setTextColor(getColor(R.color.status_orange));
            chaosRecoveryTime += System.currentTimeMillis() - chaosStartTime;
            chaosDistractionCount++;
        }, 3000);

        // 2. AI TTS Interruptions to meddle in between
        if (isTtsReady && tts != null) {
            String[] distractions = {
                    "Sorry to interrupt, can you elaborate on that?",
                    "Wait, let me stop you there.",
                    "Are you sure about that approach?",
                    "I didn't quite catch that. Keep going.",
                    "Can we pivot for a second? Nevermind, finish your thought."
            };
            String distraction = distractions[new Random().nextInt(distractions.length)];

            // Flush queue so it interrupts them immediately
            tts.speak(distraction, TextToSpeech.QUEUE_FLUSH, null, "ChaosID");

            mainHandler.post(() -> {
                binding.tvAiSpeaking.setVisibility(View.VISIBLE);
                binding.tvAiSpeaking.setText("Interviewer interjecting...");
                binding.llSoundWave.setVisibility(View.VISIBLE);
                startSoundWaveAnimation();
            });
        }
    }

    // ────────────────────────────────────────────────────────────────
    // BREATHING EXERCISE (10s circular animation)
    // ────────────────────────────────────────────────────────────────

    private void triggerBreathingExercise() {
        if (tts != null)
            tts.stop();
        if (questionTimer != null)
            questionTimer.cancel();

        binding.cardQuestionVault.setVisibility(View.INVISIBLE);
        binding.flBreathing.setVisibility(View.VISIBLE);

        // Concentric ring pulse animation
        animateBreathingRings();

        breathTimer = new CountDownTimer(10_000, 1000) {
            int phase = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                // In 4 → hold 4 → out 4 pattern (approx)
                phase++;
            }

            @Override
            public void onFinish() {
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
    // SESSION END — Bundle & Handoff to Stats
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
        if (questionTimer != null)
            questionTimer.cancel();
        if (tts != null)
            tts.stop();
        chaosHandler.removeCallbacksAndMessages(null);
        wpmHandler.removeCallbacksAndMessages(null);

        binding.btnNextQuestion.setVisibility(View.GONE);

        // Stop Camera to save battery/resources
        try {
            androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this).get().unbindAll();
        } catch (Exception e) {
        }
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

        // Switch to Phase 3 (Results)
        binding.viewFlipper.setDisplayedChild(2);
        binding.btnReturnHome.setEnabled(false);
        binding.btnReturnHome.setText("Preparing Transcripts...");

        // Layer 6: AI-Powered Analysis
        float postureStability = totalFaceChecks > 0 ? ((float) successfulFaceChecks / totalFaceChecks) : 1f;
        
        currentMetrics.telemetry.avgWpm = avgWpm;
        currentMetrics.telemetry.chaosDistractionCount = chaosDistractionCount;
        currentMetrics.telemetry.recoveryTimeMs = chaosDistractionCount > 0 ? (chaosRecoveryTime / chaosDistractionCount) : 0;
        currentMetrics.telemetry.postureStability = postureStability;
        currentMetrics.telemetry.fillerWordCount = fillerWordsCount;
        currentMetrics.telemetry.silenceCount = silenceCount;
        currentMetrics.telemetry.totalFaceChecks = totalFaceChecks;
        currentMetrics.telemetry.successfulFaceChecks = successfulFaceChecks;
        currentMetrics.transcript = sessionTranscript;
        currentMetrics.scenarioTitle = binding.tvScenarioChip.getText().toString();
        currentMetrics.timestamp = new java.util.Date();
        currentMetrics.durationSeconds = totalTimeSeconds;
        currentMetrics.chaosEnabled = chaosEnabled;
        currentMetrics.videoFilePath = currentVideoPath;
        currentMetrics.amplitudeTimeline = new ArrayList<>(sessionAmplitudeHistory);

        currentMetrics.targetRole = getIntent().hasExtra("EXTRA_ROLE") ? getIntent().getStringExtra("EXTRA_ROLE")
                : "General Practice";
        currentMetrics.sessionGoal = getIntent().hasExtra("EXTRA_GOAL") ? getIntent().getStringExtra("EXTRA_GOAL")
                : "Interview Prep";
        currentMetrics.sessionMode = getIntent().hasExtra("EXTRA_MODE") ? getIntent().getStringExtra("EXTRA_MODE")
                : (getIntent().getBooleanExtra("isDrillMode", false) ? "drill" : "vault");

        // 1. Initial Local Calculation (Used as baseline only)
        ScoreResult scores = ScoringEngine.calculate(currentMetrics);

        // 2. AI Coaching Analysis (Whisper + Llama 3) - COMPLETELY OVERWRITES math scores
        saveCurrentAnswer(); 
        uploadToWhisper(currentMetrics, scores);
    }

    private void uploadToWhisper(SessionMetrics metrics, ScoreResult scores) {
        binding.tvResultsTitle.setText("Transcribing Audio...");
        binding.tvResultsWpm.setText("Groq Whisper is converting your speech to text.");
        binding.tvResultsPauses.setText("This guarantees perfect analysis.");
        binding.tvResultsFillers.setText("Please wait 3-5 seconds...");

        if (wavFile == null || !wavFile.exists()) {
            runLlama3Analysis(metrics, scores, "");
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/wav"), wavFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", wavFile.getName(), requestFile);
        RequestBody model = RequestBody.create(MediaType.parse("text/plain"), "whisper-large-v3-turbo");
        RequestBody format = RequestBody.create(MediaType.parse("text/plain"), "json");

        GroqApiClient.getApiService().transcribeAudio(body, model, format)
                .enqueue(new Callback<GroqApiClient.WhisperResponse>() {
                    @Override
                    public void onResponse(Call<GroqApiClient.WhisperResponse> call,
                            Response<GroqApiClient.WhisperResponse> response) {
                        String transcript = "";
                        if (response.isSuccessful() && response.body() != null) {
                            transcript = response.body().text;
                            Log.d("SamvaadWhisper", "Transcription success: " + transcript);
                        } else {
                            Log.e("SamvaadWhisper", "Transcription failed. Code: " + response.code());
                        }
                        runLlama3Analysis(metrics, scores, transcript);
                    }

                    @Override
                    public void onFailure(Call<GroqApiClient.WhisperResponse> call, Throwable t) {
                        Log.e("SamvaadWhisper", "Network error reaching Whisper API", t);
                        runLlama3Analysis(metrics, scores, "");
                    }
                });
    }

    private void runLlama3Analysis(SessionMetrics metrics, ScoreResult scores, String transcript) {
        binding.tvResultsTitle.setText("Evaluating Responses...");
        binding.tvResultsWpm.setText("Llama-3-70B is analyzing your performance...");
        binding.tvResultsPauses.setText("Contextualizing WPM, Filler usage & Composure.");
        binding.tvResultsFillers.setText("");

        SessionSummary summary = SessionSummary.from(metrics, scores, "current_user_uid");
        summary.masterTranscript = transcript;

        LlmFeedbackEngine.generateFeedback(summary, new LlmFeedbackEngine.FeedbackCallback() {
            @Override
            public void onSuccess(LlmFeedback feedback) {
                binding.tvResultsTitle.setText("AI Coaching Summary");
                binding.tvResultsDuration.setText("Overall SRI: " + feedback.getOverallScore() + "/100");
                binding.tvResultsWpm.setText(feedback.getSummary());

                // Populate Strengths (Vertical Insight Cards)
                if (feedback.getStrengths() != null && !feedback.getStrengths().isEmpty()) {
                    FeedbackCarouselAdapter strengthsAdapter = new FeedbackCarouselAdapter(feedback.getStrengths(),
                            "🌟");
                    binding.rvStrengths.setAdapter(strengthsAdapter);
                    binding.tvStrengthsLabel.setVisibility(View.VISIBLE);
                    binding.rvStrengths.setVisibility(View.VISIBLE);
                } else {
                    binding.tvStrengthsLabel.setVisibility(View.GONE);
                    binding.rvStrengths.setVisibility(View.GONE);
                }

                // Populate Focus Areas (Vertical Insight Cards)
                java.util.List<String> focusAreas = new java.util.ArrayList<>();
                if (feedback.getAreasToImprove() != null)
                    focusAreas.addAll(feedback.getAreasToImprove());
                if (feedback.getCoachingTip() != null)
                    focusAreas.add("TIP: " + feedback.getCoachingTip());

                if (!focusAreas.isEmpty()) {
                    FeedbackCarouselAdapter focusAdapter = new FeedbackCarouselAdapter(focusAreas, "🎯");
                    binding.rvFocusAreas.setAdapter(focusAdapter);
                    binding.tvFocusLabel.setVisibility(View.VISIBLE);
                    binding.rvFocusAreas.setVisibility(View.VISIBLE);
                } else {
                    binding.tvFocusLabel.setVisibility(View.GONE);
                    binding.rvFocusAreas.setVisibility(View.GONE);
                }

                // Populate Question-wise Breakdown
                if (feedback.getQuestionAnalysis() != null && !feedback.getQuestionAnalysis().isEmpty()) {
                    binding.tvBreakdownLabel.setVisibility(View.VISIBLE);
                    binding.rvQuestionBreakdown.setVisibility(View.VISIBLE);
                    QuestionAnalysisAdapter qaAdapter = new QuestionAnalysisAdapter(feedback.getQuestionAnalysis());
                    binding.rvQuestionBreakdown.setAdapter(qaAdapter);
                } else {
                    binding.tvBreakdownLabel.setVisibility(View.GONE);
                    binding.rvQuestionBreakdown.setVisibility(View.GONE);
                }

                binding.btnReturnHome.setEnabled(true);
                binding.btnReturnHome.setText("Finish & Save");

                binding.btnReturnHome.setOnClickListener(v -> {
                    binding.btnReturnHome.setEnabled(false);
                    metrics.llmFeedback = feedback;
                    // UNIFY SCORES: Overwrite local math entirely with AI generated scores
                    metrics.overallScore = feedback.getOverallScore();
                    metrics.paceScore = feedback.getPaceScore();
                    metrics.clarityScore = feedback.getClarityScore();

                    android.content.SharedPreferences prefs = getSharedPreferences("SamvaadPrefs", MODE_PRIVATE);
                    float oldBest = prefs.getFloat("latest_global_score", 0f);
                    LevelSystem.Level oldLevel = LevelSystem.getLevelForScore(oldBest);
                    LevelSystem.Level newLevel = LevelSystem.getLevelForScore(feedback.getOverallScore());

                    if (newLevel.id > oldLevel.id) {
                        new LevelUpBottomSheet(newLevel, () -> {
                            saveSessionAndFinish(metrics);
                        }).show(getSupportFragmentManager(), "LevelUp");
                    } else {
                        saveSessionAndFinish(metrics);
                    }

                    if (feedback.getOverallScore() > oldBest) {
                        prefs.edit().putFloat("latest_global_score", (float) feedback.getOverallScore()).apply();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (isFinishing())
                    return;
                binding.tvResultsWpm.setText("AI Analysis timed out. Technical metrics are still available.");
                binding.btnReturnHome.setEnabled(true);
                binding.btnReturnHome.setText("Save (Metrics Only)");
                binding.btnReturnHome.setOnClickListener(v -> {
                    // Even without AI, we save what we know (Pace, Silence, Chaos)
                    saveSessionAndFinish(metrics);
                });
            }
        });
    }

    private void saveSessionAndFinish(SessionMetrics metrics) {
        androidx.appcompat.app.AlertDialog progressDialog = new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.CustomAlertDialog)
                .setTitle("Finalizing Session")
                .setMessage("Uploading your performance analysis to the cloud...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        SessionRepository.saveSession(metrics, new SessionRepository.SessionCallback() {
            @Override
            public void onSuccess(String id) {
                if (isFinishing())
                    return;

                // Trigger background sync and notification
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SessionWorker.class).build();
                WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);

                progressDialog.dismiss();
                purgeLocalVideo();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                if (isFinishing())
                    return;
                progressDialog.dismiss();

                new androidx.appcompat.app.AlertDialog.Builder(LiveSessionActivity.this, R.style.CustomAlertDialog)
                        .setTitle("Cloud Sync Failed")
                        .setMessage(
                                "We couldn't reach the database. Would you like to try again or discard this session?")
                        .setPositiveButton("Retry", (dialog, which) -> saveSessionAndFinish(metrics))
                        .setNegativeButton("Discard", (dialog, which) -> {
                            purgeLocalVideo();
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void purgeLocalVideo() {
        // Delete massive storage consumption
        if (currentVideoPath != null) {
            java.io.File file = new java.io.File(currentVideoPath);
            if (file.exists()) {
                file.delete();
            }
        }
        if (wavFile != null && wavFile.exists()) {
            wavFile.delete();
        }
    }

    // ────────────────────────────────────────────────────────────────
    // IMMERSIVE MODE
    // ────────────────────────────────────────────────────────────────

    private void saveCurrentAnswer() {
        if (questions != null && currentQuestionIndex < questions.size()) {
            if (currentQuestionIndex == lastSavedQuestionIndex)
                return; // Prevent duplicates

            String qText = questions.get(currentQuestionIndex);
            sessionTranscript.add(new QnAPair(qText, "[Transcribed via Whisper]"));
            lastSavedQuestionIndex = currentQuestionIndex;
        }
    }

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
