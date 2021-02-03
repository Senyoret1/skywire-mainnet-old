package com.skywire.skycoin.vpn.activities.start;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.skywire.skycoin.vpn.R;

public class MapBackground extends View {
    public MapBackground(Context context) {
        super(context);
        Initialize(context, null);
    }
    public MapBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public MapBackground(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private BitmapDrawable bitmapDrawable;

    private float proportion = 1;
    private Rect drawableArea = new Rect(0, 0,1, 1);
    private ObjectAnimator animation;

    private void Initialize (Context context, AttributeSet attrs) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map);
        bitmapDrawable = new BitmapDrawable(context.getResources(), bitmap);
        bitmapDrawable.setAlpha(40);

        proportion = (float)bitmap.getWidth() / (float)bitmap.getHeight();

        startAnimation(0);
    }

    public void pauseAnimation() {
        animation.pause();
    }

    public void resumeAnimation() {
        animation.resume();
    }

    public void cancelAnimation() {
        animation.cancel();
        animation = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthtSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthtSize != drawableArea.width() || heightSize != drawableArea.height()) {
            setValues(widthtSize, heightSize);
        }

        setMeasuredDimension(drawableArea.width(), drawableArea.height());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        bitmapDrawable.draw(canvas);
        super.onDraw(canvas);
    }

    private void setValues(int width, int height) {
        drawableArea = new Rect(0, 0, (int) (height * proportion), height);
        bitmapDrawable.setBounds(drawableArea);

        if (animation == null) {
            return;
        }

        boolean pause = animation.isPaused();
        animation.cancel();

        startAnimation(-(drawableArea.width() - width));
        if (pause) {
            animation.pause();
        }
    }

    private void startAnimation(int finalValue) {
        animation = ObjectAnimator.ofFloat(this, "translationX", 0, finalValue);
        animation.setDuration(45000);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setRepeatCount(ValueAnimator.INFINITE);
        animation.setRepeatMode(ValueAnimator.REVERSE);
        animation.start();
    }
}
