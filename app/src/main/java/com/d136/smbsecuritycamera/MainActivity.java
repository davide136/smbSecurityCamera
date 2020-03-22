package com.d136.smbsecuritycamera;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";

    private static int SMB_STATUS = 0, RECORD_SERVICE = 0;
    private static final int CONNECTION_SUCCESSFUL = 129, DISCONNECTED = 128,
            SHARE_FOUND = 127, SHARE_NOT_FOUND =126, CONNECTION_FAILED = 131;
    private static final int RECORDING = 123, SERVICE_RUNNING = 124, SERVICE_STOPPED = 125, SERVICE_PAUSED = 132;

    private EditText editIPv4;
    private Button btnConnect, btnRecord;
    private TextView textConnectionStatus, textRecordingStatus;
    private String ip, port, shareName = "", current_ip = "";
    private smbConnection smbConnection;
    private SharedPreferences sharedPreferences;
    private CustomRecorder customRecorder;
    private boolean skip_share_test = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        checkPermissions();
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
            }

            @Override
            public void onTimePassed() {
                customRecorder.stop();
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onFileSaved() {
                if( getFolderSize(getApplicationContext().getCacheDir()) / (1024*1024) > 0 )  //more than 100 Megabytes
                    new AsyncCopyToSMB().execute();
                customRecorder.reset();
                RECORD_SERVICE = SERVICE_RUNNING;
                updateUI();
            }

            @Override
            public void onPrepareFailed() {
                RECORD_SERVICE = SERVICE_STOPPED;
                updateUI();
            }

            @Override
            public void recordStarted() {
                RECORD_SERVICE = RECORDING;
                updateUI();
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(RECORD_SERVICE){
                    case SERVICE_RUNNING:{
                        customRecorder.pauseService();
                        RECORD_SERVICE = SERVICE_PAUSED;
                        break;
                    }
                    case 0:
                    case SERVICE_PAUSED:
                    case SERVICE_STOPPED:{
                        customRecorder.startService();
                        RECORD_SERVICE = SERVICE_RUNNING;
                        break;
                    }
                    case RECORDING:{
                        customRecorder.stop();
                        customRecorder.pauseService();
                        RECORD_SERVICE = SERVICE_PAUSED;
                        break;
                    }
                }
                updateUI();
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            class AsyncStopSMBConnection extends AsyncTask<Void,Void,Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    smbConnection.stop();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    SMB_STATUS = DISCONNECTED;
                    updateUI();
                }
            }

            @Override
            public void onClick(View v) {
                ip = editIPv4.getText().toString();
                if(validIP(ip)){
                    switch(SMB_STATUS){
                        case CONNECTION_SUCCESSFUL:
                        case SHARE_FOUND:{
                            if( RECORD_SERVICE == RECORDING ){
                                customRecorder.stop();
                                customRecorder.disconnectedWhileRunning();
                            }
                            new AsyncStopSMBConnection().execute();
                            break;
                        }
                        case 0:
                        case CONNECTION_FAILED:
                        case DISCONNECTED:
                        case SHARE_NOT_FOUND: {
                            initConnection();
                            connect();
                            break;
                        }
                    }
                }
                else
                    editIPv4.setError("Wrong ip address");
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static long getFolderSize(java.io.File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (java.io.File child : Objects.requireNonNull(file.listFiles())) {
                size += getFolderSize(child);
            }
        } else {
            size = file.length();
        }
        return size;
    }

    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        )
        {
            Intent intent = new Intent(MainActivity.this, IntroActivity.class);
            startActivity(intent);
            super.finish();
        }
    }

    private void initConnection() {
        smbConnection = new smbConnection();
        SMBConnectionCallback smbConnectionCallback = new SMBConnectionCallback() {
            @Override
            public void onConnectionSuccessful() {
                SMB_STATUS = CONNECTION_SUCCESSFUL;
                if(!current_ip.equals(editIPv4.getText()+""))
                    skip_share_test = false;
                if(!skip_share_test)
                    shareNameAlertDialog();
                updateUI();
            }

            @Override
            public void onConnectionFailed() {
                SMB_STATUS = CONNECTION_FAILED;
                skip_share_test = false;
                updateUI();
            }
        };
        smbConnection.setSMBConnectionCallback(smbConnectionCallback);
    }

    private void shareNameAlertDialog() {
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
                new AsyncTestShareName().execute();
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
        current_ip = ip;
        String user = sharedPreferences.getString("user", "");
        String password = sharedPreferences.getString("password", "");
        String[] params = {ip, port, user, password, shareName};
        smbConnection.execute(params);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void enableAudio(){
        // re-enable sound after RECORD_SERVICE.
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_ALARM,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_DTMF,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_MUSIC,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_RING,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM,false);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_VOICE_CALL,false);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void disableAudio() {
        // disable sound when RECORD_SERVICE.
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_ALARM,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_DTMF,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_MUSIC,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_RING,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM,true);
        ((AudioManager) Objects.requireNonNull(this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_VOICE_CALL,true);

    }

    private class AsyncTestShareName extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... strings) {
            DiskShare diskShare;
            File destFile = null;
            int result = SHARE_FOUND;
            try{
                diskShare = (DiskShare) smbConnection.connect(shareName);
                Set<FileAttributes> fileAttributes = new HashSet<>();
                fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
                Set<SMB2CreateOptions> createOptions = new HashSet<>();
                createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
                if(!diskShare.folderExists("SMB_REC"))
                    diskShare.mkdir("SMB_REC");
                destFile = diskShare.openFile(".mounted_in_SMB_Sec_Camera",
                        new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL)),
                        fileAttributes,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        createOptions);
            }catch(Exception e){
                result = SHARE_NOT_FOUND;
            }
            try{
                assert destFile != null;
                destFile.deleteOnClose(); }catch (Exception ignore){}
            return result;
        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);
            SMB_STATUS = s;
            if(s==SHARE_FOUND)
                skip_share_test = true;
            updateUI();
        }
    }

    private class AsyncCopyToSMB extends AsyncTask<Void, Void, Void> {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {

            java.io.File sourceLocation = getApplicationContext().getCacheDir();
            DiskShare diskShare = (DiskShare) smbConnection.getSession().connectShare(shareName);
            Set<FileAttributes> fileAttributes = new HashSet<>();
            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
            Set<SMB2CreateOptions> createOptions = new HashSet<>();
            createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
            String path = "SMB_REC" ;

            if ( sourceLocation.isDirectory()) {
                String[] children = sourceLocation.list();
                for (int i=0; i<children.length; i++) {

                    String file_name = children[i];
                    File destFile = diskShare.openFile(path + java.io.File.separator + file_name,
                            new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL)),
                            fileAttributes,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            createOptions);
                    try (InputStream in = new FileInputStream(sourceLocation + java.io.File.separator + children[i])) {
                        try (OutputStream out = destFile.getOutputStream()) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.close();
                            in.close();
                        }
                        if( !(new java.io.File(sourceLocation + java.io.File.separator + children[i]).delete()) ) Log.w(TAG,"Something went wrong.");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private void updateUI(){
        switch(SMB_STATUS){
            case CONNECTION_SUCCESSFUL:
            case SHARE_FOUND: {
                textConnectionStatus.setText("Connected");
                textConnectionStatus.setTextColor(Color.GREEN);
                btnConnect.setText(R.string.DisconnectBTN);
                break;
            }
            case DISCONNECTED:{
                textConnectionStatus.setText("Disconnected");
                textConnectionStatus.setTextColor(Color.RED);
                btnConnect.setText(R.string.ConnectBTN);
                break;
            }
            case SHARE_NOT_FOUND:{
                btnConnect.setText(R.string.ConnectBTN);
                textConnectionStatus.setTextColor(Color.RED);
                textConnectionStatus.setText(R.string.err_sharenotfound);
                break;
            }
            case CONNECTION_FAILED:{
                btnConnect.setText(R.string.ConnectBTN);
                textConnectionStatus.setTextColor(Color.RED);
                textConnectionStatus.setText(R.string.err_connfailed);
                break;
            }
        }
        switch(RECORD_SERVICE){
            case RECORDING:{
                textRecordingStatus.setText("RECORDING");
                textRecordingStatus.setTextColor(Color.RED);
                btnRecord.setText(R.string.stop_service);
                break;
            }
            case SERVICE_RUNNING:{
                textRecordingStatus.setText("Service running");
                textRecordingStatus.setTextColor(Color.GREEN);
                btnRecord.setText(R.string.stop_service);
                break;
            }
            case SERVICE_STOPPED:{
                textRecordingStatus.setText("Not running");
                textRecordingStatus.setTextColor(Color.BLUE);
                btnRecord.setText(R.string.start_service);
                break;
            }
            case SERVICE_PAUSED:{
                textRecordingStatus.setText("Paused");
                textRecordingStatus.setTextColor(Color.BLUE);
                btnRecord.setText(R.string.start_service);
                break;
            }
        }
    }
}
