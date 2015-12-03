package com.joanzapata.iconify.internal;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ReplacementSpan;
import android.widget.TextView;
import com.joanzapata.iconify.Icon;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.TEXT_DIRECTION_ANY_RTL;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG_LTR;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL;
import static android.view.View.TEXT_DIRECTION_LOCALE;
import static android.view.View.TEXT_DIRECTION_LTR;
import static android.view.View.TEXT_DIRECTION_RTL;

public class CustomTypefaceSpan extends ReplacementSpan {
    private static final int ROTATION_DURATION = 2000;
    private static final Rect TEXT_BOUNDS = new Rect();
    private static final Rect DIRTY_REGION = new Rect();
    private static final RectF DIRTY_REGION_FLOAT = new RectF();
    private static final Matrix ROTATION_MATRIX = new Matrix();
    private static final Paint LOCAL_PAINT = new Paint();
    private static final float BASELINE_RATIO = 1 / 7f;

    private final TextView view;
    private final String icon;
    private final Typeface type;
    private final float iconSizePx;
    private final float iconSizeRatio;
    private final int iconColor;
    private final boolean mirrorable;
    private final boolean rotate;
    private final boolean baselineAligned;
    private final long rotationStartTime;

    public CustomTypefaceSpan(TextView view, Icon icon, Typeface type,
            float iconSizePx, float iconSizeRatio, int iconColor,
            boolean rotate, boolean baselineAligned) {
        this.view = view;
        this.rotate = rotate;
        this.baselineAligned = baselineAligned;
        this.icon = String.valueOf(icon.character());
        this.type = type;
        this.iconSizePx = iconSizePx;
        this.iconSizeRatio = iconSizeRatio;
        this.iconColor = iconColor;
        this.mirrorable = SDK_INT >= JELLY_BEAN_MR1 && icon.supportsRtl();
        this.rotationStartTime = System.currentTimeMillis();
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
            int start, int end, Paint.FontMetricsInt fm) {
        LOCAL_PAINT.set(paint);
        applyCustomTypeFace(LOCAL_PAINT, type);
        LOCAL_PAINT.getTextBounds(icon, 0, 1, TEXT_BOUNDS);
        if (fm != null) {
            float baselineRatio = baselineAligned ? 0 : BASELINE_RATIO;
            fm.descent = (int) (TEXT_BOUNDS.height() * baselineRatio);
            fm.ascent = -(TEXT_BOUNDS.height() - fm.descent);
            fm.top = fm.ascent;
            fm.bottom = fm.descent;
        }
        return TEXT_BOUNDS.width();
    }

    @Override
    @TargetApi(JELLY_BEAN_MR1)
    public void draw(Canvas canvas, CharSequence text,
            int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        applyCustomTypeFace(paint, type);
        paint.getTextBounds(icon, 0, 1, TEXT_BOUNDS);
        int width = TEXT_BOUNDS.width();
        int height = TEXT_BOUNDS.height();
        float baselineRatio = baselineAligned ? 0f : BASELINE_RATIO;
        canvas.save();
        float baselineOffset = height * baselineRatio;
        float offsetY = y - TEXT_BOUNDS.bottom + baselineOffset;
        if (!needMirroring()) {
            canvas.translate(x - TEXT_BOUNDS.left, offsetY);
        } else {
            canvas.translate(x + width + TEXT_BOUNDS.left, offsetY);
            canvas.scale(-1.0f, 1.0f);
        }
        if (rotate) {
            float rotation = (System.currentTimeMillis() - rotationStartTime) / (float) ROTATION_DURATION * 360f;
            float centerX = TEXT_BOUNDS.left + width / 2f;
            float centerY = TEXT_BOUNDS.bottom - height / 2f;
            canvas.rotate(rotation, centerX, centerY);
            DIRTY_REGION_FLOAT.set(TEXT_BOUNDS);
            DIRTY_REGION_FLOAT.offsetTo((int) x, y - height + Math.round(baselineOffset));
            ROTATION_MATRIX.postRotate(rotation,
                    DIRTY_REGION_FLOAT.centerX(), DIRTY_REGION_FLOAT.centerY());
            ROTATION_MATRIX.mapRect(DIRTY_REGION_FLOAT);
            DIRTY_REGION_FLOAT.round(DIRTY_REGION);
            view.invalidate(DIRTY_REGION);
        }

        canvas.drawText(icon, 0, 0, paint);
        canvas.restore();
    }

    private void applyCustomTypeFace(Paint paint, Typeface tf) {
        paint.setFakeBoldText(false);
        paint.setTextSkewX(0f);
        paint.setTypeface(tf);
        if (rotate) paint.clearShadowLayer();
        if (iconSizeRatio > 0) paint.setTextSize(paint.getTextSize() * iconSizeRatio);
        else if (iconSizePx > 0) paint.setTextSize(iconSizePx);
        if (iconColor < Integer.MAX_VALUE) paint.setColor(iconColor);
    }

    // Since the 'mirrorable' flag is only set to true if the SDK
    // version supports it, we don't need an explicit check for that
    // before calling getLayoutDirection().
    @TargetApi(JELLY_BEAN_MR1)
    private boolean needMirroring() {
        if (!mirrorable) return false;

        // Passwords fields should be LTR
        if (view.getTransformationMethod() instanceof PasswordTransformationMethod) {
            return false;
        }

        // Always need to resolve layout direction first
        final boolean defaultIsRtl = view.getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        if (SDK_INT < JELLY_BEAN_MR2) {
            return defaultIsRtl;
        }

        // Select the text direction heuristic according to the
        // package-private getTextDirectionHeuristic() method in TextView
        TextDirectionHeuristic textDirectionHeuristic;
        switch (view.getTextDirection()) {
            default:
            case TEXT_DIRECTION_FIRST_STRONG:
                textDirectionHeuristic = defaultIsRtl ?
                        TextDirectionHeuristics.FIRSTSTRONG_RTL :
                        TextDirectionHeuristics.FIRSTSTRONG_LTR;
                break;
            case TEXT_DIRECTION_ANY_RTL:
                textDirectionHeuristic = TextDirectionHeuristics.ANYRTL_LTR;
                break;
            case TEXT_DIRECTION_LTR:
                textDirectionHeuristic = TextDirectionHeuristics.LTR;
                break;
            case TEXT_DIRECTION_RTL:
                textDirectionHeuristic = TextDirectionHeuristics.RTL;
                break;
            case TEXT_DIRECTION_LOCALE:
                textDirectionHeuristic = TextDirectionHeuristics.LOCALE;
                break;
            case TEXT_DIRECTION_FIRST_STRONG_LTR:
                textDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR;
                break;
            case TEXT_DIRECTION_FIRST_STRONG_RTL:
                textDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_RTL;
                break;
        }
        CharSequence text = view.getText();
        return textDirectionHeuristic.isRtl(text, 0, text.length());
    }
}