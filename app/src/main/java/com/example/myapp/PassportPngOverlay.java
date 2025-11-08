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
 * Draws your PNG overlay and exposes the capture frame as view-fractions.
 *
 * How it works:
 * - The PNG is drawn FIT-CENTER inside the view (preserves aspect ratio).
 * - We define the capture box (big top-right rectangle) as FRACTIONS of the PNG.
 * - We map that box into the view and return fractional bounds for cropping.
 *
 * If the capture box needs a tiny nudge, tweak BOX_* constants below.
 */
public class PassportPngOverlay extends View {

    private Bitmap overlayBmp;

    // ---- Tune these 4 numbers to match the box INSIDE your PNG ----
    // They are fractions of the PNG's intrinsic size (0..1).
    // Defaults are sensible for the sketch you shared; adjust if needed:
    // left column ~15%; big box starting ~22% from left, width ~48%, top ~6%, height ~18%
    private static final float BOX_LEFT_FRAC   = 0.22f; // from left edge of PNG
    private static final float BOX_TOP_FRAC    = 0.06f; // from top edge of PNG
    private static final float BOX_WIDTH_FRAC  = 0.48f; // of PNG width
    private static final float BOX_HEIGHT_FRAC = 0.18f; // of PNG height

    // Remember where the PNG actually draws on screen (fit-center)
    private final RectF dst = new RectF();

    public PassportPngOverlay(Context ctx) { this(ctx, null); }

    public PassportPngOverlay(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        // Load once
        overlayBmp = BitmapFactory.decodeResource(getResources(), R.drawable.passport_outline);
        setWillNotDraw(false);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (overlayBmp == null || overlayBmp.isRecycled()) return;

        final int vw = getWidth(), vh = getHeight();
        final int bw = overlayBmp.getWidth(), bh = overlayBmp.getHeight();
        if (vw <= 0 || vh <= 0 || bw <= 0 || bh <= 0) return;

        // --- FIT-CENTER math ---
        float viewAspect = (float) vw / vh;
        float bmpAspect  = (float) bw / bh;

        if (bmpAspect > viewAspect) {
            // image is wider than view → full width, letterbox vertically
            float drawW = vw;
            float drawH = drawW / bmpAspect;
            float top   = (vh - drawH) * 0.5f;
            dst.set(0f, top, drawW, top + drawH);
        } else {
            // image is taller than view → full height, pillarbox horizontally
            float drawH = vh;
            float drawW = drawH * bmpAspect;
            float left  = (vw - drawW) * 0.5f;
            dst.set(left, 0f, left + drawW, drawH);
        }

        // draw the PNG
        c.drawBitmap(overlayBmp, null, dst, null);
    }

    /**
     * Fractional bounds (0..1) of the capture frame IN THE VIEW coordinate space.
     * This maps your PNG box (defined above) into wherever the PNG is drawn with fit-center.
     */
    public FrameF getCaptureFrameFraction() {
        final int vw = getWidth(), vh = getHeight();
        if (vw <= 0 || vh <= 0 || dst.width() <= 0 || dst.height() <= 0) {
            // fallback: whole view
            return new FrameF(0f, 0f, 1f, 1f);
        }

        float left   = dst.left + BOX_LEFT_FRAC   * dst.width();
        float top    = dst.top  + BOX_TOP_FRAC    * dst.height();
        float right  = left     + BOX_WIDTH_FRAC  * dst.width();
        float bottom = top      + BOX_HEIGHT_FRAC * dst.height();

        // convert to fractions of view
        float fLeft   = left   / vw;
        float fTop    = top    / vh;
        float fWidth  = (right  - left) / vw;
        float fHeight = (bottom - top ) / vh;

        return new FrameF(fLeft, fTop, fWidth, fHeight);
    }

    // API parity with your previous overlay (no-ops kept for compatibility)
    public void setBoxWidthFraction(float ignored) {}
    public void setBottomUiReserveFraction(float ignored) {}

    public static class FrameF {
        public final float left, top, width, height;
        public FrameF(float l, float t, float w, float h) { left = l; top = t; width = w; height = h; }
    }
}
