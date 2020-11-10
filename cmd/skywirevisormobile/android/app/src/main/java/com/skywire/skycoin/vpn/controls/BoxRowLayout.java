package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;

public class BoxRowLayout extends FrameLayout {
    public BoxRowLayout(Context context) {
        super(context);
        Initialize(context, null);
    }
    public BoxRowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public BoxRowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private BoxRowBackground background;
    private BoxRowRipple ripple;
    private View separator;

    private void Initialize (Context context, AttributeSet attrs) {
        int type = 1;

        if (attrs != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BoxRowLayout,
                0, 0
            );

            type = attributes.getInteger(R.styleable.BoxRowLayout_type, 1);

            attributes.recycle();
        }

        background = new BoxRowBackground(context);
        ripple = new BoxRowRipple(context);
        separator = new View(context);

        if (type == 0) {
            setType(BoxRowTypes.TOP);
        } else if (type == 1) {
            setType(BoxRowTypes.MIDDLE);
        } else if (type == 2) {
            setType(BoxRowTypes.BOTTOM);
        }

        this.setClipToPadding(false);

        this.addView(background);
        this.addView(ripple);
        this.addView(separator);
    }

    public void setType(BoxRowTypes type) {
        float horizontalPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24,
            getResources().getDisplayMetrics()
        );

        float verticalPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10,
            getResources().getDisplayMetrics()
        );

        float topPaddingExtra = 0;
        float bottomPaddingExtra = 0;

        if (type == BoxRowTypes.TOP) {
            this.setBackgroundResource(R.drawable.background_box1);

            topPaddingExtra = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10,
                getResources().getDisplayMetrics()
            );

            separator.setVisibility(View.VISIBLE);
        } else if (type == BoxRowTypes.MIDDLE) {
            this.setBackgroundResource(R.drawable.background_box2);
            separator.setVisibility(View.VISIBLE);
        } else if (type == BoxRowTypes.BOTTOM) {
            this.setBackgroundResource(R.drawable.background_box3);

            bottomPaddingExtra = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                14,
                getResources().getDisplayMetrics()
            );

            separator.setVisibility(View.GONE);
        }

        int finalLeftPadding = (int)horizontalPadding;
        int finalTopPadding = (int)(verticalPadding + topPaddingExtra);
        int finalRightPadding = (int)horizontalPadding;
        int finalBottomPadding = (int)(verticalPadding + bottomPaddingExtra);

        this.setPadding(finalLeftPadding, finalTopPadding, finalRightPadding, finalBottomPadding);

        FrameLayout.LayoutParams backgroundLayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        backgroundLayoutParams.leftMargin = -finalLeftPadding;
        backgroundLayoutParams.topMargin = -finalTopPadding;
        backgroundLayoutParams.rightMargin = -finalRightPadding;
        backgroundLayoutParams.bottomMargin = -finalBottomPadding;
        background.setLayoutParams(backgroundLayoutParams);
        background.setType(type);
        ripple.setLayoutParams(backgroundLayoutParams);
        ripple.setType(type);

        float separatorHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                getResources().getDisplayMetrics()
        );

        float separatorHorizontalMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                -5,
                getResources().getDisplayMetrics()
        );

        FrameLayout.LayoutParams separatorLayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)separatorHeight);
        separatorLayoutParams.gravity = Gravity.BOTTOM;
        separatorLayoutParams.bottomMargin = -finalBottomPadding;
        separatorLayoutParams.leftMargin = (int)separatorHorizontalMargin;
        separatorLayoutParams.rightMargin = (int)separatorHorizontalMargin;
        separator.setLayoutParams(separatorLayoutParams);
        separator.setBackgroundResource(R.color.box_separator);
    }
}
