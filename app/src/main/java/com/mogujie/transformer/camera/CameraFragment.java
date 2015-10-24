package com.mogujie.transformer.camera;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.retryu.myapplication.R;

import java.io.IOException;
import java.util.List;

public class CameraFragment extends Fragment implements SurfaceHolder.Callback, Camera.PictureCallback ,CameraFragemntCallBack {

    public static final String TAG = CameraFragment.class.getSimpleName();
    public static final String CAMERA_ID_KEY = "camera_id";
    public static final String CAMERA_FLASH_KEY = "flash_mode";
    public static final String IMAGE_INFO = "image_info";

    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;
    private static final int MSG_RESTART_PREVIEW = 1001;
    private static final int MSG_START_PREVIEW_ASYNC = 1002;
    private static final int MSG_STOP_PREVIEW_ASYNC = 1003;

    private static final int RESUME_PREVIEW_TIME = 500;

    private int mCameraID;
    private String mFlashMode;
    private Camera mCamera;
    private MgCameraPreview mPreviewView;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsSafeToTakePhoto = false;
    private ImageParameters mImageParameters;
    private CameraOrientationListener mOrientationListener;
    //相机分割线和比例框
    private Camerabooster mCamerabooster;
    private CameraHandler mCameraHandler;

    public static Fragment newInstance() {
        return new CameraFragment();
    }

    public CameraFragment() {
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(getActivity());
//        mOrientationListener = new CameraOrientationListener(context);
//    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mOrientationListener = new CameraOrientationListener(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore your state here because a double rotation with this fragment
        // in the backstack will cause improper state restoration
        // onCreate() -> onSavedInstanceState() instead of going through onCreateView()
        if (savedInstanceState == null) {
            mCameraID = getBackCameraID();
            mFlashMode = CameraSettingPreferences.getCameraFlashMode(getActivity());
            mImageParameters = new ImageParameters();
        } else {
            mCameraID = savedInstanceState.getInt(CAMERA_ID_KEY);
            mFlashMode = savedInstanceState.getString(CAMERA_FLASH_KEY);
            mImageParameters = savedInstanceState.getParcelable(IMAGE_INFO);
        }
        mCameraHandler = new CameraHandler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    View mTopCoverView;
    View mBtnCoverView;
    private ImageView mBtnRatio;
    private ImageView mBtnBooster;
    private View mChangeCameraFlashModeBtn;
    private ImageView mBtnFlash;
    private RelativeLayout previewLayout;


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOrientationListener.enable();

        mPreviewView = (MgCameraPreview) view.findViewById(R.id.camera_preview_view);
        mCamerabooster = (Camerabooster) view.findViewById(R.id.cameraBooster);
        previewLayout = (RelativeLayout) view.findViewById(R.id.preview_layout);




        mPreviewView.mPreviewLayout = previewLayout;
        mPreviewView.mCamerabooster = mCamerabooster;
        mPreviewView.getHolder().addCallback(CameraFragment.this);

        mTopCoverView = view.findViewById(R.id.cover_top_view);
        mBtnCoverView = view.findViewById(R.id.cover_bottom_view);
        mBtnRatio = (ImageView) view.findViewById(R.id.preview_ratio);
        mBtnBooster = (ImageView) view.findViewById(R.id.btn_booster);


        mImageParameters.mIsPortrait =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        Log.d(TAG, "onViewCreated");
        if (savedInstanceState == null) {
            ViewTreeObserver observer = mPreviewView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mImageParameters.mPreviewWidth = mPreviewView.getWidth();
                    mImageParameters.mPreviewHeight = mPreviewView.getHeight();

                    mImageParameters.mCoverWidth = mImageParameters.mCoverHeight
                            = mImageParameters.calculateCoverWidthHeight();

//                    Log.d(TAG, "parameters: " + mImageParameters.getStringValues());
//                    Log.d(TAG, "cover height " + topCoverView.getHeight());
                    resizeTopAndBtmCover(mTopCoverView, mBtnCoverView);
//                    resizeCameraBooster();

                    mPreviewView.resizeViewByRatio((float) MgCameraPreview.PREVIEW_RATIO,false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mPreviewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        mPreviewView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
        } else {
            if (mImageParameters.isPortrait()) {
                mTopCoverView.getLayoutParams().height = mImageParameters.mCoverHeight;
                mBtnCoverView.getLayoutParams().height = mImageParameters.mCoverHeight;
            } else {
                mTopCoverView.getLayoutParams().width = mImageParameters.mCoverWidth;
                mBtnCoverView.getLayoutParams().width = mImageParameters.mCoverWidth;
            }
        }

        final ImageView swapCameraBtn = (ImageView) view.findViewById(R.id.change_camera);
        mBtnFlash = (ImageView) view.findViewById(R.id.flash_icon);
        swapCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraID == CameraInfo.CAMERA_FACING_FRONT) {
                    swapCameraBtn.setImageResource(R.drawable.ic_camera_back);
                    mCameraID = getBackCameraID();
//                    showFlash();
                } else {
                    swapCameraBtn.setImageResource(R.drawable.ic_camera_font);
                    mCameraID = getFrontCameraID();
//                    hideFlash();
                }
                restartPreview();
            }
        });

        mChangeCameraFlashModeBtn =  view.findViewById(R.id.flash);
        mChangeCameraFlashModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)) {
                    mFlashMode = Camera.Parameters.FLASH_MODE_ON;
                    mBtnFlash.setImageResource(R.drawable.ic_camera_flash_on);
                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)) {
                    mBtnFlash.setImageResource(R.drawable.ic_camera_flash_on);
                    mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)) {
                    mBtnFlash.setImageResource(R.drawable.ic_camera_flash_off);
                    mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
                }

                setupFlashMode();
                setupCamera();
            }
        });


        mBtnRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviewSize();
            }
        });
        mBtnBooster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                printSize();
                if (!mCamerabooster.isBoosterShow()) {
                    mBtnBooster.setImageResource(R.drawable.btn_camera_booster_on);
                    mCamerabooster.showBooster(true);
                } else {
                    mBtnBooster.setImageResource(R.drawable.btn_camera_booster_off);
                    mCamerabooster.showBooster(false);
                }

            }

        });

        setupFlashMode();

        final ImageView takePhotoBtn = (ImageView) view.findViewById(R.id.capture_image_button);
        mCamerabooster.setBtnTakePhoto(takePhotoBtn);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });


    }


    public void hideFlash(){
        mChangeCameraFlashModeBtn.setClickable(false);
        mBtnFlash.setImageResource(R.drawable.ic_camera_flash_off);
    }

    public void showFlash(){
        mChangeCameraFlashModeBtn.setClickable(true);
        if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)) {
            mBtnFlash.setImageResource(R.drawable.ic_camera_flash_on);
        } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)) {
            mBtnFlash.setImageResource(R.drawable.ic_camera_flash_on);
        } else if (mFlashMode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)) {
            mBtnFlash.setImageResource(R.drawable.ic_camera_flash_off);
        }

    }

    private void setupFlashMode() {
        View view = getView();
        if (view == null) return;

        final TextView autoFlashIcon = (TextView) view.findViewById(R.id.auto_flash_icon);
        if (Camera.Parameters.FLASH_MODE_AUTO.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("Auto");
        } else if (Camera.Parameters.FLASH_MODE_ON.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("On");
        } else if (Camera.Parameters.FLASH_MODE_OFF.equalsIgnoreCase(mFlashMode)) {
            autoFlashIcon.setText("Off");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        Log.d(TAG, "onSaveInstanceState");
        outState.putInt(CAMERA_ID_KEY, mCameraID);
        outState.putString(CAMERA_FLASH_KEY, mFlashMode);
        outState.putParcelable(IMAGE_INFO, mImageParameters);
        super.onSaveInstanceState(outState);
    }

    private void resizeTopAndBtmCover(final View topCover, final View bottomCover) {
        ResizeAnimation resizeTopAnimation
                = new ResizeAnimation(topCover, mImageParameters);
        resizeTopAnimation.setDuration(800);
        resizeTopAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        topCover.startAnimation(resizeTopAnimation);

        ResizeAnimation resizeBtmAnimation
                = new ResizeAnimation(bottomCover, mImageParameters);
        resizeBtmAnimation.setDuration(800);
        resizeBtmAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        bottomCover.startAnimation(resizeBtmAnimation);
    }

    private void getCamera(int cameraID) {
        try {
            mCamera = Camera.open(cameraID);
            mPreviewView.setCamera(mCamera);
        } catch (Exception e) {
            Log.d(TAG, "Can't open camera with id " + cameraID);
            e.printStackTrace();
        }
    }

    /**
     * Restart the camera preview
     */
    private void restartPreview() {
        stopCamera();

        getCamera(mCameraID);
        startCameraPreview();
    }


    /**
     * Stop the camera preview
     */
    private void stopCameraPreview() {
        setSafeToTakePhoto(false);
        setCameraFocusReady(false);

        // Nulls out callbacks, stops face detection
        mCamera.stopPreview();
        mPreviewView.setCamera(null);
    }

    private void setSafeToTakePhoto(final boolean isSafeToTakePhoto) {
        mIsSafeToTakePhoto = isSafeToTakePhoto;
    }

    private void setCameraFocusReady(final boolean isFocusReady) {
        if (this.mPreviewView != null) {
            mPreviewView.setIsFocusReady(isFocusReady);
        }
    }


    private  int orientation = 0;
    /**
     * Determine the current display orientation and rotate the camera preview
     * accordingly
     */
    private void determineDisplayOrientation() {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);

        // Clockwise rotation needed to align the window display to the natural position
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: {
                degrees = 0;
                break;
            }
            case Surface.ROTATION_90: {
                degrees = 90;
                break;
            }
            case Surface.ROTATION_180: {
                degrees = 180;
                break;
            }
            case Surface.ROTATION_270: {
                degrees = 270;
                break;
            }
        }

        int displayOrientation;

        // CameraInfo.Orientation is the angle relative to the natural position of the device
        // in clockwise rotation (angle that is rotated clockwise from the natural position)
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            // Orientation is angle of rotation when facing the camera for
            // the camera image to match the natural orientation of the device
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        mImageParameters.mDisplayOrientation = displayOrientation;
        mImageParameters.mLayoutOrientation = degrees;
        orientation = degrees;

        if(mCamera != null && mImageParameters != null) {
            mCamera.setDisplayOrientation(mImageParameters.mDisplayOrientation);
        }
    }

    /**
     * Setup the camera parameters
     */
    private void setupCamera() {
        // Never keep a global parameters

        Camera.Parameters parameters = mCamera.getParameters();

        Size bestPreviewSize = determineBestPreviewSize(parameters);
        Size bestPictureSize = determineBestPictureSize(parameters);


        printSize();


        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
//        parameters.setPreviewSize(100, 100);
        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);


        // Set continuous picture focus, if it's supported
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        final View changeCameraFlashModeBtn = getView().findViewById(R.id.flash);
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(mFlashMode)) {
            parameters.setFlashMode(mFlashMode);
//            changeCameraFlashModeBtn.setVisibility(View.VISIBLE);
//            showFlash();

        } else {
//            hideFlash();
//            changeCameraFlashModeBtn.setVisibility(View.GONE);
        }

        // Lock in the changes
        mCamera.setParameters(parameters);
    }

    private void printSize() {
        Camera.Parameters parameters = mCamera.getParameters();

        Size bestPreviewSize = determineBestPreviewSize(parameters);
        Size bestPictureSize = determineBestPictureSize(parameters);

        Log.d(TAG, "[setupCamera]  preview.width:" + bestPreviewSize.width + "  preview.height:" + bestPreviewSize.height);
        Log.d(TAG, "[setupCamera]  picture.width:" + bestPictureSize.width + "  picture.height:" + bestPictureSize.height);
    }

    public boolean isSquare = false;

    public static float RARIO_34 = 3f / 4f;
    public static float RATIPO_11 = 1f;

    public void setPreviewSize() {

        if (isSquare == true) {
            mBtnRatio.setImageResource(R.drawable.btn_camera_ratio_11);
            mPreviewView.resizeViewByRatio(RARIO_34, false);
            isSquare = false;
        } else {
            mBtnRatio.setImageResource(R.drawable.btn_camera_ratio_34);
            mPreviewView.resizeViewByRatio(RATIPO_11, false);
            isSquare = true;
        }
    }

    private Size determineBestPreviewSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPreviewSizes(), PREVIEW_SIZE_MAX_WIDTH);
    }

    private Size determineBestPictureSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPictureSizes(), PICTURE_SIZE_MAX_WIDTH);
    }

    private Size determineBestSize(List<Size> sizes, int widthThreshold) {
        Size bestSize = null;
        Size size;
        int numOfSizes = sizes.size();
        for (int i = 0; i < numOfSizes; i++) {
            size = sizes.get(i);
            boolean isDesireRatio = (size.width / 4) == (size.height / 3);
            boolean isBetterSize = (bestSize == null) || size.width > bestSize.width;

            if (isDesireRatio && isBetterSize) {
                bestSize = size;
            }
        }

        if (bestSize == null) {
            Log.d(TAG, "cannot find the best camera size");
            return sizes.get(sizes.size() - 1);
        }

        return bestSize;
    }

    private int getFrontCameraID() {
        PackageManager pm = getActivity().getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return CameraInfo.CAMERA_FACING_FRONT;
        }

        return getBackCameraID();
    }

    private int getBackCameraID() {
        return CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * Take a picture
     */
    private void takePicture() {

        if (mIsSafeToTakePhoto) {
            setSafeToTakePhoto(false);

            mOrientationListener.rememberOrientation();

            // Shutter callback occurs after the image is captured. This can
            // be used to trigger a sound to let the user know that image is taken
            Camera.ShutterCallback shutterCallback = null;

            // Raw callback occurs when the raw image data is available
            Camera.PictureCallback raw = null;

            // postView callback occurs when a scaled, fully processed
            // postView image is available.
            Camera.PictureCallback postView = null;
            Camera.Parameters parameters = mCamera.getParameters();
            Log.d("debug", "  p.Width:" + parameters.getPictureSize().width + "  p.height:" + parameters.getPictureSize().height);
            Log.d(TAG, "[takePictoccursure]");
            // jpeg callback  when the compressed image is available

            mCamera.takePicture(shutterCallback, raw, postView, this);


        }
    }


    /**
     * A picture has been taken
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        setSafeToTakePhoto(true);
        Log.d(TAG, "[takePicture]  onPictureTaken");
        savePicture(data);
        resumePreviewDelayed();
    }

    /**
     * 延时恢复相机
     */
    public void resumePreviewDelayed() {
        if (mCameraHandler != null) {
            Message msg = new Message();
            msg.what = MSG_RESTART_PREVIEW;
            mCameraHandler.sendMessageDelayed(msg, RESUME_PREVIEW_TIME);
        }
    }

    @Override
    public void updateIndicatorCount(int count) {

    }

    @Override
    public void finishCamer() {

    }


    class CameraHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MSG_RESTART_PREVIEW:
                    if (mCamera != null) {
                        mCamera.startPreview();
                    }
                    break;
                case MSG_START_PREVIEW_ASYNC:
                    new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            startCamera();
                            Log.e(TAG,"start Camera Finish");
                        }
                    }.start();

                    break;


            }
        }
    }

    class SwapCamerTasker extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            startCamera();
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "[onResume]  start");
//        mCameraHandler.sendEmptyMessage(MSG_START_PREVIEW_ASYNC);
//        startCamera();
        SwapCamerTasker swapCamerTasker = new SwapCamerTasker();
        swapCamerTasker.execute();

//        startCamera();

//        restartPreview();
        Log.d(TAG, "[onResume]  end");
    }


    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        determineDisplayOrientation();
        setupCamera();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

            setSafeToTakePhoto(true);
            setCameraFocusReady(true);
        } catch (IOException e) {
            Log.d(TAG, "Can't start camera preview due to IOException " + e);
            e.printStackTrace();
        }

    }

    public void startCamera() {
        if (mCamera == null) {
            restartPreview();
        }
    }

    @Override
    public void onStop() {
        Log.e(TAG,"[onStop] onStop start");
        mOrientationListener.disable();
        stopCamera();
        CameraSettingPreferences.saveCameraFlashMode(getActivity(), mFlashMode);

        super.onStop();
        Log.e(TAG, "[onStop] onStop finish");
    }

    private void stopCamera() {
        // stop the preview
        if (mCamera != null) {
            stopCameraPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;

//        getCamera(mCameraID);
//        startCameraPreview();
    }

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("[surfaceChanged]", "[]");
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        matrix.postScale(mSurfaceWidth / 2000f, mSurfaceHeight / 2000f);
        matrix.postTranslate(mSurfaceWidth / 2f, mSurfaceHeight / 2f);
        matrix.invert(mPreviewView.mMatrix);

        if(mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            Size bestPreviewSize = determineBestPreviewSize(parameters);
            Size bestPictureSize = determineBestPictureSize(parameters);
            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
            parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
            mPreviewView.requestLayout();
            mCamera.setParameters(parameters);

            // Important: Call startPreview() to start updating the preview surface.
            // Preview must be started before you can take a picture.
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // The surface is destroyed with the visibility of the SurfaceView is set to View.Invisible
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case 1:
                Uri imageUri = data.getData();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }


    /**
     * 保存图片到本地
     *
     * @param data
     * @return
     */
    private boolean savePicture(byte[] data) {
        printSize();
        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(getActivity(), data);
        int rotation = getPhotoRotation();
//        Log.d(TAG, "original bitmap width " + bitmap.getWidth() + " height " + bitmap.getHeight());
        if (rotation != 0) {
            Bitmap oldBitmap = bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            try {
                bitmap = Bitmap.createBitmap(
                        oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, false
                );

                oldBitmap.recycle();
                float ratio = mPreviewView.getRatio();
                ImageUtility.savePictureByRatio(getActivity(), bitmap, ratio);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private int getPhotoRotation() {
        int rotation;
        int orientation = mOrientationListener.getRememberedNormalOrientation();
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mCameraID, info);

        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {
            rotation = (info.orientation + orientation) % 360;
        }

        return rotation;
    }

    /**
     * When orientation changes, onOrientationChanged(int) of the listener will be called
     */
    private static class CameraOrientationListener extends OrientationEventListener {

        private int mCurrentNormalizedOrientation;
        private int mRememberedNormalOrientation;

        public CameraOrientationListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation != ORIENTATION_UNKNOWN) {
                mCurrentNormalizedOrientation = normalize(orientation);
            }
        }

        /**
         * @param degrees Amount of clockwise rotation from the device's natural position
         * @return Normalized degrees to just 0, 90, 180, 270
         */
        private int normalize(int degrees) {
            if (degrees > 315 || degrees <= 45) {
                return 0;
            }

            if (degrees > 45 && degrees <= 135) {
                return 90;
            }

            if (degrees > 135 && degrees <= 225) {
                return 180;
            }

            if (degrees > 225 && degrees <= 315) {
                return 270;
            }

            throw new RuntimeException("The physics as we know them are no more. Watch out for anomalies.");
        }

        public void rememberOrientation() {
            mRememberedNormalOrientation = mCurrentNormalizedOrientation;
        }

        public int getRememberedNormalOrientation() {
            rememberOrientation();
            return mRememberedNormalOrientation;
        }
    }
}
