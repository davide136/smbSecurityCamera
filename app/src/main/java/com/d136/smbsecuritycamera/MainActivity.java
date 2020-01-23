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
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    final static String TAG = "MAIN";
    private static com.d136.smbsecuritycamera.smbConnection smbConnection;

    public static smbConnection getSmbConnection(){
        return smbConnection;
    }

    //App routine
    private EditText editIPv4, editPort, editUser, editPassword;
    private Button btnConnect, btnRecord;
    private static TextView textConnectionStatus, textRecordingStatus;
    private String ip = "192.168.1.102", port, user, password, shareName = "Download";

    //Video recording
    private MediaRecorder recorder;
    private static boolean recording = false, serviceRunning = false, firstRun=true, enoughTimeHasPassed = true;
    private java.io.File tmpVideo;
    private Camera camera;
    private SurfaceView preview;
    private MotionDetector motionDetector;
    private int time = 5;
    private long lastRecordingTime;
    private int counter=0;

    public static TextView getTextStatus() {
        return textRecordingStatus;
    }

    public static boolean getRecording() {
        return recording;
    }

    public static boolean getServiceRunning() {
        return serviceRunning;
    }

    public static boolean getFirstRun() {
        return firstRun;
    }

    public static boolean getenoughTimeHasPassed() {
        return enoughTimeHasPassed;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        //init layout
        editIPv4 = findViewById(R.id.editIPv4);
        editPort = findViewById(R.id.editPort);
        editUser = findViewById(R.id.editUser);
        editPassword = findViewById(R.id.editPassword);
        btnConnect = findViewById(R.id.btnConnect);
        btnRecord = findViewById(R.id.btnRecord);
        textConnectionStatus = findViewById(R.id.textConnectionStatus);
        textRecordingStatus = findViewById(R.id.textRecordingStatus);
        preview = findViewById(R.id.camera_preview);

/*        recorder = new MediaRecorder();
        if(camera==null)
            camera = getCameraInstance();*/
        if(motionDetector == null)
            motionDetector = new MotionDetector(this, preview);


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
                textConnectionStatus.setText("Disconnected");
                textConnectionStatus.setTextColor(Color.RED);
                btnConnect.setText("Connect");
            }
        });
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCameraHardware(MainActivity.this)) {
                    if(!serviceRunning){
                        btnRecord.setText("STOP SERVICE");
                        serviceRunning = true;
                        recording = false;
                        motionDetectorCallback();
                        textRecordingStatus.setText("SERVICE RUNNING");
                        textRecordingStatus.setTextColor(Color.BLUE);
                        motionDetector.onResume();
                    }
                    else{
                        btnRecord.setText("START SERVICE");
                        serviceRunning = false;
                        textRecordingStatus.setText("SERVICE STOPPED");
                        textRecordingStatus.setTextColor(Color.RED);
                        recording = false;
                        motionDetector.onPause();
                    }
                }
                else
                    Log.e(TAG,"Device is not supported!");
            }
        });
    }





    // File management
    @RequiresApi(api = Build.VERSION_CODES.KITKAT) //SISTEMARE IP PRIMA DELLA RELEASE
    private void connect() {
//        ip = editIPv4.getText().toString();
        port = editPort.getText().toString();
        user = editUser.getText().toString();
        password = editPassword.getText().toString();

        String[] params = {ip, port, user, password, shareName};
        smbConnection = new smbConnection(this);
        smbConnection.execute(params);
        btnConnect.setText("Disconnect");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void copyToSMB() {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
                    stopRecorder();
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
            resetMediaRecorder();
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            resetMediaRecorder();
        }

    }

    private void resetMediaRecorder(){
        if (recorder != null) {
            recorder.reset();   // clear recorder configuration
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void stopRecorder() {
        //end recording and move to smb
        recorder.stop();
        Log.w(TAG,"STOPPED RECORDING");
        copyToSMB();
        Log.w(TAG,"MOVED TO SMB");
        resetMediaRecorder();
        lastRecordingTime = System.currentTimeMillis();
        textRecordingStatus.setText("SERVICE RUNNING");
        textRecordingStatus.setTextColor(Color.BLUE);
        recording = false;
    }




    //Activity managing
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    void initRoutine() {
        if (!firstRun) {
            enoughTimeHasPassed = false;
            long now = System.currentTimeMillis();
            if ( (now -lastRecordingTime) > 500 )
                enoughTimeHasPassed = true;
        }


        if (smbConnection.isConnected() & !recording & serviceRunning & enoughTimeHasPassed){
            textRecordingStatus.setText("RECORDING");
            textRecordingStatus.setTextColor(Color.GREEN);
            firstRun = false;
            Log.w(TAG,"RECORD STARTED");
            record();
        }
    }

    void motionDetectorCallback(){
        motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override    public void onMotionDetected() {
                try {
                    motionDetector.initRoutine();
                }catch(NullPointerException ignored){}
                Log.w(TAG,"Motion detected");
            }

            @Override
            public void onTooDark() {
                Log.w(TAG,"Too dark here");
            }

            @Override
            public void logCallback() {
                Log.w(TAG, "call number "+counter);
                counter ++;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        enableAudio();
        motionDetector.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableAudio();
        motionDetector.onResume();

    }

    private void enableAudio(){
        // re-enable sound after recording.
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM,false);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF,false);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC,false);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING,false);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM,false);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL,false);
    }

    private void disableAudio() {
        // disable sound when recording.
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM,true);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF,true);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC,true);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING,true);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM,true);
        ((AudioManager)this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL,true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.release();
        recorder.release();
    }
}
