package com.example.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.myapp.databinding.ActivitySelfieVerifierBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = ExperimentalGetImage.class)
public class SelfieVerifierActivity extends AppCompatActivity implements SelfieVerifierCallback {

    private enum FlowState { RUNNING, FINISHING, FINISHED }

    // -------- Tunables (unchanged) --------
    private static final boolean MIRROR_YAW_FOR_FRONT = true;
    private static final float  CENTER_YAW_ABS_DEG    = 18f;
    private static final float  TURN_DELTA_DEG        = 24f;
    private static final float  ARM_CENTER_DEG        = 10f;
    private static final float  YAW_ALPHA             = 0.85f;
    private static final long   MIN_STEP_VISIBLE_MS   = 1300L;
    private static final long   STEP_HOP_DELAY_MS     = 1200L;

    /** Key used by MainActivity’s launcher to read captured URIs */
    public static final String EXTRA_URIS = "captured_uris";

    private ActivitySelfieVerifierBinding b;

    private ExecutorService analysisExecutor;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private ProcessCameraProvider cameraProvider;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler handler     = new Handler(Looper.getMainLooper());

    // capture tracking
    private final ArrayList<String> capturedUris = new ArrayList<>();
    private int pendingCaptures = 0;

    // flow
    private FlowState flowState = FlowState.RUNNING;

    // ✅ Only these 3 steps now (no blink/smile)
    private final LivenessChallenge[] sequence = new LivenessChallenge[] {
            LivenessChallenge.CENTER_FACE,
            LivenessChallenge.TURN_LEFT,
            LivenessChallenge.TURN_RIGHT
    };
    private int stepIndex = 0;
    private long stepStartMs = 0;
    private int holdCounter = 0;
    private boolean armed = false;
    private volatile boolean stepFrozen = false;

    // signals
    private Float baselineYaw = null;
    private Float smoothYaw   = null;

    // passport step control
    private boolean selfieFlowComplete = false;   // selfies finished
    private boolean passportLaunched   = false;   // scanner started
    private static final long PASSPORT_DELAY_MS = 5000L;

    // ---- permissions / activity results ----
    private final ActivityResultLauncher<String> requestCamPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    // after 5s, we launch this; result returns a single Uri in data.getData()
    private final ActivityResultLauncher<Intent> passportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri passport = result.getData().getData();
                    if (passport != null) capturedUris.add(passport.toString());
                } else {
                    Toast.makeText(this, "Passport capture skipped.", Toast.LENGTH_SHORT).show();
                }
                finalizeAndReturnResult(); // now we’re truly done with everything
            });

    private final LivenessDetectionHelper detection = new LivenessDetectionHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySelfieVerifierBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCamPermission.launch(Manifest.permission.CAMERA);
        } else {
            startCamera();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (analysisExecutor != null) analysisExecutor.shutdownNow();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    private void startCamera() {
        analysisExecutor = Executors.newSingleThreadExecutor();
        ListenableFuture<ProcessCameraProvider> lf = ProcessCameraProvider.getInstance(this);
        lf.addListener(() -> {
            try {
                cameraProvider = lf.get();
                bindUseCases();
                startStep(0);
            } catch (Exception e) {
                Toast.makeText(this, "Camera start error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases() {
        if (cameraProvider == null) return;
        cameraProvider.unbindAll();

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(b.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(analysisExecutor, image -> {
            if (flowState != FlowState.RUNNING) { image.close(); return; }

            LivenessDetectionHelper.Signals s =
                    detection.analyze(image.getImage(), image.getImageInfo().getRotationDegrees());
            image.close();
            if (s == null) return;

            if (MIRROR_YAW_FOR_FRONT && s.yawDeg != null) s.yawDeg = -s.yawDeg;

            if (s.yawDeg != null) {
                if (smoothYaw == null) smoothYaw = s.yawDeg;
                else smoothYaw = YAW_ALPHA * smoothYaw + (1 - YAW_ALPHA) * s.yawDeg;
            }

            if (!stepFrozen) evaluateStepOnSignals(s);
        });

        cameraProvider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalysis);
    }

    private LivenessChallenge current() { return sequence[stepIndex]; }

    private void startStep(int index) {
        stepIndex = index;
        holdCounter = 0;
        armed = false;
        stepFrozen = false;
        stepStartMs = System.currentTimeMillis();
        b.overlayView.setSuccess(false);
        setHint(current().getPrompt());
        if (current() == LivenessChallenge.CENTER_FACE) baselineYaw = null;
    }

    private void nextStep() {
        if (stepIndex + 1 < sequence.length) {
            startStep(stepIndex + 1);
        } else {
            // ✅ All selfie steps are captured. When saves finish, launch passport after 5s.
            flowState = FlowState.FINISHING;
            selfieFlowComplete = true;
            maybeAdvanceAfterSaves();
        }
    }

    // called whenever a capture finishes or when selfie flow ends
    private void maybeAdvanceAfterSaves() {
        if (flowState != FlowState.FINISHING) return;

        if (pendingCaptures == 0) {
            // All selfie images are written. Start passport after 5 seconds (only once).
            if (!passportLaunched) {
                passportLaunched = true;
                handler.postDelayed(() -> {
                    Intent i = new Intent(this, PassportScanActivity.class);
                    passportLauncher.launch(i);
                }, PASSPORT_DELAY_MS);
            }
        }
    }

    /** Final return to caller with ALL URIs (selfies + passport). */
    private void finalizeAndReturnResult() {
        flowState = FlowState.FINISHED;
        try { if (cameraProvider != null) cameraProvider.unbindAll(); } catch (Exception ignored) {}

        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_URIS, capturedUris);
        setResult(RESULT_OK, result);
        finish();
    }

    // -------- robust save: pre-insert, write to that URI, guarantee a usable URI ----------
    private void capturePhotoForStep(String tag) {
        if (imageCapture == null) return;

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis());

        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "selfie_" + tag + "_" + ts + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        Uri dest = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (dest == null) { onCaptureDone(false, null); return; }

        pendingCaptures++;

        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(), dest, v).build();

        imageCapture.takePicture(opts, analysisExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                onCaptureDone(true, dest.toString());
            }
            @Override public void onError(@NonNull ImageCaptureException e) {
                try { getContentResolver().delete(dest, null, null); } catch (Exception ignored) {}
                onCaptureDone(false, null);
            }
        });
    }

    private void onCaptureDone(boolean ok, String uriStr) {
        if (ok && uriStr != null) capturedUris.add(uriStr);
        pendingCaptures = Math.max(0, pendingCaptures - 1);

        // if selfie flow already finished, check if we can schedule passport
        if (selfieFlowComplete) {
            mainHandler.post(this::maybeAdvanceAfterSaves);
        }
    }

    private void setHint(String txt) { mainHandler.post(() -> b.hintText.setText(txt)); }
    private void setOverlaySuccess(boolean success) { mainHandler.post(() -> b.overlayView.setSuccess(success)); }

    private boolean isInsideOval(float cx01, float cy01) {
        if (Float.isNaN(cx01) || Float.isNaN(cy01)) return false;
        int vw = b.previewView.getWidth(), vh = b.previewView.getHeight();
        if (vw == 0 || vh == 0) return false;
        float cx = cx01 * vw, cy = cy01 * vh;
        android.graphics.RectF oval = b.overlayView.getOvalBounds();
        android.graphics.RectF inset = new android.graphics.RectF(oval);
        inset.inset(oval.width() * 0.06f, oval.height() * 0.06f);
        return inset.contains(cx, cy);
    }

    private void evaluateStepOnSignals(LivenessDetectionHelper.Signals s) {
        if (flowState != FlowState.RUNNING) return;

        LivenessChallenge cur = current();

        boolean inside = s.hasFace && isInsideOval(s.normX, s.normY);
        if (!inside) {
            holdCounter = 0;
            setOverlaySuccess(false);
            setHint(getString(R.string.place_face_in_oval));
            return;
        }

        long elapsed = System.currentTimeMillis() - stepStartMs;

        // -------- Arming to prevent instant pass --------
        if (!armed) {
            if (elapsed >= MIN_STEP_VISIBLE_MS) {
                switch (cur) {
                    case CENTER_FACE:
                        armed = true; break;
                    case TURN_LEFT:
                    case TURN_RIGHT:
                        if (smoothYaw != null && baselineYaw != null) {
                            float d = Math.abs(smoothYaw - baselineYaw);
                            armed = (d <= ARM_CENTER_DEG);
                        }
                        break;
                    default:
                        armed = true;
                        break;
                }
            }
            setOverlaySuccess(false);
            setHint(cur.getPrompt());
            return;
        }

        // -------- Pass conditions (consecutive frames) --------
        boolean pass = false;
        Float yaw = smoothYaw;

        switch (cur) {
            case CENTER_FACE:
                pass = (yaw != null && Math.abs(yaw) <= CENTER_YAW_ABS_DEG);
                break;
            case TURN_LEFT:
                if (baselineYaw == null && yaw != null) baselineYaw = yaw;
                if (yaw != null && baselineYaw != null) pass = (yaw <= baselineYaw - TURN_DELTA_DEG);
                break;
            case TURN_RIGHT:
                if (baselineYaw == null && yaw != null) baselineYaw = yaw;
                if (yaw != null && baselineYaw != null) pass = (yaw >= baselineYaw + TURN_DELTA_DEG);
                break;
            default:
                pass = true;
        }

        holdCounter = pass ? (holdCounter + 1) : 0;
        setOverlaySuccess(pass);

        if (holdCounter >= cur.getMinHoldFrames()) {
            if (cur == LivenessChallenge.CENTER_FACE && yaw != null) baselineYaw = yaw;

            setHint("✓ Great");
            setOverlaySuccess(true);

            // capture one photo for THIS step
            capturePhotoForStep(cur.getTag());

            // freeze analysis, then move on / finish selfies
            holdCounter = 0;
            armed = false;
            stepFrozen = true;

            mainHandler.postDelayed(() -> {
                stepFrozen = false;
                nextStep();
            }, STEP_HOP_DELAY_MS);
        } else {
            setHint(cur.getPrompt());
        }
    }

    // ===== SelfieVerifierCallback =====
    @Override public void onVerificationSuccess() { /* handled by the flow */ }

    @Override public void onVerificationFailure(String errorMessage) {
        Toast.makeText(this, "Verification Failed: " + errorMessage, Toast.LENGTH_LONG).show();
        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_URIS, new ArrayList<>());
        setResult(RESULT_CANCELED, result);
        finish();
    }
}
