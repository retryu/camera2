//public abstract class AbstractCameraFragment extends MDFragment implements UnexpectedTerminationHelper.OnCrashListener, SurfaceHolder.Callback, Camera.AutoFocusCallback {
//
//    protected Camera camera;
//    private boolean previewRunning, cameraReleased, focusAreaSupported, meteringAreaSupported;
//    private UnexpectedTerminationHelper terminationHelper;
//    private int focusAreaSize;
//    private FocusSound focusSound;
//    private Matrix matrix;
//
//    protected ResultListener resultListener;
//
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        resultListener = Utils.as(activity, ResultListener.class);
//        if (resultListener == null) {
//            throw new IllegalArgumentException("Attaching activity must implement ResultListener");
//        }
//    }
//
//    @Override
//    public void onCreate(Bundle sSavedInstanceState) {
//        super.onCreate(sSavedInstanceState);
//        terminationHelper = new UnexpectedTerminationHelper(this);
//        focusAreaSize = getResources().getDimensionPixelSize(R.dimen.camera_focus_area_size);
//        matrix = new Matrix();
//        focusSound = new FocusSound();
//    }
//
//    @Override
//    public void onAutoFocus(boolean focused, Camera camera) {
//        //play default system sound if exists
//        if (focused) {
//            focusSound.play();
//        }
//    }
//
//    @Override
//    public void surfaceCreated(final SurfaceHolder holder) {
//        getSurfaceView().post(() -> openCamera(holder));
//    }
//
//    private void openCamera(SurfaceHolder holder) {
//        try {
//            camera = Camera.open();
//        } catch (Exception e) {
//            Log.e(e);
//        }
//
//        if (camera != null) {
//            cameraReleased = false;
//
//            try {
//                camera.setPreviewDisplay(holder);
//            } catch (Exception e) {
//                Log.e(e);
//            }
//        }
//
//        onCameraOpened();
//    }
//
//    /**
//     * On each tap event we will calculate focus area and metering area.
//     * <p>
//     * Metering area is slightly larger as it should contain more info for exposure calculation.
//     * As it is very easy to over/under expose
//     */
//    protected void focusOnTouch(MotionEvent event) {
//        if (camera != null) {
//            //cancel previous actions
//            camera.cancelAutoFocus();
//            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
//            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
//
//            Camera.Parameters parameters = null;
//            try {
//                parameters = camera.getParameters();
//            } catch (Exception e) {
//                Log.e(e);
//            }
//
//            // check if parameters are set (handle RuntimeException: getParameters failed (empty parameters))
//            if (parameters != null) {
//                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                parameters.setFocusAreas(Collections.newArrayList(new Camera.Area(focusRect, 1000)));
//
//                if (meteringAreaSupported) {
//                    parameters.setMeteringAreas(Collections.newArrayList(new Camera.Area(meteringRect, 1000)));
//                }
//
//                try {
//                    camera.setParameters(parameters);
//                    camera.autoFocus(this);
//                } catch (Exception e) {
//                    Log.e(e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
//     * <p>
//     * Rotate, scale and translate touch rectangle using matrix configured in
//     * {@link SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)}
//     */
//    private Rect calculateTapArea(float x, float y, float coefficient) {
//        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
//
//        int left = clamp((int) x - areaSize / 2, 0, getSurfaceView().getWidth() - areaSize);
//        int top = clamp((int) y - areaSize / 2, 0, getSurfaceView().getHeight() - areaSize);
//
//        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
//        matrix.mapRect(rectF);
//
//        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
//    }
//
//    private int clamp(int x, int min, int max) {
//        if (x > max) {
//            return max;
//        }
//        if (x < min) {
//            return min;
//        }
//        return x;
//    }
//
//    abstract protected void onCameraOpened();
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        getSurfaceView().post(() -> startPreview(holder, width, height));
//    }
//
//    private void startPreview(SurfaceHolder holder, int width, int height) {
//        if (camera == null) {
//            openCamera(holder);
//            if (camera == null) {
//                return;
//            }
//        }
//
//        if (previewRunning) {
//            camera.stopPreview();
//            previewRunning = false;
//        }
//
//        Camera.Parameters p = null;
//        try {
//            p = camera.getParameters();
//        } catch (Exception e) {
//            Log.e(e);
//        }
//
//        // can't get camera params probably called after release
//        if (p == null) {
//            return;
//        }
//
//        p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//        p.setPictureFormat(ImageFormat.JPEG);
//        p.setJpegQuality(GraphicsHelper.DEFAULT_COMPRESSION);
//        Camera.Size optimalPreviewSize = getOptimalPreviewSize(p.getSupportedPreviewSizes(), width, height);
//        Camera.Size optimalPictureSize = getOptimalPreviewSize(p.getSupportedPictureSizes(), width, height);
//
//        if (optimalPreviewSize != null) {
//            p.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
//        }
//
//        if (optimalPictureSize != null) {
//            p.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
//        }
//
//        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }
//
//        int orientation = 0;
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        getCameraInfo(0, info);
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            orientation = (info.orientation + degrees) % 360;
//            orientation = (360 - orientation) % 360;  // compensate the mirror
//        } else {  // back-facing
//            orientation = (info.orientation - degrees + 360) % 360;
//        }
//        camera.setDisplayOrientation(orientation);
//
//        List<String> supportedFocusModes = p.getSupportedFocusModes();
//
//        if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            p.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
//        } else if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_VIDEO)) {
//            p.setFocusMode(FOCUS_MODE_CONTINUOUS_VIDEO);
//        }
//
//        List<String> supportedFlashModes = p.getSupportedFlashModes();
//        if (supportedFlashModes != null && supportedFlashModes.contains(FLASH_MODE_AUTO)) {
//            p.setFlashMode(FLASH_MODE_AUTO);
//        }
//
//        if (p.getMaxNumFocusAreas() > 0) {
//            this.focusAreaSupported = true;
//        }
//
//        if (p.getMaxNumMeteringAreas() > 0) {
//            this.meteringAreaSupported = true;
//        }
//
//        Matrix matrix = new Matrix();
//        matrix.postRotate(orientation);
//        matrix.postScale(width / 2000f, height / 2000f);
//        matrix.postTranslate(width / 2f, height / 2f);
//        matrix.invert(this.matrix);
//    }
//
//    /**
//     * Calculates and returns optimal preview size from supported by each device.
//     */
//    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
//        Camera.Size optimalSize = null;
//
//        for (Camera.Size size : sizes) {
//            if ((size.width <= width && size.height <= height) || (size.height <= width && size.width <= height)) {
//                if (optimalSize == null) {
//                    optimalSize = size;
//                } else {
//                    int resultArea = optimalSize.width * optimalSize.height;
//                    int newArea = size.width * size.height;
//
//                    if (newArea > resultArea) {
//                        optimalSize = size;
//                    }
//                }
//            }
//        }
//
//        return optimalSize;
//    }
//
//    public boolean isFocusAreaSupported() {
//        return focusAreaSupported;
//    }
//
//    public int getFocusAreaSize() {
//        return focusAreaSize;
//    }
//
//    protected abstract CameraPreviewSurfaceView getSurfaceView();
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//        releaseCamera();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (terminationHelper != null) {
//            terminationHelper.finish();
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (terminationHelper != null) {
//            terminationHelper.start();
//        }
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        releaseCamera();
//    }
//
//    private void releaseCamera() {
//        if (camera != null) {
//            try {
//                if (!cameraReleased) {
//                    camera.stopPreview();
//                    previewRunning = false;
//                    camera.release();
//                    cameraReleased = true;
//                    camera = null;
//                }
//            } catch (Exception e) {
//                Log.e("Failed to release android.Camera", e);
//            }
//        }
//    }
//
//    @Override
//    public void lastBreath(Throwable ex) {
//        releaseCamera();
//    }
//
//    public void resumeContinuousAutofocus() {
//        if (camera != null && focusAreaSupported) {
//            camera.cancelAutoFocus();
//
//            Camera.Parameters parameters = camera.getParameters();
//            parameters.setFocusAreas(null);
//
//            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
//
//            String focusMode = null;
//            if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
//                focusMode = FOCUS_MODE_CONTINUOUS_PICTURE;
//            } else if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_VIDEO)) {
//                focusMode = FOCUS_MODE_CONTINUOUS_VIDEO;
//            }
//
//            if (focusMode != null) {
//                parameters.setFocusMode(focusMode);
//                camera.setParameters(parameters);
//            }
//        }
//    }
//
//    private static class FocusSound {
//        private final MediaActionSound media;
//
//        private FocusSound() {
//            if (Utils.osBuildSdkVersionAtLeast(Build.VERSION_CODES.JELLY_BEAN)) {
//                media = new MediaActionSound();
//                media.load(MediaActionSound.FOCUS_COMPLETE);
//            } else {
//                media = null;
//            }
//        }
//
//        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//        public void play() {
//            if (media != null) {
//                media.play(MediaActionSound.FOCUS_COMPLETE);
//            }
//        }
//    }
//
//    public static interface ResultListener {
//        public void onCancel();
//
//        public void onSubmit(List<String> photoUriList);
//    }
//
//}