package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

import me.vkryl.android.animator.ListAnimator;

public class DialogsActivityTopPanelLayout extends AnimatedLinearLayout {
    public DialogsActivityTopPanelLayout(@NonNull Context context) {
        super(context);

        setOrientation(LinearLayout.VERTICAL);
        updateColors();
    }

    BlurredBackgroundDrawable backgroundDrawable;

    private final Paint solidBackgroundPaint = new Paint();
    private boolean solidBackgroundEnabled;
    private int solidBackgroundColor;

    public void setBlurredBackground(BlurredBackgroundDrawable background) {
        backgroundDrawable = background;
    }

    public void setSolidBackgroundColor(int color) {
        solidBackgroundEnabled = true;
        solidBackgroundColor = color;
        solidBackgroundPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkBoundsAndClipping();
    }

    @Override
    protected void onItemsChanged() {
        super.onItemsChanged();
        checkBoundsAndClipping();
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev)
            || ev.getAction() == MotionEvent.ACTION_DOWN && backgroundDrawable != null && backgroundDrawable.getBounds().contains((int) ev.getX(), (int) ev.getY());
    }

    private final Path clipPath = new Path();
    private final RectF clipRectF = new RectF();

    private void checkBoundsAndClipping() {
        final float bgHeight = getMetadata().getTotalHeight();
        final float bgAlpha = getMetadata().getTotalVisibility();

        clipRectF.set(0, 0, getMeasuredWidth(), getPaddingTop() + bgHeight);

        clipPath.rewind();
        clipPath.addRect(clipRectF, Path.Direction.CW);

        if (backgroundDrawable != null) {
            backgroundDrawable.setAlpha((int) (bgAlpha * 255));
            backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getPaddingTop() + getPaddingBottom() + (int) bgHeight);
            backgroundDrawable.setRadius(0);
        }
    }

    private int defaultRadiusDp = 24;

    public void setDefaultRadiusDp(int defaultRadius) {
        this.defaultRadiusDp = defaultRadius;
    }

    public void updateColors() {
        if (backgroundDrawable != null) {
            backgroundDrawable.updateColors();
        }
        invalidate();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (getMetadata().getTotalVisibility() == 0) return;

        if (solidBackgroundEnabled) {
            solidBackgroundPaint.setAlpha((int) (Color.alpha(solidBackgroundColor) * getMetadata().getTotalVisibility()));
            canvas.drawRect(clipRectF, solidBackgroundPaint);
        } else if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        }

        canvas.save();
        canvas.clipPath(clipPath);
        for (int a = 0, N = getEntriesCount(); a < N; a++) {
            final ListAnimator.Entry<?> entry = getEntry(a);
            final float top = getPaddingTop() + entry.getRectF().top;

            final float position = entry.getPosition();
            final float alpha = entry.getVisibility() * Math.min(1, position);

            if (alpha <= 0) {
                continue;
            }

            final int wasAlpha = Theme.dividerPaint.getAlpha();
            Theme.dividerPaint.setAlpha((int) (wasAlpha * alpha));
            final float offsetL = getPaddingLeft() + dp(16) * (1f - alpha);
            final float offsetR = getPaddingRight() + dp(16) * (1f - alpha);
            canvas.drawLine(offsetL, top, getWidth() - offsetR, top, Theme.dividerPaint);
            Theme.dividerPaint.setAlpha(wasAlpha);
        }

        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
