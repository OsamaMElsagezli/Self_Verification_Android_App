package com.example.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class OverlayView extends View {

    private final Paint ovalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private boolean success = false;

    public OverlayView(Context c) { super(c); init(); }
    public OverlayView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public OverlayView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        ovalPaint.setStyle(Paint.Style.STROKE);
        ovalPaint.setStrokeWidth(8f);
        ovalPaint.setColor(0xFFFFFFFF); // white

        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(14f);
        checkPaint.setColor(0xFF2E7D32); // green
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // make an oval centered, 70% width, 55% height
        float ow = w * 0.70f;
        float oh = h * 0.55f;
        float left = (w - ow) / 2f;
        float top  = (h - oh) / 2f;
        oval.set(left, top, left + ow, top + oh);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        // darken outside (optional): skipped to keep simple

        // oval
        c.drawOval(oval, ovalPaint);

        // success check
        if (success) {
            Path p = new Path();
            float cx = oval.centerX();
            float cy = oval.centerY();
            float r = Math.min(oval.width(), oval.height()) * 0.25f;
            p.moveTo(cx - r * 0.6f, cy);
            p.lineTo(cx - r * 0.1f, cy + r * 0.5f);
            p.lineTo(cx + r * 0.7f, cy - r * 0.5f);
            c.drawPath(p, checkPaint);
        }
    }

    public void setSuccess(boolean s) {
        if (this.success != s) {
            this.success = s;
            invalidate();
        }
    }

    public RectF getOvalBounds() {
        return new RectF(oval);
    }
}
