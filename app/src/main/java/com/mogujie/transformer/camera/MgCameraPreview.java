package com.mogujie.transformer.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MgCameraPreview extends SurfaceView {

    public static final String TAG = MgCameraPreview.class.getSimpleName();
    private static final int INVALID_POINTER_ID = -1;

    private static final int ZOOM_OUT = 0;
    private static final int ZOOM_IN = 1;
    private static final int ZOOM_DELTA = 1;

    private static final int FOCUS_SQR_SIZE = 100;
    private static final int FOCUS_MAX_BOUND = 1000;
    private static final int FOCUS_MIN_BOUND = -FOCUS_MAX_BOUND;
    public static final int MSG_FINISH_FOUCS = 1001;
    public static final int FOUCS_FINISHED_FADE_DEALY = 500;

    public static double PREVIEW_RATIO = 3.0f / 4.0f;

    private static  double ASPECT_RATIO = Double.MIN_VALUE;

    private static double WRAP_RATIO = Double.MIN_VALUE;
    private Camera mCamera;

    private float mLastTouchX;
    private float mLastTouchY;

    // For scaling
    private int mMaxZoom;
    private boolean mIsZoomSupported;
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mScaleFactor = 1;
    private ScaleGestureDetector mScaleDetector;

    // For focus
    private boolean mIsFocus;
    private boolean mIsFocusReady;
    private Camera.Area mFocusArea;
    private Camera.Area mMeteringArea;
    private ArrayList<Camera.Area> mFocusAreas;
    private ArrayList<Camera.Area> mMeteringAreas;
    public RelativeLayout mPreviewLayout;
    public Camerabooster mCamerabooster;

    private PreviewHandler mPreviewHandler;

    public Matrix mMatrix;




    public MgCameraPreview(Context context) {
        super(context);
        init(context);
    }

    public MgCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MgCameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mPreviewHandler = new PreviewHandler();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mFocusArea = new Camera.Area(new Rect(), 1000);
        mMeteringArea = new Camera.Area(new Rect(),1000);
        mFocusAreas = new ArrayList<Camera.Area>();
        mMeteringAreas = new ArrayList<Camera.Area>();
        mMeteringAreas.add(mMeteringArea);
        mFocusAreas.add(mFocusArea);
        mMatrix = new Matrix();
    }

    boolean mIsPortrait = true;
    /**
     * Measure the view and its content to determine the measured width and the
     * measured height
     */
    private int height;
    private int width;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);

        Log.d(TAG,"[onMeasure] width:"+width+"  height:"+height);
        setMeasuredDimension(width, height);
//        Log.d(TAG, "[onMeasure]  width:" + width + " height" + height);
//        setMeasuredDimension(width, height);
//        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
//        lp.width= width;
//        lp.height = height;
//        setLayoutParams(lp);
    }


    public void resizeViewByRatio(float ratio,boolean  isPortrait){
        this.ASPECT_RATIO = ratio;
         height = mPreviewLayout.getHeight();
         width = mPreviewLayout.getWidth();
        this.mIsPortrait = isPortrait;
            if (mIsPortrait) {
                if (width > height * ASPECT_RATIO) {
                    width = (int) (height * ASPECT_RATIO + 0.5);
                } else {
                    height = (int) (width / ASPECT_RATIO + 0.5);
                }
            } else {
                    height = (int) (width / PREVIEW_RATIO + 0.5);
            }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.width= width;
        lp.height = height;
        Log.d(TAG,"[onMeasure]  resize width:"+width+"  height:"+height);
        setLayoutParams(lp);

        RelativeLayout.LayoutParams boosterParamers = (RelativeLayout.LayoutParams) mCamerabooster.getLayoutParams();
        boosterParamers.width= width;
        boosterParamers.height = height;
        mCamerabooster.setLayoutParams(boosterParamers);
        mCamerabooster.setRatio(ratio, width, height);
    }



    public float getRatio(){
        if(ASPECT_RATIO == PREVIEW_RATIO){
             View parnet  = (View) getParent();
            float width = parnet.getMeasuredWidth();
            float height = parnet.getHeight();
            float ratio = width/height;
            return ratio;
        } else {
            return (float) ASPECT_RATIO;
        }
    }

    public int getViewWidth() {
        return getWidth();
    }

    public int getViewHeight() {
        return getHeight();
    }

    public void setCamera(Camera camera) {
        mCamera = camera;

        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            mIsZoomSupported = params.isZoomSupported();
            if (mIsZoomSupported) {
                mMaxZoom = params.getMaxZoom();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mIsFocus = true;

                mLastTouchX = event.getX();
                mLastTouchY = event.getY();

                mActivePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mIsFocus && mIsFocusReady) {
                    handleFocus(mCamera.getParameters());
                }
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                mCamera.cancelAutoFocus();
                mIsFocus = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
        }

        return true;
    }

    private void handleZoom(Camera.Parameters params) {
        int zoom = params.getZoom();
        if (mScaleFactor == ZOOM_IN) {
            if (zoom < mMaxZoom) zoom += ZOOM_DELTA;
        } else if (mScaleFactor == ZOOM_OUT) {
            if (zoom > 0) zoom -= ZOOM_DELTA;
        }
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    private boolean mFocusing = false;


    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(FOCUS_SQR_SIZE * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        mMatrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private void handleFocus(Camera.Parameters params) {
        float x = mLastTouchX;
        float y = mLastTouchY;

//        if (!setFocusBound(x, y)) return;
        Rect fRect = calculateTapArea(x, y, 1f);

        mCamera.cancelAutoFocus();
        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null
                && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.d(TAG, mFocusAreas.size() + "");
            /** test **/
//            params.setFocusAreas(mFocusAreas);
            ArrayList list = new ArrayList();
            list.add(new Camera.Area(fRect, 1000));
//            params.setFocusAreas(list);

            if (!setFocusBound(x, y));
            params.setFocusAreas(mFocusAreas);


//            params.setMeteringAreas(mMeteringAreas);
            if(mFocusAreas != null && mFocusAreas.get(0)!= null){
                Rect focusRect  = mFocusAreas.get(0).rect;
                mCamerabooster.focusView(x,y,focusRect);
            }
            mFocusing = true;
            Log.d(TAG,"[focus] area"+mFocusArea.rect +"  calcute:"+fRect);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d(TAG, "[onAutoFocus]  sucess:" + success);
                    mCamerabooster.focusSuceeed();
                    finishFoucs();
                    // Callback when the auto focus completes
                }
            });
        }
    }

    public  void finishFoucs(){
        mFocusing = false;
        Message msg = new Message();
        msg.what = MSG_FINISH_FOUCS;
        mPreviewHandler.sendMessageDelayed(msg,FOUCS_FINISHED_FADE_DEALY);
    }

    class PreviewHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what){
                case MSG_FINISH_FOUCS:
                    if(mFocusing == false) {
                        mCamerabooster.focusFinish();
                    }
                    break;
            }
        }
    }


    public void setIsFocusReady(final boolean isFocusReady) {
        mIsFocusReady = isFocusReady;
    }

    private boolean setFocusBound(float x, float y) {
        int left = (int) (x - FOCUS_SQR_SIZE / 2);
        int right = (int) (x + FOCUS_SQR_SIZE / 2);
        int top = (int) (y - FOCUS_SQR_SIZE / 2);
        int bottom = (int) (y + FOCUS_SQR_SIZE / 2);

        if (FOCUS_MIN_BOUND > left || left > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > right || right > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > top || top > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > bottom || bottom > FOCUS_MAX_BOUND) return false;

        mFocusArea.rect.set(left, top, right, bottom);
//        mFocusArea.rect.set(900, -1000, 1000, -900);
        mMeteringArea.rect.set(left, top, right, bottom);

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor = (int) detector.getScaleFactor();
            handleZoom(mCamera.getParameters());
            return true;
        }
    }



}
