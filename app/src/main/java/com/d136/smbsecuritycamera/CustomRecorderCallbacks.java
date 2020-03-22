package com.d136.smbsecuritycamera;

public interface CustomRecorderCallbacks {
    void onRecorderPrepared();
    void onTimePassed();
    void onFileSaved();
    void onPrepareFailed();
    void recordStarted();
}
