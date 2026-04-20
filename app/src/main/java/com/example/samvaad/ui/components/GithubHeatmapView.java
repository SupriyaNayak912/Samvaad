package com.example.samvaad.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GithubHeatmapView — renders a contribution calendar for the last 15 weeks.
 * Call setSessionTimestamps(List<Long>) to populate it with real session data.
 */
public class GithubHeatmapView extends View {

    private static final int WEEKS = 15;
    private static final int DAYS_IN_WEEK = 7;
    private static final float CELL_PADDING_DP = 3f;

    private Paint cellPaint;
    private float cellSize;
    private float cellPadding;

    // Maps "yyyy-DayOfYear" → session count that day
    private final Map<String, Integer> sessionMap = new HashMap<>();

    // Teal gradient levels — 0 = empty, 1-4 = intensity
    private final int[] COLORS = {
        0xFF1A1D36,  // 0: empty / background
        0xFF0D3D2B,  // 1: light
        0xFF0A6642,  // 2: medium-light
        0xFF00A86B,  // 3: medium-strong
        0xFF00E6B8,  // 4: full teal
    };

    public GithubHeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        cellPadding = CELL_PADDING_DP * density;

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setSessionTimestamps(List<Long> timestamps) {
        sessionMap.clear();
        if (timestamps == null) return;
        Calendar cal = Calendar.getInstance();
        for (long ts : timestamps) {
            cal.setTimeInMillis(ts);
            String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
            sessionMap.put(key, sessionMap.getOrDefault(key, 0) + 1);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Calculate cell size to exactly fill width
        cellSize = (w - cellPadding * (WEEKS + 1)) / WEEKS;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float density = getContext().getResources().getDisplayMetrics().density;
        float idealCellSize = (getMeasuredWidth() - CELL_PADDING_DP * density * (WEEKS + 1)) / WEEKS;
        int h = (int) (idealCellSize * DAYS_IN_WEEK + CELL_PADDING_DP * density * (DAYS_IN_WEEK + 1));
        setMeasuredDimension(getMeasuredWidth(), h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cellSize <= 0) return;

        // Start from today and walk backwards WEEKS*7 days
        Calendar today = Calendar.getInstance();
        // Align to column end = today's week
        Calendar walker = (Calendar) today.clone();
        // Walk back to fill grid; col 0 = oldest, col WEEKS-1 = most recent
        int totalDays = WEEKS * DAYS_IN_WEEK;
        walker.add(Calendar.DAY_OF_YEAR, -(totalDays - 1));

        for (int col = 0; col < WEEKS; col++) {
            for (int row = 0; row < DAYS_IN_WEEK; row++) {
                String key = walker.get(Calendar.YEAR) + "-" + walker.get(Calendar.DAY_OF_YEAR);
                int count = sessionMap.getOrDefault(key, 0);
                int colorIdx = count == 0 ? 0 : Math.min(4, count);
                cellPaint.setColor(COLORS[colorIdx]);

                float left = cellPadding + col * (cellSize + cellPadding);
                float top = cellPadding + row * (cellSize + cellPadding);
                RectF rect = new RectF(left, top, left + cellSize, top + cellSize);
                canvas.drawRoundRect(rect, 3f, 3f, cellPaint);

                walker.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }
}
