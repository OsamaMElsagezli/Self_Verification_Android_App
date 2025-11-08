package com.example.myapp;

public interface SelfieVerifierCallback {
    void onVerificationSuccess();
    void onVerificationFailure(String errorMessage);
}
