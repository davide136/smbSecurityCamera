package com.d136.smbsecuritycamera.motiondetection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MotionDetector {
    private boolean workerIsRunning = true;
    private boolean firstRun = true;

    public Surface getSurface() {
        return mSurface.getHolder().getSurface();
    }

    private int mWidth, mHeight;
    public void fixCamera(){
        try {
            mCamera.setPreviewDisplay(previewHolder);
            mCamera.setPreviewCallback(previewCallback);
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(mWidth, mHeight, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("MotionDetector", "Using width=" + size.width + " height=" + size.height);
            }
            mCamera.setParameters(parameters);


        } catch (Throwable t) {
            Log.e("MotionDetector", "Exception in setPreviewDisplay()", t);
        }


    }

    class MotionDetectorThread extends Thread {
        private AtomicBoolean isRunning = new AtomicBoolean(true);
        private volatile boolean paused = false;
        private volatile boolean running = true;
        private final Object pauseLock = new Object();



        void stopDetection() {
            isRunning.set(false);
        }

        @Override
        public void run() {

            while (running) {
                synchronized (pauseLock) {
                    if (!running) { // may have changed while waiting to
                        // synchronize on pauseLock
                        break;
                    }
                    if (paused) {
                        try {
                            synchronized (pauseLock) {
                                pauseLock.wait(); // will cause this Thread to block until
                                // another thread calls pauseLock.notifyAll()
                                // Note that calling wait() will
                                // relinquish the synchronized lock that this
                                // thread holds on pauseLock so another thread
                                // can acquire the lock to call notifyAll()
                                // (link with explanation below this code)
                            }
                        } catch (InterruptedException ex) {
                            break;
                        }
                        if (!running) { // running might have changed since we paused
                            break;
                        }
                    }
                }
                motionDetectCode();
            }


        }

        private void motionDetectCode() {

                long now = System.currentTimeMillis();
                if (now-lastCheck > checkInterval) {
                    lastCheck = now;

                    if (nextData.get() != null) {
                        int[] img = ImageProcessing.decodeYUV420SPtoLuma(nextData.get(), nextWidth.get(), nextHeight.get());

                        // check if it is too dark
                        int lumaSum = 0;
                        for (int i : img) {
                            lumaSum += i;
                        }
                        if (lumaSum < minLuma) {
                            if (motionDetectorCallback != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        motionDetectorCallback.onTooDark();
                                    }
                                });
                            }
                        } else if (detector.detect(img, nextWidth.get(), nextHeight.get())) {
                            // check
                            if (motionDetectorCallback != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        motionDetectorCallback.onMotionDetected();
                                    }
                                });
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

        }

        void pause() {
            paused = true;
        }

        void resumE() {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll(); // Unblocks thread
            }
        }

    }

    private final AggregateLumaMotionDetection detector;
    private long checkInterval = 500;
    private long lastCheck = 0;
    private MotionDetectorCallback motionDetectorCallback;
    private Handler mHandler = new Handler();

    private AtomicReference<byte[]> nextData = new AtomicReference<>();
    private AtomicInteger nextWidth = new AtomicInteger();
    private AtomicInteger nextHeight = new AtomicInteger();
    private int minLuma = 1000;
    private MotionDetectorThread worker;

    private Camera mCamera;
    private boolean inPreview;
    private SurfaceHolder previewHolder;
    private Context mContext;
    private SurfaceView mSurface;

    public MotionDetector(Context context, SurfaceView previewSurface) {
        detector = new AggregateLumaMotionDetection();
        mContext = context;
        mSurface = previewSurface;
    }

    public void setMotionDetectorCallback(MotionDetectorCallback motionDetectorCallback) {
        this.motionDetectorCallback = motionDetectorCallback;
    }

    private void consume(byte[] data, int width, int height) {
        nextData.set(data);
        nextWidth.set(width);
        nextHeight.set(height);
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void setMinLuma(int minLuma) {
        this.minLuma = minLuma;
    }

    public void setLeniency(int l) {
        detector.setLeniency(l);
    }

    public void onResume() {
        if (checkCameraHardware()) {
            mCamera = getCameraInstance();

            worker = new MotionDetectorThread();
            worker.start();
            if(firstRun)
                worker.pause();


            // configure preview
            previewHolder = mSurface.getHolder();
            previewHolder.addCallback(surfaceCallback);
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private boolean checkCameraHardware() {
        // this device has a camera
        // no camera on this device
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public Camera getmCamera(){return mCamera;}

    private Camera getCameraInstance(){
        Camera c = null;

        try {
            c = Camera.open();
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            //txtStatus.setText("Kamera nicht zur Benutzung freigegeben");
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;

            consume(data, size.width, size.height);
        }
    };


    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(previewHolder);
                mCamera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.e("MotionDetector", "Exception in setPreviewDisplay()", t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mWidth = width;
            mHeight = height;
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(mWidth, mHeight, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("MotionDetector", "Using width=" + size.width + " height=" + size.height);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            inPreview = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }
    };

    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }

    public void pauseDetection(){
        if (workerIsRunning)
        {
            worker.pause();
            workerIsRunning = false;
        }
    }

    public void resumeDetection(){
        if (firstRun || !workerIsRunning) {
            worker.resumE();
            workerIsRunning = true;
        }
    }

    public void onPause() {
        releaseCamera();
        if (previewHolder != null) previewHolder.removeCallback(surfaceCallback);
        if (worker != null) worker.stopDetection();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            if (inPreview) mCamera.stopPreview();
            inPreview = false;
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
}