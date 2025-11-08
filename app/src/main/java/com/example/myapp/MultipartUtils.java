package com.example.myapp;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public final class MultipartUtils {
    private MultipartUtils(){}

    public static RequestBody text(String value) {
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    public static MultipartBody.Part fromUri(
            Context ctx,
            String partName,
            String filename,
            String mimeType,
            Uri uri
    ) throws IOException {
        ContentResolver cr = ctx.getContentResolver();
        try (InputStream is = cr.openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) throw new IOException("Cannot open input stream for " + uri);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
            byte[] bytes = bos.toByteArray();
            RequestBody body = RequestBody.create(bytes, MediaType.parse(mimeType));
            return MultipartBody.Part.createFormData(partName, filename, body);
        }
    }

    // Optional bitmap encoders (for future passport resume)
    public static MultipartBody.Part partFromBitmap(
            String partName, String filename, Bitmap bmp, int jpegQuality
    ) throws IOException {
        if (bmp == null) throw new IOException("Bitmap is null for " + partName);
        if (jpegQuality < 1 || jpegQuality > 100) jpegQuality = 92;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bos))
            throw new IOException("Failed to encode JPEG for " + partName);
        byte[] bytes = bos.toByteArray();
        RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
        return MultipartBody.Part.createFormData(partName, filename, body);
    }
}
