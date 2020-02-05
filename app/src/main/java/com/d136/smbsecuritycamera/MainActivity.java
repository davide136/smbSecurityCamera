package com.d136.smbsecuritycamera;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    final static String TAG = "MAIN";

    //App routine
    private EditText editIPv4;
    private Button btnConnect, btnRecord;
    private TextView textConnectionStatus, textRecordingStatus;
    private String ip, port, user="", password="", shareName = "";
    private java.io.File tmpVideo;
    private smbConnection smbConnection;
    private MotionDetector motionDetector;
    private SharedPreferences sharedPreferences;
    private Boolean detectorStarted=false;
    private SMBConnectionCallback smbConnectionCallback;
    private SurfaceView preview;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //init layout
        editIPv4 = findViewById(R.id.editIPv4);
        btnConnect = findViewById(R.id.btnConnect);
        btnRecord = findViewById(R.id.btnRecord);
        textConnectionStatus = findViewById(R.id.textConnectionStatus);
        textRecordingStatus = findViewById(R.id.textRecordingStatus);
        preview = findViewById(R.id.surfaceView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        port = sharedPreferences.getString("port","445");
        ip = sharedPreferences.getString("ip","");
        if(ip != "")
            editIPv4.setText(ip);
        initConnection();

        motionDetector = new MotionDetector(this, preview);
        motionDetector.setMotionDetectorCallback(new MotionDetectorCallback() {
            @Override
            public void onMotionDetected() {
                Log.w(TAG,"MOTION DETECTED");
            }

            @Override
            public void onTooDark() {
                Log.w(TAG,"TOO DARK");
            }
        });

        ////// Config Options
        //motionDetector.setCheckInterval(500);
        //motionDetector.setLeniency(20);
        //motionDetector.setMinLuma(1000);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!detectorStarted)
                {
                    motionDetector.resumeDetection();
                    btnRecord.setText("Service started");
                    detectorStarted = true;
                }
                else
                {
                    motionDetector.pauseDetection();
                    btnRecord.setText("Record");
                    detectorStarted = false;
                }
            }
        });


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (smbConnection == null || !smbConnection.isConnected()) {
                    connect();
                } else {
                    smbConnection.stop();
                    smbConnection.cancel(true);
                    initConnection();
                    btnConnect.setText("Connect");
                }
            }
        });
    }

    private void initConnection() {
        smbConnection = new smbConnection(this);
        smbConnectionCallback = new SMBConnectionCallback() {
            @Override
            public void onConnectionSuccessful() {
                setShareName();
            }
        };
        smbConnection.setSMBConnectionCallback(smbConnectionCallback);
    }

    private void setShareName() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Insert the name of the share to connect to:");

// Set up the input
        final EditText input = new EditText(this);
        input.setText(sharedPreferences.getString("shareName", null));
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);
// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shareName = input.getText().toString();
                sharedPreferences.edit().putString("shareName",shareName).apply();
                testShareName();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                sharedPreferences.edit().putString("shareName","").apply();
                shareName = "";
            }
        });
        builder.show();
    }

    private void testShareName() {
        DiskShare diskShare;
        File destFile = null;
        try{
            diskShare = (DiskShare) smbConnection.getSession().connectShare(shareName);
            Set<FileAttributes> fileAttributes = new HashSet<>();
            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
            Set<SMB2CreateOptions> createOptions = new HashSet<>();
            createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
            destFile = diskShare.openFile("testRW",
                    new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL)),
                    fileAttributes,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    createOptions);
        }catch(Exception e){
            textConnectionStatus.setTextColor(Color.RED);
            textConnectionStatus.setText("ERR: Share not found!");
        }
        try{ destFile.deleteOnClose(); }catch (Exception ignore){}
    }


    @Override
    protected void onResume() {
        super.onResume();
//        if(detectorStarted)
            motionDetector.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(detectorStarted)
            motionDetector.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, MySettingsActivity.class);
                startActivity(i);
                (this).overridePendingTransition(0, 0);
                return true;
            case R.id.about:
//                about();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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




    // File management
    // SISTEMARE IP PRIMA DELLA RELEASE
    private void connect() {
        ip = editIPv4.getText().toString();
        sharedPreferences.edit().putString("ip",ip).apply();
        user = sharedPreferences.getString("user",null);
        password = sharedPreferences.getString("password",null);

        String[] params = {ip, port, user, password, shareName};
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

}
