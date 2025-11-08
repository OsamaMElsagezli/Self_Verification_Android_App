package com.example.myapp.feature;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapp.MultipartUtils;
import com.example.myapp.UploadApi;
import com.example.myapp.UploadResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public final class Uploader {
    private Uploader(){}

    /** Passport is PAUSED by request — we pass null for now. */
    public static void uploadAll(
            @NonNull Context ctx,
            @NonNull UploadApi api,
            @NonNull String username,
            @NonNull Map<String, Uri> partsByName,
            @NonNull Callback<UploadResponse> callback
    ) {
        RequestBody usernameBody = MultipartUtils.text(username);

        MultipartBody.Part pFront   = partOrNull(ctx, "selfie_front", "selfie_front.jpg", "image/jpeg", partsByName.get("selfie_front"));
        MultipartBody.Part pLeft    = partOrNull(ctx, "selfie_left",  "selfie_left.jpg",  "image/jpeg", partsByName.get("selfie_left"));
        MultipartBody.Part pRight   = partOrNull(ctx, "selfie_right", "selfie_right.jpg", "image/jpeg", partsByName.get("selfie_right"));
        MultipartBody.Part pBlink   = partOrNull(ctx, "selfie_blink", "selfie_blink.jpg", "image/jpeg", partsByName.get("selfie_blink"));
        MultipartBody.Part pSmile   = partOrNull(ctx, "selfie_smile", "selfie_smile.jpg", "image/jpeg", partsByName.get("selfie_smile"));

        // Passport PAUSED:
        MultipartBody.Part pPassport = null; // TODO when you resume: build from bitmap/uri

        api.upload(usernameBody, pFront, pLeft, pRight, pBlink, pSmile, pPassport).enqueue(callback);
    }

    private static @Nullable MultipartBody.Part partOrNull(
            @NonNull Context ctx,
            @NonNull String partName,
            @NonNull String filename,
            @NonNull String mime,
            @Nullable Uri uri
    ) {
        try {
            if (uri == null) return null;
            return MultipartUtils.fromUri(ctx, partName, filename, mime, uri);
        } catch (Exception e) {
            Log.e("Uploader", "Building part failed for " + partName, e);
            return null;
        }
    }
}
