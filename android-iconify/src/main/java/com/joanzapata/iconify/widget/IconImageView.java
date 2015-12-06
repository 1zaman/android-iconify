package com.joanzapata.iconify.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.R;

public class IconImageView extends ImageView {

    static final int DEFAULT_COLOR = Color.BLACK;

    private ColorStateList colorStateList = ColorStateList.valueOf(DEFAULT_COLOR);

    public IconImageView(Context context) {
        super(context);
    }

    public IconImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.IconImageView, defStyleAttr, 0);
        ColorStateList colorStateList = a.getColorStateList(R.styleable.IconImageView_iconColor);
        if (colorStateList != null) {
            this.colorStateList = colorStateList;
        }
        String iconKey = a.getString(R.styleable.IconImageView_iconName);
        if (iconKey != null) {
            IconDrawable drawable = new IconDrawable(context, iconKey);
            if (a.getBoolean(R.styleable.IconImageView_iconSpin, false)) {
                drawable.spin();
            }
            setImageDrawable(drawable);
        }
        a.recycle();
    }

    public void setIcon(Icon icon) {
        setImageDrawable(new IconDrawable(getContext(), icon));
    }

    public final Icon getIcon() {
        Drawable drawable = getDrawable();
        if (drawable instanceof IconDrawable) {
            return ((IconDrawable) drawable).getIcon();
        }
        return null;
    }

    public void setIconColor(int color) {
        setIconColor(ColorStateList.valueOf(color));
    }

    public void setIconColor(ColorStateList colorStateList) {
        if (colorStateList == null) {
            colorStateList = ColorStateList.valueOf(DEFAULT_COLOR);
        }
        this.colorStateList = colorStateList;
        Drawable drawable = getDrawable();
        if (drawable instanceof IconDrawable) {
            ((IconDrawable) drawable).color(colorStateList);
        }
    }

    public void setIconColorResource(int colorResId) {
        setIconColor(getContext().getResources().getColorStateList(colorResId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable instanceof IconDrawable && drawable != getDrawable()) {
            ((IconDrawable) drawable).color(colorStateList);
        }
        super.setImageDrawable(drawable);
    }

    public void setIconSpinning(boolean spin) {
        setIconSpinning(spin, false);
    }

    public void setIconSpinning(boolean spin, boolean restart) {
        Drawable drawable = getDrawable();
        if (drawable instanceof IconDrawable) {
            IconDrawable iconDrawable = (IconDrawable) drawable;
            if (spin) {
                iconDrawable.start();
            } else {
                iconDrawable.stop();
            }
            if (restart && getVisibility() == VISIBLE) {
                iconDrawable.setVisible(true, true);
            }
        }
    }

}
