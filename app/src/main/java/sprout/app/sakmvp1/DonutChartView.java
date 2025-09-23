package sprout.app.sakmvp1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

public class DonutChartView extends View {
    private Paint paintProgress;
    private Paint paintBackground;
    private RectF rectF;
    private float progress = 75f; // 기본값 75%
    private float strokeWidth = 20f;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 진행률 페인트 (파란색)
        paintProgress = new Paint();
        paintProgress.setAntiAlias(true);
        paintProgress.setStyle(Paint.Style.STROKE);
        paintProgress.setStrokeWidth(strokeWidth);
        paintProgress.setStrokeCap(Paint.Cap.ROUND);
        paintProgress.setColor(0xFF2196F3); // 파란색

        // 배경 페인트 (회색)
        paintBackground = new Paint();
        paintBackground.setAntiAlias(true);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setStrokeWidth(strokeWidth);
        paintBackground.setColor(0xFFE0E0E0); // 연한 회색
        paintBackground.setAlpha(50); // 투명도 설정

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - (int)strokeWidth;

        // 중심점
        int centerX = width / 2;
        int centerY = height / 2;

        // 도넛 차트가 그려질 영역 설정
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // 배경 원 그리기 (전체 360도)
        canvas.drawArc(rectF, 0, 360, false, paintBackground);

        // 진행률 원 그리기 (상단부터 시계방향으로)
        float sweepAngle = (progress / 100f) * 360f;
        canvas.drawArc(rectF, -90, sweepAngle, false, paintProgress);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate(); // 다시 그리기
    }

    public float getProgress() {
        return progress;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        paintProgress.setStrokeWidth(strokeWidth);
        paintBackground.setStrokeWidth(strokeWidth);
        invalidate();
    }

    public void setProgressColor(int color) {
        paintProgress.setColor(color);
        invalidate();
    }
}