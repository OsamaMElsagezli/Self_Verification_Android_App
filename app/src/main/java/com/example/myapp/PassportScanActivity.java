package com.example.myapp;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewStub;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.myapp.databinding.ActivityPassportScanBinding;
import com.google.common.util.concurrent.ListenableFuture;

import android.hardware.camera2.CaptureRequest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@OptIn(markerClass = ExperimentalGetImage.class)
public class PassportScanActivity extends AppCompatActivity {

    // TODO: change to your site (folder says you have it). Must end with '/'
    private static final String BASE_URL = "https://YOUR-WEBSITE-BASE-URL/"; // e.g. https://api.example.com/

    private ActivityPassportScanBinding b;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private PreviewView previewView; // inflated from ViewStub (prevents Layout Editor crash)
    private final Deque<String> neededPerms = new ArrayDeque<>();

    private final ActivityResultLauncher<String> requestPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) requestNextPermissionOrStart();
                else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPassportScanBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Inflate PreviewView at runtime so the Layout Editor won’t crash
        ViewStub stub = b.previewStub;
        stub.setLayoutResource(R.layout.include_preview_view);
        stub.inflate();
        previewView = findViewById(R.id.previewView);

        if (b.overlay != null) b.overlay.bringToFront();

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Only CAMERA permission is needed (we save to app cache, not gallery)
        neededPerms.clear();
        neededPerms.add(Manifest.permission.CAMERA);
        requestNextPermissionOrStart();

        b.btnCapture.setOnClickListener(v -> captureAndUpload());
        b.btnBack.setOnClickListener(v -> finish());
    }

    private void requestNextPermissionOrStart() {
        while (!neededPerms.isEmpty()) {
            String p = neededPerms.peekFirst();
            if (ContextCompat.checkSelfPermission(this, p)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                neededPerms.removeFirst();
            } else {
                requestPerm.launch(p);
                return;
            }
        }
        initCamera();
    }

    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(this);
        f.addListener(() -> {
            try {
                cameraProvider = f.get();
                bindUseCases();
            } catch (Exception e) {
                Toast.makeText(this, "Camera start error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (cameraProvider != null) cameraProvider.unbindAll(); } catch (Exception ignored) {}
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void bindUseCases() {
        if (cameraProvider == null || previewView == null) return;
        cameraProvider.unbindAll();

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview with AF/stabilization hints
        Preview.Builder pBuilder = new Preview.Builder();
        new Camera2Interop.Extender<>(pBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                .setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        preview = pBuilder.build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Fast capture
        ImageCapture.Builder cBuilder = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);
        new Camera2Interop.Extender<>(cBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                .setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
        imageCapture = cBuilder.build();

        cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
    }

    /** Capture into app cache (no gallery), then upload to website, then delete temp. */
    private void captureAndUpload() {
        if (imageCapture == null) return;

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        File outFile = new File(getCacheDir(), "passport_" + ts + ".jpg");

        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(outFile).build();

        setUiBusy(true);
        imageCapture.takePicture(opts, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> uploadTempFile(outFile));
            }
            @Override public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    setUiBusy(false);
                    Toast.makeText(PassportScanActivity.this,
                            "Capture error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void uploadTempFile(@NonNull File file) {
        try {
            UploadApi api = ApiClient.create(BASE_URL);

            // Example parts — adjust keys/names to match your server
            RequestBody username = RequestBody.create(
                    MediaType.parse("text/plain"), "demo_user");

            RequestBody fileBody = RequestBody.create(
                    MediaType.parse("image/jpeg"), file);
            MultipartBody.Part filePart =
                    MultipartBody.Part.createFormData("file", file.getName(), fileBody);

            Call<UploadResponse> call = api.uploadPassport(filePart, username);
            call.enqueue(new Callback<UploadResponse>() {
                @Override public void onResponse(@NonNull Call<UploadResponse> call,
                                                 @NonNull Response<UploadResponse> resp) {
                    // delete temp either way
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    setUiBusy(false);

                    if (resp.isSuccessful() && resp.body() != null) {
                        Toast.makeText(PassportScanActivity.this,
                                "Uploaded ✅", Toast.LENGTH_SHORT).show();
                        // Optionally return something to caller
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(PassportScanActivity.this,
                                "Upload failed: " + resp.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                    // delete temp on failure as well
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    setUiBusy(false);
                    Toast.makeText(PassportScanActivity.this,
                            "Upload error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            // delete temp if exception
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            setUiBusy(false);
            Toast.makeText(this, "Upload exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setUiBusy(boolean busy) {
        b.btnCapture.setEnabled(!busy);
        b.btnBack.setEnabled(!busy);
        // If you have a ProgressBar in this layout, toggle it here
        // b.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }
}
