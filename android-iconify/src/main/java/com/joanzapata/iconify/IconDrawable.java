package com.joanzapata.iconify;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.LayoutDirection;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.View;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.view.View.LAYOUT_DIRECTION_RTL;

/**
 * Embed an icon into a Drawable that can be used as TextView icons, or ActionBar icons.
 * <pre>
 *     new IconDrawable(context, IconValue.icon_star)
 *           .colorRes(R.color.white)
 *           .actionBarSize();
 * </pre>
 * If you don't set the size of the drawable, it will use the size
 * that is given to him. Note that in an ActionBar, if you don't
 * set the size explicitly it uses 0, so please use actionBarSize().
 */
public final class IconDrawable extends Drawable implements Animatable {
    private static final int DEFAULT_COLOR = Color.BLACK;
    // Set the default tint to make it half translucent on disabled state.
    private static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.MULTIPLY;
    private static final ColorStateList DEFAULT_TINT = new ColorStateList(
            new int[][] { { -android.R.attr.state_enabled }, StateSet.WILD_CARD },
            new int[] { 0x80FFFFFF, 0xFFFFFFFF }
    );
    private static final int ROTATION_DURATION = 2000;
    // Font Awesome uses 8-step rotation for pulse, and
    // it seems to have the only pulsing spinner. If
    // spinners with different pulses are introduced at
    // some point, then a pulse property can be
    // implemented for the icons.
    private static final int ROTATION_PULSES = 8;
    private static final int ROTATION_PULSE_DURATION = ROTATION_DURATION / ROTATION_PULSES;
    private static final int ANDROID_ACTIONBAR_ICON_SIZE_DP = 24;
    private static final Rect TEMP_DRAW_BOUNDS = new Rect();

    private IconState iconState;
    private final TextPaint paint;
    private int color;
    private ColorFilter tintFilter;
    private int tintColor;
    private long spinStartTime = -1;
    private boolean mMutated;
    private final String text;
    private final Rect drawBounds = new Rect();
    private float centerX, centerY;
    private Runnable invalidateRunnable;

    private static Icon findValidIconForKey(String iconKey) {
        Icon icon = Iconify.findIconForKey(iconKey);
        if (icon == null) {
            throw new IllegalArgumentException("No icon found with key \"" + iconKey + "\".");
        }
        return icon;
    }

    private static Icon validateIcon(Icon icon) {
        if (icon == null) {
            throw new NullPointerException("Icon can't be null.");
        }
        if (Iconify.findTypefaceOf(icon) == null) {
            throw new IllegalStateException("Unable to find the module associated " +
                    "with icon " + icon.key() + ", have you registered the module " +
                    "you are trying to use with Iconify.with(...) in your Application?");
        }
        return icon;
    }

    /**
     * Create an IconDrawable.
     * @param context Your activity or application context.
     * @param iconKey The icon key you want this drawable to display.
     * @throws IllegalArgumentException if the key doesn't match any icon.
     */
    public IconDrawable(Context context, String iconKey) {
        this(context, new IconState(findValidIconForKey(iconKey)));
    }

    /**
     * Create an IconDrawable.
     * @param context Your activity or application context.
     * @param icon    The icon you want this drawable to display.
     */
    public IconDrawable(Context context, Icon icon) {
        this(context, new IconState(validateIcon(icon)));
    }

    private IconDrawable(IconState state) {
        this(null, state);
    }

    private IconDrawable(Context context, IconState state) {
        iconState = state;
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // We have already confirmed that a typeface exists for this icon during
        // validation, so we can ignore the null pointer warning.
        //noinspection ConstantConditions
        paint.setTypeface(Iconify.findTypefaceOf(state.icon).getTypeface(context));
        paint.setStyle(state.style);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setUnderlineText(false);
        color = state.colorStateList.getColorForState(StateSet.WILD_CARD, DEFAULT_COLOR);
        paint.setColor(color);
        updateTintFilter();
        setModulatedAlpha();
        paint.setDither(iconState.dither);
        text = String.valueOf(iconState.icon.character());
    }

    /**
     * Set the size of this icon to the standard Android ActionBar.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable actionBarSize(Context context) {
        return sizeDp(context, ANDROID_ACTIONBAR_ICON_SIZE_DP);
    }

    /**
     * Set the size of the drawable.
     * @param dimenRes The dimension resource.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable sizeRes(Context context, int dimenRes) {
        return sizePx(context.getResources().getDimensionPixelSize(dimenRes));
    }

    /**
     * Set the size of the drawable.
     * @param size The size in density-independent pixels (dp).
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable sizeDp(Context context, int size) {
        return sizePx((int) TypedValue.applyDimension(
                COMPLEX_UNIT_DIP, size,
                context.getResources().getDisplayMetrics()));
    }

    /**
     * Set the size of the drawable.
     * @param size The size in pixels (px).
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable sizePx(int size) {
        iconState.height = size;
        paint.setTextSize(size);
        paint.getTextBounds(text, 0, 1, TEMP_DRAW_BOUNDS);
        iconState.width = TEMP_DRAW_BOUNDS.width();
        return this;
    }

    /**
     * Set the color of the drawable.
     * @param color The color, usually from android.graphics.Color or 0xFF012345.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable color(int color) {
        return color(ColorStateList.valueOf(color));
    }

    /**
     * Set the color of the drawable.
     * @param colorStateList The color state list.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable color(ColorStateList colorStateList) {
        if (colorStateList == null) {
            colorStateList = ColorStateList.valueOf(DEFAULT_COLOR);
        }
        if (colorStateList != iconState.colorStateList) {
            iconState.colorStateList = colorStateList;
            color = colorStateList.getColorForState(StateSet.WILD_CARD, DEFAULT_COLOR);
            paint.setColor(color);
            invalidateSelf();
        }
        return this;
    }

    /**
     * Set the color of the drawable.
     * @param colorRes The color resource, from your R file.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable colorRes(Context context, int colorRes) {
        return color(context.getResources().getColorStateList(colorRes));
    }

    /**
     * Set the alpha of this drawable.
     * @param alpha The alpha, between 0 (transparent) and 255 (opaque).
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable alpha(int alpha) {
        setAlpha(alpha);
        return this;
    }

    /**
     * Start a spinning animation on this drawable. Call {@link #stop()}
     * to stop it.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable spin() {
        iconState.pulse = false;
        start();
        return this;
    }

    /**
     * Start a pulse animation on this drawable. Call {@link #stop()}
     * to stop it.
     * @return The current IconDrawable for chaining.
     */
    public IconDrawable pulse() {
        iconState.pulse = true;
        start();
        return this;
    }

    /**
     * Returns the icon to be displayed
     * @return The icon
     */
    public final Icon getIcon() {
        return iconState.icon;
    }

    @Override
    public int getIntrinsicHeight() {
        return iconState.height;
    }

    @Override
    public int getIntrinsicWidth() {
        return iconState.width;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        final int width = bounds.width();
        final int height = bounds.height();
        paint.setTextSize(height);
        paint.getTextBounds(text, 0, 1, drawBounds);
        paint.setTextSize(Math.min(height, (int) Math.ceil(
                width * (height / (float) drawBounds.width()))));
        paint.getTextBounds(text, 0, 1, drawBounds);
        drawBounds.offsetTo(bounds.left + (width - drawBounds.width()) / 2,
                bounds.top + (height - drawBounds.height()) / 2 - drawBounds.bottom);
        centerX = bounds.exactCenterX();
        centerY = bounds.exactCenterY();
    }

    @Override
    public Rect getDirtyBounds() {
        return drawBounds;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void getOutline(Outline outline) {
        outline.setRect(drawBounds);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        final boolean needMirroring = needMirroring();
        if (needMirroring) {
            canvas.translate(getBounds().width(), 0);
            canvas.scale(-1.0f, 1.0f);
        }
        if (iconState.spinning) {
            long currentTime = SystemClock.uptimeMillis();
            if (spinStartTime < 0) {
                spinStartTime = currentTime;
                if (isVisible()) {
                    if (iconState.pulse) {
                        scheduleSelf(invalidateRunnable, currentTime + ROTATION_PULSE_DURATION);
                    } else {
                        invalidateSelf();
                    }
                }
            } else {
                boolean isVisible = isVisible();
                long timeElapsed = currentTime - spinStartTime;
                float rotation;
                if (iconState.pulse) {
                    rotation = timeElapsed / (float) ROTATION_PULSE_DURATION;
                    if (isVisible) {
                        scheduleSelf(invalidateRunnable, currentTime +
                                (timeElapsed * (int) (rotation + 1)));
                    }
                    rotation = ((int) Math.floor(rotation)) * 360f / ROTATION_PULSES;
                } else {
                    rotation = timeElapsed / (float) ROTATION_DURATION * 360f;
                    if (isVisible) {
                        invalidateSelf();
                    }
                }
                canvas.rotate(rotation, centerX, centerY);
            }
            if (isVisible()) {
                invalidateSelf();
            }
        }
        canvas.drawText(text, centerX, drawBounds.bottom, paint);
        canvas.restore();
    }

    @Override
    public boolean isStateful() {
        return iconState.colorStateList.isStateful() ||
                (iconState.tint != null && iconState.tint.isStateful());
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = false;

        int newColor = iconState.colorStateList.getColorForState(state, DEFAULT_COLOR);
        if (newColor != color) {
            color = newColor;
            paint.setColor(color);
            setModulatedAlpha();
            changed = true;
        }

        if (tintFilter != null) {
            int newTintColor = iconState.tint.getColorForState(state, Color.TRANSPARENT);
            if (newTintColor != tintColor) {
                tintColor = newTintColor;
                tintFilter = new PorterDuffColorFilter(tintColor, iconState.tintMode);
                if (iconState.colorFilter == null) {
                    paint.setColorFilter(tintFilter);
                    changed = true;
                }
            }
        }

        return changed;
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != iconState.alpha) {
            iconState.alpha = alpha;
            setModulatedAlpha();
            invalidateSelf();
        }
    }

    private void setModulatedAlpha() {
        paint.setAlpha(((color >> 24) * iconState.alpha) / 255);
    }

    @Override
    public int getAlpha() {
        return iconState.alpha;
    }

    @Override
    public int getOpacity() {
        int baseAlpha = color >> 24;
        if (baseAlpha == 255 && iconState.alpha == 255) return PixelFormat.OPAQUE;
        if (baseAlpha == 0 || iconState.alpha == 0) return PixelFormat.TRANSPARENT;
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setDither(boolean dither) {
        if (dither != iconState.dither) {
            iconState.dither = dither;
            paint.setDither(dither);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf != iconState.colorFilter) {
            iconState.colorFilter = cf;
            paint.setColorFilter(cf);
            invalidateSelf();
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return iconState.colorFilter;
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint != iconState.tint) {
            iconState.tint = tint;
            updateTintFilter();
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        if (tintMode != iconState.tintMode) {
            iconState.tintMode = tintMode;
            updateTintFilter();
            invalidateSelf();
        }
    }

    private void updateTintFilter() {
        if (iconState.tint == null || iconState.tintMode == null) {
            if (tintFilter == null) {
                return;
            }
            tintColor = 0;
            tintFilter = null;
        } else {
            tintColor = iconState.tint.getColorForState(getState(), Color.TRANSPARENT);
            tintFilter = new PorterDuffColorFilter(tintColor, iconState.tintMode);
        }
        if (iconState.colorFilter == null) {
            paint.setColorFilter(tintFilter);
            invalidateSelf();
        }
    }

    /**
     * Sets paint style.
     * @param style to be applied
     */
    public void setStyle(Paint.Style style) {
        if (style != iconState.style) {
            iconState.style = style;
            paint.setStyle(style);
            invalidateSelf();
        }
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (SDK_INT >= JELLY_BEAN_MR1 && iconState.icon.supportsRtl() &&
                iconState.autoMirrored != mirrored) {
            iconState.autoMirrored = mirrored;
            invalidateSelf();
        }
    }

    @Override
    public final boolean isAutoMirrored() {
        return iconState.autoMirrored;
    }

    // Since the auto-mirrored state is only set to true if the SDK
    // version supports it, we don't need an explicit check for that
    // before calling getLayoutDirection().
    @TargetApi(JELLY_BEAN_MR1)
    private boolean needMirroring() {
        if (isAutoMirrored()) {
            if (SDK_INT >= M) {
                return getLayoutDirection() == LayoutDirection.RTL;
            }
            // Since getLayoutDirection() is hidden prior to Marshmallow, we
            // will try to get the layout direction from the View, which we will
            // assume is set as the callback. As the setLayoutDirection() method
            // is also hidden, we can safely rely on the behaviour of the
            // platform Views to provide a correct replacement for the hidden
            // method.
            Callback callback = getCallback();
            if (callback instanceof View) {
                return ((View) callback).getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (!iconState.spinning) {
            iconState.spinning = true;
            if (iconState.pulse && invalidateRunnable == null) {
                invalidateRunnable = new InvalidateRunnable();
            }
            invalidateSelf();
        }
    }

    @Override
    public void stop() {
        if (iconState.spinning) {
            iconState.spinning = false;
            if (invalidateRunnable != null) {
                unscheduleSelf(invalidateRunnable);
                invalidateRunnable = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return iconState.spinning;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        if (iconState.spinning) {
            if (changed) {
                if (visible) {
                    invalidateSelf();
                } else if (invalidateRunnable != null) {
                    unscheduleSelf(invalidateRunnable);
                }
            } else {
                if (restart && visible) {
                    spinStartTime = -1;
                }
            }
        }
        return changed;
    }

    private class InvalidateRunnable implements Runnable {
        @Override
        public void run() {
            invalidateSelf();
        }
    }

    @Override
    public int getChangingConfigurations() {
        return iconState.changingConfigurations;
    }

    @Override
    public void setChangingConfigurations(int configs) {
        iconState.changingConfigurations = configs;
    }

    // Implementing shared state despite being a third-party implementation
    // in order to work around bugs in the framework and support library:
    // http://b.android.com/191754
    // https://github.com/JoanZapata/android-iconify/issues/93
    @Override
    public ConstantState getConstantState() {
        return iconState;
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            iconState = new IconState(iconState);
            mMutated = true;
        }
        return this;
    }

    private static class IconState extends ConstantState {
        final Icon icon;
        int height = -1, width = -1;
        ColorStateList colorStateList = ColorStateList.valueOf(DEFAULT_COLOR);
        int alpha = 255;
        boolean dither;
        ColorFilter colorFilter;
        ColorStateList tint = DEFAULT_TINT;
        PorterDuff.Mode tintMode = DEFAULT_TINT_MODE;
        Paint.Style style = Paint.Style.FILL;
        boolean spinning;
        boolean pulse;
        boolean autoMirrored;
        int changingConfigurations;

        IconState(Icon icon) {
            this.icon = icon;
            autoMirrored = SDK_INT >= JELLY_BEAN_MR1 && icon.supportsRtl();
        }

        IconState(IconState state) {
            icon = state.icon;
            height = state.height;
            width = state.width;
            colorStateList = state.colorStateList;
            alpha = state.alpha;
            dither = state.dither;
            colorFilter = state.colorFilter;
            tint = state.tint;
            tintMode = state.tintMode;
            style = state.style;
            spinning = state.spinning;
            pulse = state.pulse;
            autoMirrored = state.autoMirrored;
            changingConfigurations = state.changingConfigurations;
        }

        @Override
        public Drawable newDrawable() {
            return new IconDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return changingConfigurations;
        }
    }
}