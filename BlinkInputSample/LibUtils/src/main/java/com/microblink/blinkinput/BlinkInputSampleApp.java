package com.microblink.input;

import android.app.Application;

import com.microblink.blinkinput.MicroblinkSDK;
import com.microblink.blinkinput.intent.IntentDataTransferMode;

public final class BlinkInputSampleApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // obtain your licence at http://microblink.com/login or
        // contact us at http://help.microblink.com
        MicroblinkSDK.setLicenseFile("com.microblink.input.mblic", this);

        // use optimised way for transferring RecognizerBundle between activities, while ensuring
        // data does not get lost when Android restarts the scanning activity
        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);
    }
}
