package br.com.jefferson.totemsga.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import br.com.jefferson.totemsga.R;

public class FlowGradientView extends View {

    private Paint paint;
    private Path path;
    private LinearGradient shader;
    private Matrix matrix;
    private float offset = 0f;
    private ValueAnimator animator;
    private boolean isTop = true;
    private SessionManager sessionManager;

    private static final int[] COLORS = {
            Color.parseColor("#FFCC00"), // Amarelo
            Color.parseColor("#F47B20"), // Laranja
            Color.parseColor("#E31E24"), // Vermelho
            Color.parseColor("#FFCC00")  // Loop
    };

    public FlowGradientView(Context context) {
        this(context, null);
    }

    public FlowGradientView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowGradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        sessionManager = new SessionManager(getContext());

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FlowGradientView);
            isTop = a.getBoolean(R.styleable.FlowGradientView_isTop, true);
            a.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();
        matrix = new Matrix();

        int speedConfig = sessionManager.getFlowSpeed();
        long duration = 4000;
        if (speedConfig == 0) duration = 8000;
        else if (speedConfig == 2) duration = 2000;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            offset = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePath(w, h);
        updateShader(w, h);
    }

    private void updatePath(int w, int h) {
        path.reset();
        float sw = w / 400f;
        float sh = h / 200f;

        if (isTop) {
            path.moveTo(0, 0);
            path.lineTo(w, 0);
            path.lineTo(w, 100 * sh);
            path.cubicTo(350 * sw, 150 * sh, 250 * sw, 50 * sh, 150 * sw, 150 * sh);
            path.cubicTo(100 * sw, 200 * sh, 50 * sw, 150 * sh, 0, 100 * sh);
            path.close();
        } else {
            path.moveTo(0, h);
            path.lineTo(w, h);
            path.lineTo(w, 100 * sh);
            path.cubicTo(300 * sw, 50 * sh, 200 * sw, 180 * sh, 100 * sw, 120 * sh);
            path.cubicTo(50 * sw, 90 * sh, 0, 130 * sh, 0, 150 * sh);
            path.close();
        }
    }

    private void updateShader(int w, int h) {
        int direction = sessionManager.getFlowDirection();
        if (direction >= 2) { // Cima -> Baixo ou Baixo -> Cima (Vertical)
            shader = new LinearGradient(
                    0, 0, 0, h,
                    new int[]{COLORS[0], COLORS[1], COLORS[2], COLORS[3]},
                    null,
                    Shader.TileMode.REPEAT
            );
        } else { // Esquerda -> Direita ou Direita -> Esquerda (Horizontal)
            shader = new LinearGradient(
                    0, 0, w, 0,
                    new int[]{COLORS[0], COLORS[1], COLORS[2], COLORS[3]},
                    null,
                    Shader.TileMode.REPEAT
            );
        }
        paint.setShader(shader);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (shader != null) {
            int direction = sessionManager.getFlowDirection();
            switch (direction) {
                case 1: // Direita -> Esquerda
                    matrix.setTranslate(getWidth() * offset, 0);
                    break;
                case 2: // Cima -> Baixo
                    matrix.setTranslate(0, -getHeight() * offset);
                    break;
                case 3: // Baixo -> Cima
                    matrix.setTranslate(0, getHeight() * offset);
                    break;
                default: // Esquerda -> Direita
                    matrix.setTranslate(-getWidth() * offset, 0);
                    break;
            }
            shader.setLocalMatrix(matrix);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}
