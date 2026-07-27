package br.com.jefferson.totemsga;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.res.ColorStateList;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import br.com.jefferson.totemsga.util.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        applyTheme();
    }

    protected void applyTheme() {
        try {
            String colorHex = sessionManager.getPrimaryColor();
            int color = Color.parseColor(colorHex);

            // 1. Status Bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(color);
                
                // Luminance logic for icons
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    View decor = getWindow().getDecorView();
                    double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
                    if (luminance > 0.5) {
                        decor.setSystemUiVisibility(decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    } else {
                        decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                }
            }

            // 2. Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setBackgroundColor(color);
                toolbar.setTitleTextColor(Color.WHITE);
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    toolbar.setNavigationOnClickListener(v -> onBackPressed());
                }
            }

            // 3. Activity Background
            View root = findViewById(android.R.id.content);
            if (root != null) {
                String bgColor = sessionManager.getBackgroundColor();
                root.setBackgroundColor(Color.parseColor(bgColor));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void styleButtons(Button... buttons) {
        try {
            String colorHex = sessionManager.getPrimaryColor();
            int color = Color.parseColor(colorHex);
            String textColorHex = sessionManager.getButtonTextColor();
            int textColor = Color.parseColor(textColorHex);
            ColorStateList csl = ColorStateList.valueOf(color);

            for (Button btn : buttons) {
                if (btn != null) {
                    btn.setBackgroundTintList(csl);
                    btn.setTextColor(textColor);
                }
            }
        } catch (Exception e) {}
    }
}
