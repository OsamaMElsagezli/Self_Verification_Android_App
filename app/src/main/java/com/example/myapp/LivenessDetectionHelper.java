package com.example.myapp;

import android.media.Image;

import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@ExperimentalGetImage
public class LivenessDetectionHelper {
    // ... same imports & class header ...

        private static final float EYE_CLOSED_THRESH = 0.35f;
        private static final float SMILE_THRESH      = 0.58f; // was 0.65f

    private final FaceDetector detector;

    public LivenessDetectionHelper() {
        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();
        detector = FaceDetection.getClient(opts);
    }

    /** Synchronous analysis helper; returns Signals or null on no image. */
    @Nullable
    public Signals analyze(@Nullable Image image, int rotationDeg) {
        if (image == null) return null;

        InputImage ii = InputImage.fromMediaImage(image, rotationDeg);
        final Signals[] out = new Signals[1];
        CountDownLatch latch = new CountDownLatch(1);

        detector.process(ii)
                .addOnSuccessListener(faces -> {
                    out[0] = toSignals(faces, ii.getWidth(), ii.getHeight());
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    out[0] = new Signals(); // no face
                    latch.countDown();
                });

        try { latch.await(); } catch (InterruptedException ignored) { }
        return out[0];
    }

    private Signals toSignals(List<Face> faces, int w, int h) {
        Signals s = new Signals();
        if (faces == null || faces.isEmpty()) {
            s.hasFace = false;
            return s;
        }

        // pick largest face
        Face best = faces.get(0);
        float bestArea = 0f;
        for (Face f : faces) {
            float a = f.getBoundingBox().width() * f.getBoundingBox().height();
            if (a > bestArea) { bestArea = a; best = f; }
        }

        s.hasFace = true;

        // normalized center 0..1
        float cx = best.getBoundingBox().centerX() / (float) w;
        float cy = best.getBoundingBox().centerY() / (float) h;
        s.normX = clamp01(cx);
        s.normY = clamp01(cy);

        // ✅ yaw is primitive float; no null check
        s.yawDeg = best.getHeadEulerAngleY();

        // probabilities (these are @Nullable Floats)
        Float le = best.getLeftEyeOpenProbability();
        Float re = best.getRightEyeOpenProbability();
        Float sp = best.getSmilingProbability();

        s.leftEyeOpenProb  = (le == null ? Float.NaN : le);
        s.rightEyeOpenProb = (re == null ? Float.NaN : re);
        s.smileProb        = (sp == null ? Float.NaN : sp);

        boolean leftClosed  = !Float.isNaN(s.leftEyeOpenProb)  && s.leftEyeOpenProb  < EYE_CLOSED_THRESH;
        boolean rightClosed = !Float.isNaN(s.rightEyeOpenProb) && s.rightEyeOpenProb < EYE_CLOSED_THRESH;
        s.blink = leftClosed && rightClosed;

        s.smile = !Float.isNaN(s.smileProb) && s.smileProb > SMILE_THRESH;

        return s;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static class Signals {
        public boolean hasFace = false;
        public float normX = Float.NaN; // 0..1
        public float normY = Float.NaN; // 0..1
        @Nullable public Float yawDeg = null; // assigned from primitive; autoboxed

        // derived
        public boolean blink = false;
        public boolean smile = false;

        // debug
        public float leftEyeOpenProb = Float.NaN;
        public float rightEyeOpenProb = Float.NaN;
        public float smileProb = Float.NaN;
    }
}
