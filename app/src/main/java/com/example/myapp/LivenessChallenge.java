package com.example.myapp;

public enum LivenessChallenge {
    CENTER_FACE("Look forward & stay inside the oval", 6, 0, "center"),
    TURN_LEFT   ("Turn head LEFT",                      8, 0, "left"),
    TURN_RIGHT  ("Turn head RIGHT",                     8, 0, "right"),
    BLINK       ("Blink your eyes",                     1, 0, "blink"),
    SMILE       ("Smile",                               4, 0, "smile");

    public static final LivenessChallenge[] DEFAULT_SEQUENCE = new LivenessChallenge[]{
            CENTER_FACE, TURN_LEFT, TURN_RIGHT, BLINK, SMILE
    };

    private final String prompt;
    private final int minHoldFrames;
    private final long timeoutMs; // 0 = disabled
    private final String tag;

    LivenessChallenge(String prompt, int minHoldFrames, long timeoutMs, String tag) {
        this.prompt = prompt;
        this.minHoldFrames = minHoldFrames;
        this.timeoutMs = timeoutMs;
        this.tag = tag;
    }

    public String getPrompt() { return prompt; }
    public int getMinHoldFrames() { return minHoldFrames; }
    public long getTimeoutMs() { return timeoutMs; }
    public String getTag() { return tag; }
}
