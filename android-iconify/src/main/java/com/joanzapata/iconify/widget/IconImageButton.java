package com.joanzapata.iconify.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.R;
import com.joanzapata.iconify.internal.Animation;

public class IconImageButton extends ImageButton {

    private ColorStateList colorStateList = ColorStateList.valueOf(IconImageView.DEFAULT_COLOR);

    public IconImageButton(Context context) {
        super(context);
    }

    public IconImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
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
            switch (Animation.values()[a.getInt(R.styleable.IconImageView_iconAnimation,
                    Animation.NONE.ordinal())]) {
                case SPIN:
                    drawable.spin();
                    break;
                case PULSE:
                    drawable.pulse();
                    break;
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
            colorStateList = ColorStateList.valueOf(IconImageView.DEFAULT_COLOR);
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

    public void setIconAnimation(Animation animation) {
        setIconAnimation(animation, false);
    }

    public void setIconAnimation(Animation animation, boolean restart) {
        Drawable drawable = getDrawable();
        if (drawable instanceof IconDrawable) {
            IconDrawable iconDrawable = (IconDrawable) drawable;
            switch (animation) {
                case SPIN:
                    iconDrawable.spin();
                    break;
                case PULSE:
                    iconDrawable.pulse();
                    break;
                case NONE:
                    iconDrawable.stop();
                    break;
            }
            if (restart && getVisibility() == VISIBLE) {
                iconDrawable.setVisible(true, true);
            }
        }
    }

}
