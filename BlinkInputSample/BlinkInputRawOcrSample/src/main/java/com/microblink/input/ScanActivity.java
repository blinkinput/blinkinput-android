package com.microblink.input;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microblink.entities.ocrengine.AbstractOCREngineOptions;
import com.microblink.entities.ocrengine.deep.DeepOCREngineOptions;
import com.microblink.entities.ocrengine.legacy.BlinkOCREngineOptions;
import com.microblink.entities.parsers.raw.RawParser;
import com.microblink.entities.processors.parserGroup.ParserGroupProcessor;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkinput.BlinkInputRecognizer;
import com.microblink.metadata.MetadataCallbacks;
import com.microblink.metadata.detection.points.DisplayablePointsDetection;
import com.microblink.metadata.detection.points.PointsDetectionCallback;
import com.microblink.recognition.RecognitionSuccessType;
import com.microblink.util.CameraPermissionManager;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.ocrResult.OcrResultDotsView;
import com.microblink.view.recognition.RecognizerRunnerView;
import com.microblink.view.recognition.ScanResultListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScanActivity extends Activity {

    private static final String EXTRA_OCR_ENGINE_TYPE = "OCR_ENGINE_TYPE";

    private RecognizerRunnerView mRecognizerRunnerView;
    private RawParser mRawParser;
    private TextView mResultTextView;
    private OcrResultDotsView mDotsView;

    private CameraPermissionManager mCameraPermissionManager;

    public static Intent buildBlinkOcrIntent(Context context) {
        Intent intent = new Intent(context, ScanActivity.class);
        intent.putExtra(EXTRA_OCR_ENGINE_TYPE, OcrEngineType.Blink);
        return intent;
    }

    public static Intent buildDeepOcrIntent(Context context) {
        Intent intent = new Intent(context, ScanActivity.class);
        intent.putExtra(EXTRA_OCR_ENGINE_TYPE, OcrEngineType.DeepOcr);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_ocr_scan);
        mRecognizerRunnerView = findViewById(R.id.recognizerView);
        mResultTextView = findViewById(R.id.result_view);

        BlinkInputRecognizer blinkInputRecognizer = setupRecognizer(getIntent());
        RecognizerBundle recognizerBundle = new RecognizerBundle(blinkInputRecognizer);
        mRecognizerRunnerView.setRecognizerBundle(recognizerBundle);

        mDotsView = setupDotsView();

        MetadataCallbacks metadataCallbacks = new MetadataCallbacks();
        metadataCallbacks.setPointsDetectionCallback(new PointsDetectionCallback() {
            @Override
            public void onPointsDetection(@NonNull DisplayablePointsDetection displayablePointsDetection) {
                mDotsView.addDisplayablePointsDetection(displayablePointsDetection);
            }
        });
        mRecognizerRunnerView.setMetadataCallbacks(metadataCallbacks);
        mRecognizerRunnerView.setScanResultListener(mScanResultListener);
        mRecognizerRunnerView.setCameraEventsListener(mCameraEventsListener);

        mCameraPermissionManager = setupCameraPermissionManager();

        mRecognizerRunnerView.create();
    }

    private BlinkInputRecognizer setupRecognizer(Intent intent) {
        OcrEngineType ocrEngineType;
        if (intent.getExtras() == null) {
            ocrEngineType = OcrEngineType.Blink;
        } else {
            ocrEngineType = (OcrEngineType) intent.getSerializableExtra(EXTRA_OCR_ENGINE_TYPE);
        }

        AbstractOCREngineOptions engineOptions = createOcrEngineOptions(ocrEngineType);
        mRawParser = new RawParser();
        mRawParser.setOcrEngineOptions(engineOptions);
        return new BlinkInputRecognizer(new ParserGroupProcessor(mRawParser));
    }

    private OcrResultDotsView setupDotsView() {
        OcrResultDotsView ocrResultDotsView = new OcrResultDotsView(this,
                mRecognizerRunnerView.getHostScreenOrientation(),
                mRecognizerRunnerView.getInitialOrientation());
        mRecognizerRunnerView.addChildView(ocrResultDotsView.getView(), false);
        return ocrResultDotsView;
    }

    private CameraPermissionManager setupCameraPermissionManager() {
        CameraPermissionManager cameraPermissionManager = new CameraPermissionManager(this);
        View cameraPermissionOverlay = cameraPermissionManager.getAskPermissionOverlay();
        if (cameraPermissionOverlay != null) {
            ViewGroup root = findViewById(R.id.root);
            root.addView(cameraPermissionOverlay);
        }
        return cameraPermissionManager;
    }

    private AbstractOCREngineOptions createOcrEngineOptions(OcrEngineType ocrEngineType) {
        switch (ocrEngineType) {
            case Blink:
                return new BlinkOCREngineOptions();
            case DeepOcr:
                return new DeepOCREngineOptions();
            default:
                return new BlinkOCREngineOptions();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRecognizerRunnerView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecognizerRunnerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecognizerRunnerView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRecognizerRunnerView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognizerRunnerView.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mRecognizerRunnerView == null) {
            return;
        }

        mRecognizerRunnerView.changeConfiguration(newConfig);
        if (mDotsView != null) {
            mDotsView.setHostActivityOrientation(mRecognizerRunnerView.getHostScreenOrientation());
        }
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private ScanResultListener mScanResultListener = new ScanResultListener() {
        @Override
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            if (recognitionSuccessType == RecognitionSuccessType.UNSUCCESSFUL) {
                return;
            }

            final String resultString = mRawParser.getResult().getRawText();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResultTextView.setText(resultString);
                }
            });
        }
    };

    private CameraEventsListener mCameraEventsListener = new CameraEventsListener() {
        @Override
        public void onCameraPermissionDenied() {
            mCameraPermissionManager.askForCameraPermission();
        }

        @Override
        public void onCameraPreviewStarted() {
        }

        @Override
        public void onCameraPreviewStopped() {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onAutofocusFailed() {
        }

        @Override
        public void onAutofocusStarted(Rect[] rects) {
        }

        @Override
        public void onAutofocusStopped(Rect[] rects) {
        }
    };

    enum OcrEngineType {
        Blink,
        DeepOcr
    }

}
