package com.example.myapp.feature;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapp.ApiClient;
import com.example.myapp.UploadApi;
import com.example.myapp.UploadResponse;

import java.util.Map;

import android.net.Uri;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Convenience wrapper that wires up Retrofit, calls Uploader.uploadAll,
 * and reports progress via a simple Listener interface (defined below).
 *
 * Passport upload is PAUSED (we pass null in Uploader).
 */
public final class UploadActions {
    private UploadActions(){}

    /** Replace any old references to a missing 'Listener' with this interface. */
    public interface Listener {
        /** Optional progress text like "Uploading..." */
        default void onProgress(@NonNull String message) {}
        /** Called on HTTP 2xx with a non-null body. */
        void onSuccess(@NonNull UploadResponse response);
        /** Called on network error or non-2xx response. */
        void onError(@NonNull String message, @Nullable Throwable t);
    }

    /**
     * Create the API client and upload all provided selfie parts.
     *
     * @param ctx       Android context
     * @param baseUrl   e.g. "http://10.0.2.2:8000/" (must end with '/')
     * @param username  the username to send as text form field
     * @param partsByName keys: "selfie_front","selfie_left","selfie_right","selfie_blink","selfie_smile"
     * @param listener  progress/success/error callbacks
     */
    public static void uploadAll(
            @NonNull Context ctx,
            @NonNull String baseUrl,
            @NonNull String username,
            @NonNull Map<String, Uri> partsByName,
            @NonNull Listener listener
    ) {
        listener.onProgress("Uploading...");

        UploadApi api = ApiClient.create(baseUrl);

        Uploader.uploadAll(
                ctx,
                api,
                username,
                partsByName,
                new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<UploadResponse> call,
                                           @NonNull Response<UploadResponse> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            listener.onSuccess(resp.body());
                        } else {
                            String msg = "Upload failed: HTTP " + resp.code();
                            listener.onError(msg, null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UploadResponse> call,
                                          @NonNull Throwable t) {
                        listener.onError("Upload error: " + t.getMessage(), t);
                    }
                }
        );
    }
}
