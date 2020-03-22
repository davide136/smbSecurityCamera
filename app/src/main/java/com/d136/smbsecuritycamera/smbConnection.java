package com.d136.smbsecuritycamera;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;

import java.io.IOException;

public class smbConnection extends AsyncTask<String, Void, Session> {

    final static String TAG = "smbConnection";
    private Session ySession = null;
    private boolean isSuccessful =  false;
    private SMBConnectionCallback smbConnectionCallback;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Session doInBackground(String... strings) {
        String ip = strings[0];
        Session mSession = null;
        int port = 445;
        try{
            port = Integer.valueOf(strings[1]);
        }catch(Exception e){
            Log.e(TAG, "Port selected is not valid! Using default.");
        }
        String user = strings[2];
        String password = strings[3];

        SMBClient client = new SMBClient();

        AuthenticationContext mAc = new AuthenticationContext(user, password.toCharArray(), ip);
        try{
            Connection mConnection = client.connect(ip, port);
            mSession = mConnection.authenticate(mAc);
            isSuccessful = true;
        } catch (IOException e) {e.printStackTrace();}
        return mSession;
    }

    @Override
    protected void onPostExecute(Session session) {
        super.onPostExecute(session);
        if (smbConnectionCallback != null && isSuccessful && session != null){
            smbConnectionCallback.onConnectionSuccessful();
            ySession = session;
        }
        else {
            assert smbConnectionCallback != null;
            smbConnectionCallback.onConnectionFailed();
            ySession = session;
        }
    }

    void setSMBConnectionCallback(SMBConnectionCallback smbConnectionCallback){
        this.smbConnectionCallback = smbConnectionCallback;
    }

    Session getSession(){return ySession;}

    void stop() {
        isSuccessful=false;
        try {
            ySession.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Object connect(String shareName) {
        return ySession.connectShare(shareName);
    }
}