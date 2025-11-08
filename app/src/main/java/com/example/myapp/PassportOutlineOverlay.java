package com.example.myapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * PNG-backed overlay that preserves your previous class name & API.
 * - Draws passport_outline.png FIT-CENTER over the camera.
 * - Returns the big capture box as FRACTIONS of the View via getCaptureFrameFraction().
 * - You can set the box by exact PNG pixels with setBoxByPx(...).
 *
 * PNG source size you gave: 376 x 577 (w x h).
 */
public class PassportOutlineOverlay extends View {

    private Bitmap overlayBmp;

    // ---- Capture box inside the PNG (fractions 0..1 of PNG width/height) ----
    // Start with reasonable defaults; you can refine via setBoxByPx().
    private float boxLeftF   = 0.30f;  // ~ left 113px of 376
    private float boxTopF    = 0.09f;  // ~ top  52px of 577
    private float boxWidthF  = 0.56f;  // ~ width 210px of 376
    private float boxHeightF = 0.23f;  // ~ height132px of 577

    // Where the PNG is actually rendered in view coords (FIT-CENTER)
    private final RectF dst = new RectF();

    public PassportOutlineOverlay(Context ctx) { this(ctx, null); }
    public PassportOutlineOverlay(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        overlayBmp = BitmapFactory.decodeResource(getResources(), R.drawable.passport_outline);
        setWillNotDraw(false);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (overlayBmp == null || overlayBmp.isRecycled()) return;

        computeDstRect();
        c.drawBitmap(overlayBmp, null, dst, null); // the PNG artwork itself
    }

    /** Fractional bounds (0..1) of the capture frame IN VIEW SPACE. */
    public FrameF getCaptureFrameFraction() {
        if (getWidth() <= 0 || getHeight() <= 0 || overlayBmp == null) {
            return new FrameF(0f, 0f, 1f, 1f);
        }
        computeDstRect();

        float left   = dst.left + boxLeftF  * dst.width();
        float top    = dst.top  + boxTopF   * dst.height();
        float right  = left     + boxWidthF * dst.width();
        float bottom = top      + boxHeightF* dst.height();

        float vw = getWidth(), vh = getHeight();
        return new FrameF(left / vw, top / vh, (right - left) / vw, (bottom - top) / vh);
    }

    // ---- Compatibility no-ops (keeps your existing calls compiling) ----
    public void setBoxWidthFraction(float ignored) {}
    public void setBottomUiReserveFraction(float ignored) {}

    // ---- Optional: set the box by EXACT PNG pixels (you know PNG is 376x577) ----
    public void setBoxByPx(int leftPx, int topPx, int rightPx, int bottomPx) {
        // Clamp and convert to fractions
        int bw = 376, bh = 577; // from you
        leftPx   = Math.max(0, Math.min(bw, leftPx));
        rightPx  = Math.max(0, Math.min(bw, rightPx));
        topPx    = Math.max(0, Math.min(bh, topPx));
        bottomPx = Math.max(0, Math.min(bh, bottomPx));
        if (rightPx <= leftPx || bottomPx <= topPx) return;

        boxLeftF   = leftPx   / (float) bw;
        boxTopF    = topPx    / (float) bh;
        boxWidthF  = (rightPx - leftPx) / (float) bw;
        boxHeightF = (bottomPx - topPx) / (float) bh;
        invalidate();
    }

    private void computeDstRect() {
        final int vw = getWidth(), vh = getHeight();
        if (vw <= 0 || vh <= 0 || overlayBmp == null) { dst.setEmpty(); return; }

        final int bw = overlayBmp.getWidth(), bh = overlayBmp.getHeight();
        if (bw <= 0 || bh <= 0) { dst.setEmpty(); return; }

        float viewAspect = (float) vw / vh;
        float bmpAspect  = (float) bw / bh;

        if (bmpAspect > viewAspect) {
            float drawW = vw;
            float drawH = drawW / bmpAspect;
            float top   = (vh - drawH) * 0.5f;
            dst.set(0f, top, drawW, top + drawH);
        } else {
            float drawH = vh;
            float drawW = drawH * bmpAspect;
            float left  = (vw - drawW) * 0.5f;
            dst.set(left, 0f, left + drawW, drawH);
        }
    }

    // Same inner type your old code used
    public static class FrameF {
        public final float left, top, width, height;
        public FrameF(float l, float t, float w, float h) { left = l; top = t; width = w; height = h; }
    }
}
