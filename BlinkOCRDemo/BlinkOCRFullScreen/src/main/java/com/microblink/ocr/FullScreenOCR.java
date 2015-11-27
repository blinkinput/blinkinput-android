package com.microblink.ocr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.microblink.hardware.orientation.Orientation;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.metadata.OcrMetadata;
import com.microblink.metadata.detection.PointsDetectionMetadata;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkbarcode.bardecoder.BarDecoderRecognizerSettings;
import com.microblink.recognizers.blinkbarcode.bardecoder.BarDecoderScanResult;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognitionResult;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognizerSettings;
import com.microblink.recognizers.blinkocr.parser.generic.RawParserSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.results.ocr.OcrResult;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.view.BaseCameraView;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.ocrResult.OcrResultCharsView;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.PointSetView;

public class FullScreenOCR extends Activity implements MetadataListener, CameraEventsListener, ScanResultListener {

    /** RecognizerView is the view that controls camera and recognition */
    private RecognizerView mRecognizerView;
    /**  OcrResultCharsView is builtin view that can display OCR result on top of camera */
    private OcrResultCharsView mOcrResultView;
    /**  PoinSetView is builtin view that can display points of interest on top of camera */
    private PointSetView mPointSetView;
    /** CameraPermissionManager is provided helper class that can be used to obtain the permission to use camera.
     * It is used on Android 6.0 (API level 23) or newer.
     */
    private CameraPermissionManager mCameraPermissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_ocr);
        // obtain reference to RecognizerView
        mRecognizerView = (RecognizerView) findViewById(R.id.recognizerView);

        // initialize BlinkOCR recognizer with only raw parser
        BlinkOCRRecognizerSettings ocrSett = new BlinkOCRRecognizerSettings();
        RawParserSettings rawSett = new RawParserSettings();
        // add raw parser with name "Raw" to default parser group
        // parser name is important for obtaining results later
        ocrSett.addParser("Raw", rawSett);

        // initialize 1D barcode recognizer and set it to scan Code39 and Code128 barcodes
        BarDecoderRecognizerSettings barSett = new BarDecoderRecognizerSettings();
        barSett.setScanCode128(true);
        barSett.setScanCode39(true);

        // prepare the recognition settings
        RecognitionSettings recognitionSettings = new RecognitionSettings();
        // BlinkOCRRecognizer and BarDecoderRecognizer will be used in the recognition process
        recognitionSettings.setRecognizerSettingsArray(new RecognizerSettings[]{ocrSett, barSett});

        mRecognizerView.setRecognitionSettings(recognitionSettings);

        // we want each frame to be scanned for both OCR and barcodes so we must
        // allow multiple scan results on single image.
        // If this is not allowed (default), the first recognizer that finds its object
        // of interest stops the recognition chain (for example in that case if barcode is found
        // OCR will not be performed - we do not want this, so we allow multiple scan results
        // on single image).
        recognitionSettings.setAllowMultipleScanResultsOnSingleImage(true);

        // set the license key
        try {
            mRecognizerView.setLicenseKey("CNDHGUQS-3REAUYG3-OJYH4FCG-QNW7QSOK-DEO5SIWW-MKYTEYZT-UGBW36CJ-YIELTPLQ");
        } catch (InvalidLicenceKeyException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid licence key", Toast.LENGTH_SHORT).show();
            finish();
            mRecognizerView = null;
            return;
        }

        // use all available view area for displaying camera, possibly cropping the camera frame
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);

        // configure metadata settings and chose detection metadata
        // that will be passed to metadata listener
        MetadataSettings mdSett = new MetadataSettings();
        // set OCR metadata to be available in metadata listener
        mdSett.setOcrMetadataAllowed(true);
        // enable detection metadata for obtaining points of interest
        mdSett.setDetectionMetadataAllowed(true);
        // metadata listener receives detection metadata during recognition process
        mRecognizerView.setMetadataListener(this, mdSett);
        // camera events listener receives camera events, like when camera preview has started, stopped
        // or if camera error happened
        mRecognizerView.setCameraEventsListener(this);
        // scan result listener receives scan result once it becomes available
        mRecognizerView.setScanResultListener(this);

        // orientation allowed listener is asked whether given orientation
        // is allowed in UI. We keep activity always in portrait, but allow
        // scanning in all orientations.
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });

        // instantiate the camera permission manager
        mCameraPermissionManager = new CameraPermissionManager(this);
        // get the built in layout that should be displayed when camera permission is not given
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            // add it to the current layout that contains the recognizer view
            ViewGroup vg = (ViewGroup) findViewById(R.id.full_screen_root);
            vg.addView(v);
        }

        // all activity lifecycle events must be passed on to RecognizerView
        mRecognizerView.create();

        // create OCR result view
        mOcrResultView = new OcrResultCharsView(this, null, mRecognizerView.getHostScreenOrientation());

        // OCR result view will be added as child of recognizer view. This makes sure that if
        // recognizer view letter-boxes the camera preview (ASPECT_FIT camera mode), the OCR
        // result view will be layed out exactly above camera preview
        // Note that we can add child views to RecognizerView only after we called create on it.
        // The boolean parameter defines whether added view will be rotated with device. Allowed
        // orientations are defined with OrientationAllowedListener.
        mRecognizerView.addChildView(mOcrResultView, false);

        // we do the same with PointSetView
        mPointSetView = new PointSetView(this, null);
        mRecognizerView.addChildView(mPointSetView, false);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.start();
        }
        mCameraPermissionManager.askForCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            if (mCameraPermissionManager.hasCameraPermission()) {
                mRecognizerView.resume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null && mRecognizerView.getCameraViewState() == BaseCameraView.CameraViewState.RESUMED) {
            mRecognizerView.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.destroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.changeConfiguration(newConfig);
            mOcrResultView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
        }
    }

    @Override
    public void onScanningDone(RecognitionResults results) {
        // called when scanning completes. In this example, we first check if dataArray contains
        // barcode result and display a barcode contents in the Toast.
        // We also check if dataArray contains raw parser result and log it to ADB.
        BaseRecognitionResult[] dataArray = results.getRecognitionResults();
        for (BaseRecognitionResult r : dataArray) {
            if (r instanceof BarDecoderScanResult) { // r is barcode scan result
                BarDecoderScanResult bdsr = (BarDecoderScanResult) r;

                // create toast with contents: Barcode type: barcode contents
                StringBuilder sb = new StringBuilder();
                sb.append(bdsr.getBarcodeType().name());
                sb.append(": ");
                sb.append(bdsr.getStringData());

                Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
            } else if (r instanceof BlinkOCRRecognitionResult) {
                BlinkOCRRecognitionResult bocrRes = (BlinkOCRRecognitionResult) r;

                // obtain parse result from the parser named "Raw"
                String rawParsed = bocrRes.getParsedResult("Raw");
                Log.i("Parsed", rawParsed);

                // obtain OCR result that was used for parsing
                OcrResult ocrResult = bocrRes.getOcrResult();
                Log.i("OcrResult", ocrResult.toString());
            }
        }

        // Finally, we resume scanning and reset internal state. If you want OCR to reuse
        // results from previous scan to make current scan of better quality, call
        // resumeScanning(false). Note that preserving state preserves state of all
        // recognizers, including barcode recognizers (if enabled).
        mRecognizerView.resumeScanning(true);
    }

    @Override
    public void onCameraPreviewStarted() {
        // called immediately after camera preview has been started. This is useful
        // if you display splash screen in your app while loading camera. In this method
        // you should then remove the splash screen.
    }

    @Override
    public void onCameraPreviewStopped() {
        // called immediately after camera preview has been stopped. This is useful
        // if you want to release some resources that are required only while camera preview
        // is active.
    }

    @Override
    public void onError(Throwable ex) {
        // This method will be called when opening of camera resulted in exception or
        // recognition process encountered an error.
        // The error details will be given in exc parameter.
        Log.e(this, ex, "Error");
        Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        finish();
    }



    @Override
    public void onMetadataAvailable(Metadata metadata) {
        // This method will be called when metadata becomes available during recognition process.
        // Here, for every metadata type that is allowed through metadata settings,
        // desired actions can be performed.
        if (metadata instanceof OcrMetadata) {
            // get the ocr result and show it inside ocr result view
            mOcrResultView.setOcrResult(((OcrMetadata) metadata).getOcrResult());
        } else if (metadata instanceof PointsDetectionMetadata) {
            // show the points of interest inside point set view
            mPointSetView.setPointSet(((PointsDetectionMetadata) metadata).getPoints());
        }
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
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // on API level 23, request permission result should be passed to camera permission manager
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
