package com.mogujie.transformer.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.retryu.myapplication.R;

/**
 * Created by retryu on 15/10/10.
 */
public class Camerabooster extends FrameLayout {

    private static final int mRowNumber = 3;
    private static final int mLineNumber = 3;
    private static int mLineGap = 0;
    private static int mrowGap = 0;
    private static final double ASPECT_RATIO = 3.0 / 4.0;

    private Paint mPaint;
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private View mTopCover;
    private View mBottomCover;
    private View btnTakePhot;
    private Context mContext;
    private boolean showBooster = false;
    public static float ratio = (float) ASPECT_RATIO;
    private static int mFocusWidth = 100;
    private static int mFocusHeight = 100;

    private FocusView mFoucusView;

    public Camerabooster(Context context) {
        super(context);
        initView(context);
    }

    public Camerabooster(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public Camerabooster(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void initView(Context context) {

        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = layoutInflater.inflate(R.layout.layout_camerbooster, null);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(rootView, lp);
        mPaint = new Paint();
        mPaint.setColor(Color.GRAY);

        mTopCover = findViewById(R.id.topCoverView);
        mBottomCover = findViewById(R.id.bottomCoverView);

        initFoucuView(context);
//        initCoverParamers(mTopCover);
//        initCoverParamers(mBottomCover);

    }

    private void initFoucuView(Context context) {
        mFoucusView = new FocusView(context);
        LayoutParams lp = new LayoutParams(mFocusWidth,mFocusHeight);
        addView(mFoucusView, lp);
    }

    /**
     * 更新聚焦框位置
     * @param x
     * @param y
     */
    public void focusView(float  x,float y,Rect rect){
        mFoucusView.updateFouccusCenter(x, y, rect);
    }

    public void focusSuceeed(){
        mFoucusView.focusSucessed();
    }

    public void focusFinish(){
        mFoucusView.focusFinish();
    }




    private void setCoverHeight(View coverView, int height, int top) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) coverView.getLayoutParams();
        lp.height = height;
        coverView.setLayoutParams(lp);
    }

    private int getCoverHeight(float ratio) {
        int previewHeight = (int) (getWidth() / ratio);
        int coverHeight = (int) ((getHeight() - previewHeight) / 2f);
        return coverHeight;
    }

    public void setRatio(float ratio,int  width, int height) {
        this.ratio = ratio;
        int coverHeight = getCoverHeight(ratio);
        setCoverHeight(mTopCover, coverHeight, 0);
        setCoverHeight(mBottomCover, coverHeight, 0);
        updateTakePhotoMargin(coverHeight);
        calculateBoosterGap(coverHeight, width, height);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//
//        final boolean isPortrait =
//                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
//
//        if (isPortrait) {
//            if (width > height * ASPECT_RATIO) {
//                width = (int) (height * ASPECT_RATIO + 0.5);
//            } else {
//                height = (int) (width / ASPECT_RATIO + 0.5);
//            }
//        } else {
//            if (height > width * ASPECT_RATIO) {
//                height = (int) (width * ASPECT_RATIO + 0.5);
//            } else {
//                width = (int) (height / ASPECT_RATIO + 0.5);
//            }
//        }
//
//        setMeasuredDimension(width, height);
//        ViewGroup.LayoutParams lp = getLayoutParams();
//        lp.height = height;
//        lp.width = width;
//        setLayoutParams(lp);
        Log.d("[onMeasure]", " width:" + getMeasuredWidth() + "  mViewHeight:" + getMeasuredHeight());
    }


    /**
     * 计算辅助线间距
     */
    private void calculateBoosterGap(int coverHeight,int width,int height) {
        mViewHeight = height- coverHeight * 2;
        mViewWidth = width;
        mLineGap = mViewHeight / mLineNumber;
        mrowGap = mViewWidth / mRowNumber;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showBooster) {
            mViewHeight = getHeight();
            mViewWidth = getWidth();
            for (int i = 1; i < mLineNumber; i++) {
                int top = i * mLineGap + mTopCover.getHeight();
                canvas.drawLine(0, top, mViewWidth, top, mPaint);
            }
            for (int j = 1; j < mRowNumber; j++) {
                int left = j * mrowGap;
                canvas.drawLine(left, 0, left, mViewHeight, mPaint);
            }
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        Rect focusRect = new Rect(0,0,1000,1000);
        canvas.drawRect(focusRect, paint);


        float  f2 = (float)mViewHeight/2;
        float f3 = (float)mViewWidth;
        float f4 = (float)mViewHeight/2;
        canvas.drawLine(0,f2 , f3,f4 ,paint);
    }

    public void setBtnTakePhoto(View takePhoto) {
        btnTakePhot = takePhoto;
    }

    private void updateTakePhotoMargin(int margin) {
        if (btnTakePhot == null)
            return;
        if(margin <0){
            margin = 0;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnTakePhot.getLayoutParams();
        lp.bottomMargin = margin + dip2px(mContext, 20);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void showBooster(boolean show) {
        showBooster = show;
        setRatio(ratio,getWidth(),getHeight());
        invalidate();
    }

    public boolean isBoosterShow() {
        return showBooster;
    }
}
