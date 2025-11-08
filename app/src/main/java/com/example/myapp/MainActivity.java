package com.example.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapp.databinding.ActivityMainBinding;
import com.example.myapp.feature.Uploader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // TODO: set to your website API base; MUST end with a slash
    private static final String BASE = "http://10.0.2.2:8000/";

    // Must match SelfieVerifierActivity
    public static final String EXTRA_URIS = "captured_uris";

    // OPTIONAL: send images to another installed app after upload (keep null to show chooser)
    private static final @Nullable String TARGET_EXTERNAL_APP_PACKAGE = null;

    private ActivityMainBinding b;

    private final ActivityResultLauncher<String> requestReadImagesPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) onReadyToShowThumbs();
                else toast("Permission denied. Thumbnails/collage may not work.");
            });

    private final ActivityResultLauncher<Intent> selfieFlowLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    // Show thumbnails / put passport into shape (asks READ permission if needed)
                    ensureReadPermissionThenShow(data);
                    // Upload to your website
                    uploadIfPossible(data);
                } else {
                    toast("Verification was cancelled.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.startVerificationButton.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, SelfieVerifierActivity.class);
            selfieFlowLauncher.launch(i);
        });

        b.retryUploadButton.setOnClickListener(v -> {
            Object tag = b.retryUploadButton.getTag();
            if (tag instanceof Intent) uploadIfPossible((Intent) tag);
            else toast("Nothing to retry yet.");
        });
    }

    /* ---------- Thumbs / Collage / Passport shape ---------- */

    private void ensureReadPermissionThenShow(@NonNull Intent data) {
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            onReadyToShowThumbs(data);
        } else {
            b.retryUploadButton.setTag(data);
            requestReadImagesPermission.launch(perm);
        }
    }

    private void onReadyToShowThumbs() {
        Object tag = b.retryUploadButton.getTag();
        if (tag instanceof Intent) onReadyToShowThumbs((Intent) tag);
    }

    private void onReadyToShowThumbs(@NonNull Intent data) {
        ArrayList<String> uris = data.getStringArrayListExtra(EXTRA_URIS);
        if (uris == null || uris.isEmpty()) {
            toast("No images returned from verifier.");
            return;
        }

        // 1) Find passport and show it in the passport shape
        Uri passportUri = findPassportUri(uris);
        if (b.passportShape != null && passportUri != null) {
            b.passportShape.setImageURI(passportUri);
        }

        // 2) Order our four images (front/left/right/passport) and show thumbs if you have 4 slots
        Map<String, Uri> ordered = orderFour(uris);
        int idx = 0;
        for (Uri u : ordered.values()) {
            setThumbAt(idx++, u); // assumes you have iv1..iv4; ignore extra safely
            if (idx >= 4) break;
        }

        // 3) Collage is optional; if you want a 2x2 collage from the four:
        try {
            Uri collage = composeAndSaveCollage2x2(ordered);
            b.collageStatus.setText(collage != null ? "Collage saved: " + collage : "Collage not created.");
        } catch (Exception e) {
            Log.e("MainActivity", "Collage error", e);
            b.collageStatus.setText("Collage error: " + e.getMessage());
        }
    }

    private Uri findPassportUri(@NonNull ArrayList<String> uris) {
        for (String s : uris) {
            if (s != null && s.toLowerCase().contains("passport")) return Uri.parse(s);
        }
        // fallback: often the last is passport
        return Uri.parse(uris.get(uris.size() - 1));
    }

    private void setThumbAt(int index, Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap square = squareCenterCrop(bmp, 320);
            switch (index) {
                case 0: if (b.iv1 != null) b.iv1.setImageBitmap(square); break;
                case 1: if (b.iv2 != null) b.iv2.setImageBitmap(square); break;
                case 2: if (b.iv3 != null) b.iv3.setImageBitmap(square); break;
                case 3: if (b.iv4 != null) b.iv4.setImageBitmap(square); break;
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Thumb decode failed", e);
        }
    }

    private static Bitmap squareCenterCrop(@NonNull Bitmap src, int size) {
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2, y = (h - side) / 2;
        Bitmap cropped = Bitmap.createBitmap(src, x, y, side, side);
        return Bitmap.createScaledBitmap(cropped, size, size, true);
    }

    private Uri composeAndSaveCollage2x2(@NonNull Map<String, Uri> ordered) throws IOException {
        final int cell = 360, cols = 2, rows = 2;
        final int w = cell * cols, h = cell * rows;

        Bitmap canvasBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        int i = 0;
        for (Uri u : ordered.values()) {
            Bitmap thumb = squareCenterCrop(MediaStore.Images.Media.getBitmap(getContentResolver(), u), cell);
            int col = i % cols, row = i / cols;
            canvas.drawBitmap(thumb, col * cell, row * cell, paint);
            if (++i >= cols * rows) break;
        }
        return saveBitmapToGallery(canvasBmp, "selfie_passport_collage");
    }

    private Uri saveBitmapToGallery(@NonNull Bitmap bmp, @NonNull String baseName) throws IOException {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, baseName + "_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver cr = getContentResolver();
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri dest = cr.insert(collection, v);
        if (dest == null) return null;

        try (OutputStream os = cr.openOutputStream(dest)) {
            if (os == null || !bmp.compress(Bitmap.CompressFormat.JPEG, 92, os))
                throw new IOException("Failed to encode JPEG");
        }

        v.clear();
        v.put(MediaStore.Images.Media.IS_PENDING, 0);
        cr.update(dest, v, null, null);
        return dest;
    }

    /** Only the four we care about: selfie_front, selfie_left, selfie_right, passport */
    private static Map<String, Uri> orderFour(@NonNull ArrayList<String> uris) {
        Map<String, Uri> map = new LinkedHashMap<>(4);
        // pass 1: pick labeled ones
        for (String s : uris) {
            Uri u = Uri.parse(s);
            String name = s.toLowerCase();
            if (!map.containsKey("selfie_front") && (name.contains("front") || name.contains("center")))
                map.put("selfie_front", u);
            else if (!map.containsKey("selfie_left") && name.contains("left"))
                map.put("selfie_left", u);
            else if (!map.containsKey("selfie_right") && name.contains("right"))
                map.put("selfie_right", u);
            else if (!map.containsKey("passport") && name.contains("passport"))
                map.put("passport", u);
        }
        // pass 2: fill missing from leftover uris (order preserved)
        for (String s : uris) {
            if (map.size() >= 4) break;
            Uri u = Uri.parse(s);
            if (!map.containsValue(u)) {
                if (!map.containsKey("selfie_front")) map.put("selfie_front", u);
                else if (!map.containsKey("selfie_left")) map.put("selfie_left", u);
                else if (!map.containsKey("selfie_right")) map.put("selfie_right", u);
                else if (!map.containsKey("passport")) map.put("passport", u);
            }
        }
        return map;
    }

    /* ---------- Uploading / Transfer to website ---------- */

    private void uploadIfPossible(@NonNull Intent data) {
        ArrayList<String> uris = data.getStringArrayListExtra(EXTRA_URIS);
        if (uris == null || uris.isEmpty()) {
            toast("No images to upload.");
            return;
        }
        b.retryUploadButton.setTag(data);

        UploadApi api = ApiClient.create(BASE);
        String username = "demo_user";

        // Only these four:
        Map<String, Uri> ordered = orderFour(uris);

        b.uploadStatus.setText("Uploading...");
        b.progress.setVisibility(View.VISIBLE);

        // NOTE: Ensure your Uploader expects these exact keys, or map them inside Uploader.
        Uploader.uploadAll(
                this,
                api,
                username,
                ordered, // selfie_front / selfie_left / selfie_right / passport
                new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> resp) {
                        b.progress.setVisibility(View.GONE);
                        if (resp.isSuccessful() && resp.body() != null && "ok".equalsIgnoreCase(resp.body().status)) {
                            b.uploadStatus.setText("Upload OK" + (resp.body().batch_id != null ? (" • Batch " + resp.body().batch_id) : ""));
                            toast("Uploaded successfully.");

                            // Optional: also share the images to another app
                            // sendToOtherApp(ordered, TARGET_EXTERNAL_APP_PACKAGE);
                        } else {
                            String msg = "Upload failed: " + resp.code();
                            b.uploadStatus.setText(msg);
                            toast(msg);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                        b.progress.setVisibility(View.GONE);
                        String msg = "Upload error: " + t.getMessage();
                        b.uploadStatus.setText(msg);
                        toast(msg);
                    }
                }
        );
    }

    /** Share images to another app if you need (optional). */
    private void sendToOtherApp(@NonNull Map<String, Uri> ordered, @Nullable String targetPackage) {
        ArrayList<Uri> list = new ArrayList<>(ordered.values());
        if (list.isEmpty()) {
            toast("No images to send.");
            return;
        }

        final Intent share;
        if (list.size() == 1) {
            share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, list.get(0));
        } else {
            share = new Intent(Intent.ACTION_SEND_MULTIPLE);
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        }
        share.setType("image/jpeg");
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (targetPackage != null && !targetPackage.isEmpty()) {
            share.setPackage(targetPackage);
        }

        startActivity(Intent.createChooser(share, "Send images"));
    }

    private void toast(@NonNull String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
