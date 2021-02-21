package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.options.OptionsItem;
import com.skywire.skycoin.vpn.controls.options.OptionsModalWindow;

import java.util.ArrayList;

public class Select extends FrameLayout implements View.OnTouchListener, View.OnClickListener {
    public static class SelectOption {
        public String text;
        public String value;
        public Integer iconId;
    }

    private EditText edit;
    private FrameLayout clickArea;

    private ArrayList<SelectOption> options;
    private int selectedIndex = 0;

    public Select(Context context) {
        super(context);
        Initialize(context, null);
    }
    public Select(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public Select(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    protected void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_select, this, true);

        edit = this.findViewById (R.id.edit);
        clickArea = this.findViewById (R.id.clickArea);

        clickArea.setOnTouchListener(this);
        clickArea.setOnClickListener(this);
    }

    public void setValues(ArrayList<SelectOption> options, int selectedIndex) {
        this.options = options;
        this.selectedIndex = selectedIndex;

        updateContent();
    }

    private void updateContent() {
        SelectOption currentOption = options.get(selectedIndex);

        Drawable leftDrawable = null;
        if (currentOption.iconId != null) {
            leftDrawable = ContextCompat.getDrawable(getContext(), currentOption.iconId);
            leftDrawable.setBounds(0, 0, leftDrawable.getIntrinsicWidth(), leftDrawable.getIntrinsicHeight());
        }
        Drawable[] drawables = edit.getCompoundDrawables();
        edit.setCompoundDrawables(leftDrawable, drawables[1], drawables[2], drawables[3]);

        edit.setText(currentOption.text);
    }

    public String getSelectedValue() {
        return options.get(selectedIndex).value;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setAlpha(0.5f);
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_POINTER_UP || event.getAction() == MotionEvent.ACTION_UP) {
            setAlpha(1f);
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        ArrayList<OptionsItem.SelectableOption> optionsToShow = new ArrayList();

        for (SelectOption option : options) {
            OptionsItem.SelectableOption optionToShow = new OptionsItem.SelectableOption();
            optionToShow.drawableId = option.iconId;
            optionToShow.label = option.text;

            optionsToShow.add(optionToShow);
        }

        OptionsModalWindow modal = new OptionsModalWindow(getContext(), null, optionsToShow, (int selectedOption) -> {
            selectedIndex = selectedOption;
            updateContent();
        });

        modal.show();
    }
}
