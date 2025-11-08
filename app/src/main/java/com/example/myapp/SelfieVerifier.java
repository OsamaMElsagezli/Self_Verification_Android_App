package com.example.myapp;

public class SelfieVerifier {
    private final SelfieVerifierCallback callback;

    public SelfieVerifier(SelfieVerifierCallback cb) {
        this.callback = cb;
    }

    public void success() {
        if (callback != null) callback.onVerificationSuccess();
    }

    public void fail(String reason) {
        if (callback != null) callback.onVerificationFailure(reason);
    }
}
