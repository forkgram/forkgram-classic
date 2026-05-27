package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

public class SearchTabsAndFiltersLayout extends FrameLayout implements Theme.Colorable {
    private final Path clipPath = new Path();
    private BlurredBackgroundDrawable blurredBackgroundDrawable;

    public SearchTabsAndFiltersLayout(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void setBlurredBackground(BlurredBackgroundDrawable drawable) {
        setBackground(blurredBackgroundDrawable = drawable);
    }

    @Override
    public void updateColors() {
        if (blurredBackgroundDrawable != null) {
            blurredBackgroundDrawable.updateColors();
        }
    }
}
