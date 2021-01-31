package com.skywire.skycoin.vpn.helpers;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import androidx.core.content.res.ResourcesCompat;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.R;

public class MaterialFontSpan extends TypefaceSpan {
    private static final Typeface materialFont = ResourcesCompat.getFont(App.getContext(), R.font.material_font);

    public MaterialFontSpan() {
        super("");
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        paint.setTypeface(materialFont);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        paint.setTypeface(materialFont);
    }
}
