package com.d136.smbsecuritycamera;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.d136.smbsecuritycamera.motiondetection.MotionDetector;
import com.d136.smbsecuritycamera.motiondetection.MotionDetectorCallback;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    final static String TAG = "MAIN";

    //App routine
    private EditText editIPv4, editPort, editUser, editPassword;
    private Button btnConnect, btnRecord;
    private TextView textStatus;
    private String ip, port, user, password, status="Unknown", shareName = "VideoRecorded";
    private smbConnection smbConnection;

    //Video recording
    private MediaRecorder recorder;
    boolean recording = false;
    private java.io.File tmpVideo;
    private Camera camera;
    private SurfaceView preview;
    private MotionDetector motionDetector;
    private int time = 120;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set mute
        ((AudioManager) Objects.requireNonNull(this.getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM,true);
        setContentView(R.layout.activity_main);

        //init layout
        editIPv4 = findViewById(R.id.editIPv4);
        editPort = findViewById(R.id.editPort);
        editUser = findViewById(R.id.editUser);
        editPassword = findViewById(R.id.editPassword);
        btnConnect = findViewById(R.id.btnConnect);
        btnRecord = findViewById(R.id.btnRecord);
        preview = findViewById(R.id.camera_preview);
        textStatus = findViewById(R.id.textStatus);




        //Actions
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                try{ smbConnection.isConnected(); }
                catch (NullPointerException e){
                    connect();
                    return;
                }
                smbConnection = null;
                textStatus.setText("Disconnected");
                textStatus.setTextColor(Color.RED);
                btnConnect.setText("Connect");
            }
        });
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCameraHardware(MainActivity.this))
//                    record();
                    initRoutine();
                else
                    Log.e(TAG,"Device is not supported!");
            }
        });
    }




    // File management
    @RequiresApi(api = Build.VERSION_CODES.KITKAT) //SISTEMARE IP PRIMA DELLA RELEASE
    private void connect() {
//        ip = editIPv4.getText().toString();
        ip = "192.168.1.9";
        port = editPort.getText().toString();
        user = editUser.getText().toString();
        password = editPassword.getText().toString();

        String[] params = {ip, port, user, password, shareName};
        smbConnection = new smbConnection(this);
        smbConnection.execute(params);
        btnConnect.setText("Disconnect");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void copyToSMB() throws IOException {
        DiskShare diskShare = (DiskShare) smbConnection.getSession().connectShare(shareName);
        Set<FileAttributes> fileAttributes = new HashSet<>();
        fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
        Set<SMB2CreateOptions> createOptions = new HashSet<>();
        createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
        String path =  "REC_" + timeStamp + ".mp4";
        File destFile = diskShare.openFile(path,
                new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL)),
                fileAttributes,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                createOptions);

        try (InputStream in = new FileInputStream(tmpVideo)) {
            try (OutputStream out = destFile.getOutputStream()) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
        if(!tmpVideo.delete()) Log.w(TAG,"Something went wrong.");
    }

    private java.io.File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        java.io.File mediaStorageDir = new java.io.File(this.getCacheDir(), "TEMPVideos");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        java.io.File mediaFile;
        mediaFile = new java.io.File(mediaStorageDir.getPath() + java.io.File.separator +
                "VID_"+ timeStamp + ".mp4");
        tmpVideo = mediaFile;
        return mediaFile;
    }



    // Camera and recording
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void record() {
        if (!recording) {
            recording = true;
            prepareVideoRecorder();
            recorder.start();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recorder.stop();
                    releaseMediaRecorder();
                    initRoutine();
                    try {
                        copyToSMB();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    recording = false;
                }
            }, time*1000);
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG,"Camera is not available (in use or does not exist");
        }
        return c; // returns null if camera is unavailable
    }

    private void prepareVideoRecorder(){
        // Step 1: Unlock and set camera to MediaRecorder

        motionDetector.releaseCamera();
        camera = getCameraInstance();

        camera.unlock();
        recorder.setCamera(camera);

        // Step 2: Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        recorder.setOutputFile(getOutputMediaFile()+"");

        // Step 5: Set the preview output
        recorder.setPreviewDisplay(preview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }

    }

    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.reset();   // clear recorder configuration
            recorder.release(); // release the recorder object
            recorder = null;
//            camera.lock();           // lock camera for later use
        }
    }




    //Activity managing

    @Override
    protected void onPause() {
        super.onPause();
        motionDetector.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        initRoutine();
    }

    private void initRoutine() {
        btnRecord.setText("SERVICE STARTED");
        camera = getCameraInstance();
        recorder = new MediaRecorder();
        motionDetector = new MotionDetector(this, preview, camera);
        motionDetector.onResume();
        motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override    public void onMotionDetected() {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                Objects.requireNonNull(v).vibrate(80);
                try {
                    if (smbConnection.isConnected())
                        btnRecord.setText("RECORDING");
                        record();
                }catch(NullPointerException ignored){}
                Log.w(TAG,"Motion detected");
            }

            @Override    public void onTooDark() {
                Log.w(TAG,"Too dark here");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        motionDetector.releaseCamera();
    }
}
