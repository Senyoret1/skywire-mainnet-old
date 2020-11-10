package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;

public class TopBarButton extends LinearLayout implements View.OnClickListener {
    public TopBarButton(Context context) {
        super(context);
        Initialize(context, null);
    }
    public TopBarButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public TopBarButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private TextView textIcon;

    private void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.control_top_bar_button, this, true);

        textIcon = this.findViewById (R.id.textIcon);

        if (attrs != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TopBarButton,
                0, 0);

            if (attributes.getInteger(R.styleable.TopBarButton_material_icon, 0) == 0) {
                textIcon.setText("\ue5d2");
            } else {
                textIcon.setText("\ue5c4");
            }

            attributes.recycle();
        } else {
            textIcon.setText("\ue5d2");
        }

        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }
}
