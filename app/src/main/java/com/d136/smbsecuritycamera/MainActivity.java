package com.d136.smbsecuritycamera;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.d136.smbsecuritycamera.about.AboutActivity;
import com.d136.smbsecuritycamera.preferences.MyPreferencesActivity;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class MainActivity extends AppCompatActivity {

    final static String TAG = "MAIN";

    private EditText editIPv4;
    private Button btnConnect, btnRecord;
    private TextView textConnectionStatus, textRecordingStatus;
    private String ip, port, shareName = "";
    private smbConnection smbConnection;
    private SharedPreferences sharedPreferences;
    private Boolean detectorStarted=false;
    private CustomRecorder customRecorder;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //init layout
        editIPv4 = findViewById(R.id.editIPv4);
        btnConnect = findViewById(R.id.btnConnect);
        btnRecord = findViewById(R.id.btnRecord);
        textConnectionStatus = findViewById(R.id.textConnectionStatus);
        textRecordingStatus = findViewById(R.id.textRecordingStatus);
        SurfaceView preview = findViewById(R.id.surfaceView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        port = sharedPreferences.getString("port","445");
        ip = sharedPreferences.getString("ip","");
        if(!ip.equals(""))
            editIPv4.setText(ip);
        initConnection();

        customRecorder = new CustomRecorder(
                preview,
                this,
                textRecordingStatus,
                sharedPreferences
        );
        customRecorder.setCustomRecorderCallback(new CustomRecorderCallbacks() {
            @Override
            public void onRecorderPrepared() {
                Log.w(TAG,"RECORDER READY!");
                textRecordingStatus.setText(R.string.recording);
                textRecordingStatus.setTextColor(Color.RED);
            }

            @Override
            public void onTimePassed() {
                textRecordingStatus.setText(R.string.serviceStarted);
                textRecordingStatus.setTextColor(Color.GREEN);
                customRecorder.stop();
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onFileSaved() {
                copyToSMB();
                customRecorder.reset();
            }

            @Override
            public void onPrepareFailed() {

            }
        });


        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!detectorStarted)
                {
                    customRecorder.startService();
                    btnRecord.setText(R.string.serviceStarted);
                    detectorStarted = true;
                }
                else
                {
                    customRecorder.pauseService();
                    btnRecord.setText(R.string.RecordBTN);
                    detectorStarted = false;
                }

            }
        });


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = editIPv4.getText().toString();
                if(validIP(ip)){
                    if (smbConnection == null || !smbConnection.isConnected())
                        connect();
                    else {
                        smbConnection.stop();
                        smbConnection.cancel(true);
                        initConnection();
                        customRecorder.disconnectedWhileRunning();
                        btnConnect.setText(R.string.ConnectBTN);
                    }
                }
                else {
                    editIPv4.setError("Wrong ip address");
                }
            }
        });
    }

    private void initConnection() {
        smbConnection = new smbConnection(this);
        SMBConnectionCallback smbConnectionCallback = new SMBConnectionCallback() {
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
            textConnectionStatus.setText(R.string.err_sharenotfound);
        }
        try{
            assert destFile != null;
            destFile.deleteOnClose(); }catch (Exception ignore){}
    }

    private static boolean validIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if (ip.length() < 6 & ip.length() > 15) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        try{
            disableAudio();
        }catch (Exception ignore){}
        customRecorder.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onPause() {
        try{
            enableAudio();
        }catch (Exception ignore){}
        super.onPause();
        customRecorder.onPause();

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
                Intent i = new Intent(this, MyPreferencesActivity.class);
                startActivity(i);
                (this).overridePendingTransition(0, 0);
                return true;
            case R.id.about:
                Intent g = new Intent(this, AboutActivity.class);
                startActivity(g);
                (this).overridePendingTransition(0, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void connect() {
        sharedPreferences.edit().putString("ip",ip).apply();
        String user = sharedPreferences.getString("user", "");
        String password = sharedPreferences.getString("password", "");

        String[] params = {ip, port, user, password, shareName};
        smbConnection.execute(params);
        btnConnect.setText(R.string.DisconnectBTN);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void copyToSMB() {
        java.io.File tmpVideo = customRecorder.getFile();
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void enableAudio(){
        // re-enable sound after recording.
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_ALARM,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_DTMF,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_MUSIC,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_RING,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_VOICE_CALL,false);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void disableAudio() {
        // disable sound when recording.
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_ALARM,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_DTMF,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_MUSIC,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_RING,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_VOICE_CALL,true);

    }

}
