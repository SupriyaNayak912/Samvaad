package com.example.samvaad.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PremiumLineChartView extends View {
    private List<Float> dataPoints = new ArrayList<>();
    private Paint linePaint;
    private Paint fillPaint;
    private Paint circlePaint;
    private Path linePath;
    private Path fillPath;

    public PremiumLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00E6B8")); // Teal Accent
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.FILL);

        linePath = new Path();
        fillPath = new Path();
        
        // Default Mock Data in case none provided
        dataPoints.add(45f);
        dataPoints.add(60f);
        dataPoints.add(55f);
        dataPoints.add(78f);
        dataPoints.add(85f);
    }

    public void setData(List<Float> points) {
        this.dataPoints.clear();
        this.dataPoints.addAll(points);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fillPaint.setShader(new LinearGradient(
                0, 0, 0, h,
                Color.parseColor("#6600E6B8"),
                Color.parseColor("#0000E6B8"),
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.isEmpty() || dataPoints.size() == 1) return;

        int width = getWidth();
        int height = getHeight();
        float padding = 20f;
        
        float usableWidth = width - 2 * padding;
        float usableHeight = height - 2 * padding;

        float maxData = Collections.max(dataPoints);
        float minData = Math.min(0, Collections.min(dataPoints)); 
        if (maxData == minData) maxData += 10;
        
        float xStep = usableWidth / (dataPoints.size() - 1);
        
        linePath.reset();
        fillPath.reset();
        
        float[] xCoords = new float[dataPoints.size()];
        float[] yCoords = new float[dataPoints.size()];

        for (int i = 0; i < dataPoints.size(); i++) {
            float val = dataPoints.get(i);
            float normalized = (val - minData) / (maxData - minData);
            float x = padding + (i * xStep);
            float y = padding + (usableHeight - (normalized * usableHeight));
            
            xCoords[i] = x;
            yCoords[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, height);
                fillPath.lineTo(x, y);
            } else {
                // Determine control points for smooth bezier curve
                float prevX = xCoords[i - 1];
                float prevY = yCoords[i - 1];
                float cx1 = prevX + (x - prevX) / 2f;
                float cy1 = prevY;
                float cx2 = prevX + (x - prevX) / 2f;
                float cy2 = y;
                
                linePath.cubicTo(cx1, cy1, cx2, cy2, x, y);
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y);
            }
        }

        // Close the fill path
        fillPath.lineTo(xCoords[xCoords.length - 1], height);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Draw points
        for (int i = 0; i < xCoords.length; i++) {
            canvas.drawCircle(xCoords[i], yCoords[i], 8f, circlePaint);
        }
    }
}
