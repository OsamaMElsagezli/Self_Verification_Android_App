package com.example.myapp;

public class UploadResponse {
    public String status;   // e.g. "ok"
    public String url;      // optional: server returns file URL
    public String message;  // optional
    public String batch_id; // optional (if your backend uses it)
}
