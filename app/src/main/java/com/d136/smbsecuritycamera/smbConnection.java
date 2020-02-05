package com.d136.smbsecuritycamera;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;

import java.io.IOException;

public class smbConnection extends AsyncTask<String, Void, Void> {

    final static String TAG = "smbConnection";

    private boolean isSuccessful =  false;
    private Session mSession;
    private TextView status;
    private Activity mActivity;
    private Connection mConnection;
    private AuthenticationContext mAc;
    private SMBConnectionCallback smbConnectionCallback;


    smbConnection(Activity mainActivity) {
        mActivity = mainActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Void doInBackground(String... strings) {
        String ip = strings[0];
        int port = 445;
        try{
            port = Integer.valueOf(strings[1]);
        }catch(Exception e){
            Log.e(TAG, "Port selected is not valid! Using default.");
        }
        String user = strings[2];
        String password = strings[3];

        SMBClient client = new SMBClient();

        mAc = new AuthenticationContext(user, password.toCharArray(), ip);
        try{
            mConnection = client.connect(ip,port);
            mSession = mConnection.authenticate(mAc);
            isSuccessful = true;
        } catch (IOException e) {e.printStackTrace();}
        return null;
    }

    @Override
    protected void onPostExecute(Void session) {
        super.onPostExecute(session);
        if (smbConnectionCallback != null )
            smbConnectionCallback.onConnectionSuccessful();
        updateUI();
    }

    public void setSMBConnectionCallback(SMBConnectionCallback smbConnectionCallback){
        this.smbConnectionCallback = smbConnectionCallback;
    }

    public void updateUI() {
        status = mActivity.findViewById(R.id.textConnectionStatus);
        if(!isSuccessful) {
            status.setText("Disconnected");
            status.setTextColor(Color.RED);
            return;
        }
        status.setText("Connected");
        status.setTextColor(Color.GREEN);
    }

    public boolean isConnected(){ return isSuccessful; }

    Session getSession(){return mSession;}

    public void stop() {
        isSuccessful=false;
        try {
            mSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateUI();
    }
}