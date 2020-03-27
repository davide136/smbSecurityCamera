package com.d136.smbsecuritycamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import com.d136.smbsecuritycamera.motiondetection.MotionDetector;
import com.d136.smbsecuritycamera.motiondetection.MotionDetectorCallback;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class CustomRecorder {
    private static String TAG="CustomRecorder";
    private final SharedPreferences sharedPreferences;
    private boolean recording = false, cameraLocked = true;
    private MediaRecorder recorder;
    private MotionDetector motionDetector;
    private Context context;
    private CustomRecorderCallbacks customRecorderCallbacks;
    private java.io.File file;



    //CONSTRUCTOR
    CustomRecorder(SurfaceView preview,
                   Context context,
                   SharedPreferences sharedPreferences){
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        motionDetector = new MotionDetector(context,preview);
        motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
            @Override
            public void onMotionDetected() {
                Log.w(TAG,"MOTION DETECTED");
                boolean isConnected = customRecorderCallbacks.isConnected();
                if(!recording && isConnected )
                    start();
            }

            @Override
            public void onTooDark() {
                Log.w(TAG,"TOO DARK");
            }
        });
    }

    //CALLBACK
    void setCustomRecorderCallback(CustomRecorderCallbacks customRecorderCallbacks){
        this.customRecorderCallbacks = customRecorderCallbacks;
    }

    private void start(){
        int time = Integer.valueOf(sharedPreferences.getString("time", "5"));
        motionDetector.setCheckInterval(Integer.valueOf(sharedPreferences.getString("frequency","500")));
        motionDetector.setLeniency(sharedPreferences.getInt("tolerance", 20));
        motionDetector.setMinLuma(sharedPreferences.getInt("luma", 1000));
        prepareVideoRecorder();
        recorder.start();
        recording = true;
        customRecorderCallbacks.recordStarted();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(recording)
                    customRecorderCallbacks.onTimePassed();
            }
        }, time *1000);

    }

    private void getOutputMediaFile(){
        java.io.File mediaStorageDir = new java.io.File(String.valueOf(context.getCacheDir()));

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create temp directory");
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String file_name =  "REC_" + timeStamp + ".mp4";
        file = new java.io.File(mediaStorageDir.getPath() + java.io.File.separator +
                file_name );
    }

    private void prepareVideoRecorder(){
        // Step 1: Unlock and set camera to MediaRecorder
        recorder = new MediaRecorder();
        motionDetector.pauseDetection();
        Camera camera = motionDetector.getmCamera();
        if(cameraLocked)
            camera.unlock();
        recorder.setCamera(camera);

        // Step 2: Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        CharSequence size_char = sharedPreferences.getString("quality","640x480");
        recorder.setVideoSize(
                Integer.valueOf(size_char.toString().split("x")[0]),
                Integer.valueOf(size_char.toString().split("x")[1]));

        // Step 4: Set output file
        getOutputMediaFile();
        recorder.setOutputFile(file+"");

        // Step 5: Set the preview output
        recorder.setPreviewDisplay(motionDetector.getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "!!!!!!!!!IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        } catch (IOException e) {
            Log.e(TAG, "!!!!!!!!!IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }
        customRecorderCallbacks.onRecorderPrepared();
    }

    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.release();   // clear recorder configuration
        }
        customRecorderCallbacks.onPrepareFailed();
    }

    void startService() {
        motionDetector.resumeDetection();
    }

    void pauseService() {
        recording = false;
        cameraLocked = true;
        if(recorder!=null)
            recorder.release();
        motionDetector.pauseDetection();
    }

    void onResume() {
        motionDetector.onResume();
    }

    void onPause() {
        motionDetector.onPause();
    }

    void stop() {
        recorder.stop();
        recorder.release();
        motionDetector.fixCamera();
        customRecorderCallbacks.onFileSaved();
    }

    void reset() {
        recording = false;
        motionDetector.resumeDetection();
    }

    void disconnectedWhileRunning() {
        recording = false;
        cameraLocked = true;
        if(recorder!=null)
            recorder.release();
        pauseService();
    }
}
