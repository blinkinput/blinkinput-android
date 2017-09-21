package com.microblink.ocr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.microblink.directApi.DirectApiErrorListener;
import com.microblink.directApi.Recognizer;
import com.microblink.geometry.Rectangle;
import com.microblink.hardware.SuccessCallback;
import com.microblink.hardware.camera.VideoResolutionPreset;
import com.microblink.image.Image;
import com.microblink.metadata.ImageMetadata;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.recognition.FeatureNotSupportedException;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkinput.BlinkInputRecognitionResult;
import com.microblink.recognizers.blinkinput.BlinkInputRecognizerSettings;
import com.microblink.recognizers.blinkocr.parser.generic.RawParserSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.results.ocr.CharWithVariants;
import com.microblink.results.ocr.OcrChar;
import com.microblink.results.ocr.OcrResult;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;

public class CombinationScanActivity extends Activity implements CameraEventsListener, ScanResultListener, MetadataListener {

    // obtain your licence key at http://microblink.com/login or
    // contact us at http://help.microblink.com
    private static final String LICENSE_KEY = "GZLX6RM4-KUOPKVFO-F27ZHP23-GKFVGELE-GXCYIOHW-DNT6JOYT-RNJRDRDR-CTHZ4N3O";

    /** RecognizerView is the built-in view that controls camera and recognition */
    private RecognizerView mRecognizerView;
    /** Button that controls flashlight state. */
    private ImageButton mFlashButton;
    /** Flashlight state. */
    private boolean mTorchOn = false;
    /** Shows the ocr result to user. */
    private TextView mOcrResult;
    /** Holds last scanned image for processing with direct API*/
    private Image mLastScannedImage = null;
    /** Recognizer instance. */
    private Recognizer mDirectApiRecognizer;
    /** CameraPermissionManager is provided helper class that can be used to obtain the permission to use camera.
     * It is used on Android 6.0 (API level 23) or newer.
     */
    private CameraPermissionManager mCameraPermissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combination_scan);

        // set log level to information because ocr results will be passed to Log (information level)
        Log.setLogLevel(com.microblink.util.Log.LogLevel.LOG_INFORMATION);

        // obtain references to needed member variables
        mRecognizerView = (RecognizerView) findViewById(R.id.rec_view);
        mFlashButton = (ImageButton) findViewById(R.id.btnFlash);
        mOcrResult = (TextView) findViewById(R.id.txtResult);

        // camera events listener is required as it will receive camera-related events
        // such as startup errors, autofocus callbacks etc.
        mRecognizerView.setCameraEventsListener(this);
        // scan result listener is requires as it will receive recognition results
        mRecognizerView.setScanResultListener(this);
        // we want camera to use whole available view space by cropping the camera preview
        // instead of letterboxing it
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);
        // optimize camera lens for near object scanning
        mRecognizerView.setOptimizeCameraForNearScan(true);
        // use 720p resolution instead of default 1080p to make everything work faster
        mRecognizerView.setVideoResolutionPreset(VideoResolutionPreset.VIDEO_RESOLUTION_720p);

        // get the recognizer instance
        try {
            mDirectApiRecognizer = Recognizer.getSingletonInstance();
        } catch (FeatureNotSupportedException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recognizer not supported!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // In order for scanning to work, you must enter a valid licence key. Without licence key,
        // scanning will not work. Licence key is bound the the package name of your app, so when
        // obtaining your licence key from Microblink make sure you give us the correct package name
        // of your app. You can obtain your licence key at http://microblink.com/login or contact us
        // at http://help.microblink.com.
        // Licence key also defines which recognizers are enabled and which are not. Since the licence
        // key validation is performed on image processing thread in native code, all enabled recognizers
        // that are disallowed by licence key will be turned off without any error and information
        // about turning them off will be logged to ADB logcat.
        try {
            mRecognizerView.setLicenseKey(LICENSE_KEY);
            mDirectApiRecognizer.setLicenseKey(this, LICENSE_KEY);
        } catch (InvalidLicenceKeyException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid license key!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // create BlinkInput recognizer settings object and add parser to it
        BlinkInputRecognizerSettings ocrSett = new BlinkInputRecognizerSettings();
        RawParserSettings rawSett = new RawParserSettings();

        // add raw parser with name "Raw" to default parser group
        // parser name is important for obtaining results later
        ocrSett.addParser("Raw", rawSett);

        // prepare recognition settings
        RecognitionSettings recognitionSettings = new RecognitionSettings();
        // set recognizer settings array that is used to configure recognition
        // BlinkInputRecognizer will be used in the recognition process
        recognitionSettings.setRecognizerSettingsArray(new RecognizerSettings[]{ocrSett});
        mRecognizerView.setRecognitionSettings(recognitionSettings);

        // define the scanning region of the image that will be scanned.
        // You must ensure that scanning region defined here is the same as in the layout
        // The coordinates for scanning region are relative to recognizer view.
        // the following means: rectangle starts at 5% of recognizer view's width and
        // 17% of its height. Rectangle width is 90% of recognizer view's width and
        // 47% of its height.
        // If you do not set this, OCR will be performed on full camera frame and this
        // will result in very poor performance.
        mRecognizerView.setScanningRegion(new Rectangle(0.05f, 0.17f, 0.9f, 0.47f), false);

        // define which metadata will be available in MetadataListener (onMetadataAvailable method)
        MetadataSettings metadataSett = new MetadataSettings();

        // prepare image metadata settings - define which images should be available in MetadataListener.
        MetadataSettings.ImageMetadataSettings ims = new MetadataSettings.ImageMetadataSettings();
        // dewarped image should be available in MetadataListener for processing with direct API
        ims.setDewarpedImageEnabled(true);

        metadataSett.setImageMetadataSettings(ims);

        // set image listener that will obtain image that was used for OCR
        mRecognizerView.setMetadataListener(this, metadataSett);

        // instantiate the camera permission manager
        mCameraPermissionManager = new CameraPermissionManager(this);
        // get the built-in overlay that should be displayed when camera permission is not given
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            // add it to the current layout that contains the recognizer view
            ViewGroup root = (ViewGroup) findViewById(R.id.combination_root);
            root.addView(v);
        }

        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.create();

        // initialize DirectAPI with same settings
        mDirectApiRecognizer.initialize(this, recognitionSettings, new DirectApiErrorListener() {
            @Override
            public void onRecognizerError(Throwable t) {
                onError(t);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerView != null) {
            mRecognizerView.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.pause();
        }
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.destroy();
        }
        if (mDirectApiRecognizer != null) {
            mDirectApiRecognizer.terminate();
            mDirectApiRecognizer = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.changeConfiguration(newConfig);
        }
    }

    @Override
    public void onCameraPreviewStarted() {
        // this method is called when camera preview has started
        // camera is being initialized in background thread and when
        // it is ready, this method is called.
        // You can use it to check camera properties, such as whether
        // torch is supported and then show/hide torch button.
        if (mRecognizerView != null) {
            if (mRecognizerView.isCameraTorchSupported()) {
                mFlashButton.setVisibility(View.VISIBLE);
            }
            // after camera is started, we can set the metering area for autofocus, white balance
            // and auto exposure measurements
            // we set the same rectangle as for scanning region
            // we also define that this metering area will not follow device orientation changes because
            // we have set non rotatable scanning region
            mRecognizerView.setMeteringAreas(new RectF[]{new RectF(0.05f, 0.17f, 0.05f + 0.9f, 0.17f + 0.47f)}, false);
        }
    }

    public void onBtnExitClicked(View v) {
        finish();
    }

    public void onBtnFlashClicked(View v) {
        mRecognizerView.setTorchState(!mTorchOn, new SuccessCallback() {
            @Override
            public void onOperationDone(boolean success) {
                if (success) {
                    mTorchOn = !mTorchOn;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mTorchOn) {
                                mFlashButton.setImageResource(R.drawable.flashlight_inverse_blink_ocr);
                            } else {
                                mFlashButton.setImageResource(R.drawable.flashlight_blink_ocr);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCameraPreviewStopped() {
        // this method is called when camera preview has stopped
        // camera is being terminated in background thread and when
        // it finishes grabbing frames, this method is called
    }

    @Override
    public void onError(Throwable throwable) {
        // this method is called when error happens whilst loading RecognizerView or during recognition
        // this can be either because camera is busy and cannot be opened
        // or native library could not be loaded because of unsupported processor architecture
        // or because some feature is not supported
        Log.e("ScanActivity", "On error!");
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setCancelable(false)
                .setTitle("Error")
                .setMessage(throwable.getMessage())
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) dialog.dismiss();
                        finish();
                    }
                }).create().show();
    }

    @Override
    @TargetApi(23)
    public void onCameraPermissionDenied() {
        // this method is called on Android 6.0 and newer if camera permission was not given
        // by user

        // ask user to give a camera permission. Provided manager asks for
        // permission only if it has not been already granted.
        // on API level < 23, this method does nothing
        mCameraPermissionManager.askForCameraPermission();
    }

    @Override
    public void onAutofocusFailed() {
        // This method will be called when camera focusing has failed.
        // Camera manager usually tries different focusing strategies and this method is called when all
        // those strategies fail to indicate that either object on which camera is being focused is too
        // close or ambient light conditions are poor.
    }

    @Override
    public void onAutofocusStarted(Rect[] rects) {
        // This method will be called when camera focusing has started.
        // You can utilize this method to draw focusing animation on UI.
        // Areas parameter is array of rectangles where focus is being measured.
        // It can be null on devices that do not support fine-grained camera control.
    }

    @Override
    public void onAutofocusStopped(Rect[] rects) {
        // This method will be called when camera focusing has stopped.
        // You can utilize this method to remove focusing animation on UI.
        // Areas parameter is array of rectangles where focus is being measured.
        // It can be null on devices that do not support fine-grained camera control.
    }

    @Override
    public void onScanningDone(RecognitionResults results) {
        BaseRecognitionResult[] dataArray = results.getRecognitionResults();
        // we have enabled only one recognizer, so we expect only one element in dataArray
        if (dataArray != null && dataArray.length == 1) {
            if (dataArray[0] instanceof BlinkInputRecognitionResult) {
                BlinkInputRecognitionResult result = (BlinkInputRecognitionResult) dataArray[0];

                String scanned = result.getParsedResult("Raw");

                Log.i("SCAN RESULT", scanned);

                if(scanned != null && !scanned.isEmpty()) {
                    OcrResult ocrResult = result.getOcrResult();

                    // method 'findString' is not optimized for speed
                    OcrResultIterator iter = findString("Carat Weight", ocrResult);
                    if (iter != null) { // we found "Carat Weight"
                        // first display the result in text (from found "Carat Weight" position onwards)
                        mOcrResult.setText("Horizontal text: " + iteratorToString(iter));

                        if(mLastScannedImage != null) {
                            CharWithVariants ch = iter.getCurrentCharWithVariants();

                            // now determine position of image where vertical text is expected
                            // let's say it is the whole vertical strip of image ending with
                            // first letter of string "Carat Weight"

                            float charX = ch.getChar().getPosition().getX();

                            // determine region that will be scanned for vertical text as vertical
                            // strip left of string "Carat Weight"

                            Rect roi = new Rect(0, 0, (int) charX, mLastScannedImage.getHeight());

                            // set the calculated ROI to image that we will scan
                            mLastScannedImage.setROI(roi);
                            // rotate image's orientation by 90 degrees clockwise (this is fast operation, just rotation info
                            // is updated, no pixels are moved)
                            mLastScannedImage.setImageOrientation(mLastScannedImage.getImageOrientation().rotate90Clockwise());

                            // check if recognizer is still active (required if onScanningDone was called after activity was destroyed)
                            if(mDirectApiRecognizer != null) {
                                // pause scanning to prevent arrival of new results while DirectAPI processes image
                                mRecognizerView.pauseScanning();
                                // finally, perform recognition of image
                                mDirectApiRecognizer.recognizeImage(mLastScannedImage, new ScanResultListener() {
                                    @Override
                                    public void onScanningDone(RecognitionResults results) {
                                        BaseRecognitionResult[] baseRecognitionResults = results.getRecognitionResults();
                                        if (baseRecognitionResults != null && baseRecognitionResults.length == 1) {
                                            if (baseRecognitionResults[0] instanceof BlinkInputRecognitionResult) {
                                                BlinkInputRecognitionResult result = (BlinkInputRecognitionResult) baseRecognitionResults[0];
                                                final String verticalText = result.getParsedResult("Raw");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mOcrResult.setText("Vertical text: " + verticalText + "\n\n" + mOcrResult.getText());
                                                    }
                                                });
                                            }
                                        }
                                        // resume video scanning loop and reset internal state
                                        mRecognizerView.resumeScanning(true);
                                    }
                                });
                            }
                            // if bitmap cannot be recognized, this means stop() and destroy() have been called, so resuming scanning makes no sense
                        }
                    }
                }
            }
        }
        // unless paused, scanning will be automatically resumed without internal state reset
    }

    /**
     * Converts OcrResult from iterator's position to string
     */
    private String iteratorToString(OcrResultIterator i) {
        StringBuilder sb = new StringBuilder();
        OcrResultIterator iter = new OcrResultIterator(i);
        while(iter.hasNext()) {
            sb.append(iter.getCurrentCharWithVariants().getChar().getValue());
            boolean newLine = iter.moveToNext();
            if(newLine) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * very inefficient implementation of string search
     */
    private OcrResultIterator findString(String str, OcrResult result) {
        OcrResultIterator iter = new OcrResultIterator(result);
        while(iter.hasNext()) {
            if(stringMatch(str, iter)) return iter;
            iter.moveToNext();
        }
        return null;
    }

    /**
     * Returns true if string matches OcrResult from position pointed by iterator.
     */
    private boolean stringMatch(String str, OcrResultIterator iter) {
        OcrResultIterator iterCopy = new OcrResultIterator(iter);
        for (int pos = 0; pos < str.length(); ++pos) {
            if(!charMatch(str.charAt(pos), iterCopy.getCurrentCharWithVariants())) return false;
            if(pos != str.length() - 1 && !iterCopy.hasNext()) return false;
            iterCopy.moveToNext();
        }
        return true;
    }

    /**
     * Returns true if char matches given char or any of its recognition alternatives
     */
    private boolean charMatch(char c, CharWithVariants ocrCharWithVariants) {
        if(c == ocrCharWithVariants.getChar().getValue()) return true;
        // check alternatives
        OcrChar[] variants = ocrCharWithVariants.getRecognitionVariants();
        if (variants != null) { // some chars do not have alternatives
            for (OcrChar var : variants) {
                if (c == var.getValue()) return true;
            }
        }
        return false;
    }

    @Override
    public void onMetadataAvailable(Metadata metadata) {
        if (metadata instanceof ImageMetadata) {
            mLastScannedImage = ((ImageMetadata) metadata).getImage().clone();
        }
    }


    /**
     * Simple OcrResultIterator implementation. Not optimized for speed.
     */
    private class OcrResultIterator {
        private int mBlock = 0;
        private int mLine = 0;
        private int mChar = 0;
        private OcrResult mOcrResult = null;

        public OcrResultIterator(OcrResult result) {
            mOcrResult = result;
        }

        /**
         * copy constructor
         */
        public OcrResultIterator(OcrResultIterator other) {
            mOcrResult = other.mOcrResult;
            mBlock = other.mBlock;
            mChar = other.mChar;
            mLine = other.mLine;
        }

        public CharWithVariants getCurrentCharWithVariants() {
            return mOcrResult.getBlocks()[mBlock].getLines()[mLine].getChars()[mChar];
        }

        boolean hasNext() {
            return (mBlock < mOcrResult.getBlocks().length - 1) ||
                    (mLine < mOcrResult.getBlocks()[mBlock].getLines().length - 1) ||
                    (mChar < mOcrResult.getBlocks()[mBlock].getLines()[mLine].getChars().length - 1);
        }

        /** moves to next char and returns true if new line is crossed */
        public boolean moveToNext() {
            boolean newLine = false;
            mChar++;
            if (mChar == mOcrResult.getBlocks()[mBlock].getLines()[mLine].getChars().length) {
                mChar = 0;
                mLine++;
                newLine = true;
                if (mLine == mOcrResult.getBlocks()[mBlock].getLines().length) {
                    mLine = 0;
                    mBlock++;
                }
            }
            return newLine;
        }
    }
}

