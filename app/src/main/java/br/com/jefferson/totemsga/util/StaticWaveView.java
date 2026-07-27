package br.com.jefferson.totemsga.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import br.com.jefferson.totemsga.R;

public class StaticWaveView extends View {

    private Paint paint;
    private Path path;
    private boolean isTop = true;

    private static final int[] COLORS = {
            Color.parseColor("#FFCC00"), // Amarelo
            Color.parseColor("#F47B20"), // Laranja
            Color.parseColor("#E31E24")  // Vermelho
    };

    public StaticWaveView(Context context) {
        this(context, null);
    }

    public StaticWaveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StaticWaveView);
            isTop = a.getBoolean(R.styleable.StaticWaveView_isTop, true);
            a.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAlpha((int) (255 * 0.35f)); // Fixed 35% opacity
        path = new Path();
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
            // Smoother, broader curve for background
            path.lineTo(w, 140 * sh);
            path.cubicTo(300 * sw, 60 * sh, 100 * sw, 220 * sh, 0, 130 * sh);
            path.close();
        } else {
            path.moveTo(0, h);
            path.lineTo(w, h);
            // Smoother, broader curve for background
            path.lineTo(w, 60 * sh);
            path.cubicTo(250 * sw, 160 * sh, 150 * sw, -20 * sh, 0, 80 * sh);
            path.close();
        }
    }

    private void updateShader(int w, int h) {
        Shader shader = new LinearGradient(
                0, 0, w, 0,
                COLORS,
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}
