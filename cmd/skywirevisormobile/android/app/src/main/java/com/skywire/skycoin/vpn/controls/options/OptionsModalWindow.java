package com.skywire.skycoin.vpn.controls.options;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;

public class OptionsModalWindow extends Dialog implements ClickWithIndexEvent<Void> {
    public static class SelectableOption {
        public String icon;
        public String label;
    }

    public interface OptionSelected {
        void optionSelected(int selectedIndex);
    }

    private LinearLayout container;

    private SelectableOption[] options;
    private OptionSelected event;

    public OptionsModalWindow(Context ctx, SelectableOption[] options, OptionSelected event) {
        super(ctx);

        this.options = options;
        this.event = event;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_options);

        container = findViewById(R.id.container);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int i = 0;
        for (SelectableOption option : options) {
            OptionsItem view = new OptionsItem(getContext());
            view.setIconText(option.icon);
            view.setLabel(option.label);
            view.setIndex(i++);
            view.setClickWithIndexEventListener(this);
            container.addView(view);
        }
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (event != null) {
            event.optionSelected(index);
        }

        dismiss();
    }
}
