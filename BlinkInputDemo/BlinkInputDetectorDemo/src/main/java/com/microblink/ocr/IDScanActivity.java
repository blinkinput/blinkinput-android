package com.microblink.ocr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.microblink.detectors.DetectorResult;
import com.microblink.detectors.quad.QuadDetectorResult;
import com.microblink.hardware.SuccessCallback;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.metadata.DetectionMetadata;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.metadata.OcrMetadata;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.IResultHolder;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.results.date.DateResult;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.OnSizeChangedListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.ocrResult.IOcrResultView;
import com.microblink.view.ocrResult.OcrResultCharsView;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.quadview.QuadViewManager;
import com.microblink.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.view.viewfinder.quadview.QuadViewPreset;

public class IDScanActivity extends Activity implements CameraEventsListener, ScanResultListener, MetadataListener, OnSizeChangedListener {

    public static final String EXTRAS_RECOGNITION_SETTINGS = "EXTRAS_RECOGNITION_SETTINGS";

    private RecognizerView mRecognizerView;
    private QuadViewManager mQuadViewManager;
    private IOcrResultView mOcrResultView;
    private CameraPermissionManager mCameraPermissionManager;
    private ImageButton mBackButton;
    private ImageButton mTorchButton;
    private boolean mTorchEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idscan);

        mRecognizerView = (RecognizerView)findViewById(R.id.recognizerView);

        try {
            mRecognizerView.setLicenseKey(MenuActivity.LICENSE_KEY);
        } catch (InvalidLicenceKeyException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid licence key!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mRecognizerView.setCameraEventsListener(this);
        mRecognizerView.setScanResultListener(this);
        mRecognizerView.setOnSizeChangedListener(this);
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });

        RecognitionSettings settings = getIntent().getParcelableExtra(EXTRAS_RECOGNITION_SETTINGS);
        mRecognizerView.setRecognitionSettings(settings);

        MetadataSettings ms = new MetadataSettings();
        ms.setDetectionMetadataAllowed(true);
        ms.setOcrMetadataAllowed(true);

        mRecognizerView.setMetadataListener(this, ms);

        mCameraPermissionManager = new CameraPermissionManager(this);
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            ViewGroup vg = (ViewGroup) findViewById(R.id.idscan_screen_root);
            vg.addView(v);
        }

        mRecognizerView.create();

        View overlay = getLayoutInflater().inflate(R.layout.detector_camera_overlay, null);
        mRecognizerView.addChildView(overlay, true);

        mBackButton = (ImageButton) overlay.findViewById(R.id.btnCancel);
        mTorchButton = (ImageButton) overlay.findViewById(R.id.btnFlash);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mQuadViewManager = QuadViewManagerFactory.createQuadViewFromPreset(mRecognizerView, QuadViewPreset.DEFAULT_CORNERS_FROM_SCAN_ACTIVITY);
        mOcrResultView = new OcrResultCharsView(this, null, mRecognizerView.getHostScreenOrientation());
        mRecognizerView.addChildView(mOcrResultView.getView(), false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRecognizerView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecognizerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecognizerView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRecognizerView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognizerView.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mRecognizerView.changeConfiguration(newConfig);
        mQuadViewManager.configurationChanged(mRecognizerView, newConfig);
        mOcrResultView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
    }

    private void enableTorchButtonIfPossible() {
        if (mRecognizerView.isCameraTorchSupported() && mTorchButton != null) {
            mTorchButton.setVisibility(View.VISIBLE);
            mTorchButton.setImageResource(R.drawable.flashlight_blink_ocr);
            mTorchEnabled = false;
            mTorchButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mRecognizerView.setTorchState(!mTorchEnabled, new SuccessCallback() {
                        @Override
                        public void onOperationDone(final boolean success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(IDScanActivity.this, "Setting torch to {}. Success: {}", !mTorchEnabled, success);
                                    if (success) {
                                        mTorchEnabled = !mTorchEnabled;
                                        if (mTorchEnabled) {
                                            mTorchButton.setImageResource(R.drawable.flashlight_inverse_blink_ocr);
                                        } else {
                                            mTorchButton.setImageResource(R.drawable.flashlight_blink_ocr);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onCameraPreviewStarted() {
        enableTorchButtonIfPossible();
    }

    @Override
    public void onCameraPreviewStopped() {

    }

    @Override
    public void onError(Throwable exc) {
        Log.e(this, exc, "Error on initialisation");
        Toast.makeText(this, "Error on init. See ADB log for details!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onCameraPermissionDenied() {
        mCameraPermissionManager.askForCameraPermission();
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAutofocusFailed() {

    }

    @Override
    public void onAutofocusStarted(Rect[] focusAreas) {

    }

    @Override
    public void onAutofocusStopped(Rect[] focusAreas) {

    }

    @Override
    public void onScanningDone(@Nullable RecognitionResults results) {
        if (results != null) {
            BaseRecognitionResult[] brrs = results.getRecognitionResults();
            if (brrs != null && brrs.length > 0) {

                BaseRecognitionResult result = brrs[0];

                // display dialog with data from first element of array
                mRecognizerView.pauseScanning();
                StringBuilder sb = new StringBuilder();

                IResultHolder resultHolder = result.getResultHolder();
                for (String key : resultHolder.keySet()) {
                    Object value = resultHolder.getObject(key);
                    String stringValue = null;
                    if (value instanceof String) {
                        stringValue = (String) value;
                    } else if (value instanceof DateResult) {
                        stringValue = ((DateResult) value).getOriginalDateString();
                    }
                    if (stringValue != null && !stringValue.isEmpty()) {
                        sb.append(key).append(": ").append(stringValue).append('\n');
                    }
                }

                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setTitle("Scan results").setMessage(sb.toString()).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mOcrResultView.clearOcrResults();
                        mRecognizerView.resumeScanning(true);
                    }
                }).setCancelable(false).create().show();
            }
        }
    }

    @Override
    public void onMetadataAvailable(Metadata metadata) {
        if (metadata instanceof DetectionMetadata) {
            DetectorResult dr = ((DetectionMetadata) metadata).getDetectionResult();
            if (dr == null) {
                mQuadViewManager.animateQuadToDefaultPosition();
                mOcrResultView.clearOcrResults();
            } else if (dr instanceof QuadDetectorResult) {
                mQuadViewManager.animateQuadToDetectionPosition((QuadDetectorResult) dr);
            }
        } else if (metadata instanceof OcrMetadata) {
            mOcrResultView.addOcrResult(((OcrMetadata) metadata).getOcrResult());
        }
    }

    @Override
    public void onSizeChanged(int width, int height) {
        Log.d(this, "[onSizeChanged] Width:{}, Height:{}", width, height);
        int horizontalMargin = (int) (width * 0.07);
        int verticalMargin = (int) (height * 0.07);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int tmp = horizontalMargin;
            horizontalMargin = verticalMargin;
            verticalMargin = tmp;
        }

        if (mBackButton != null) {
            // set margins for back button
            FrameLayout.LayoutParams backButtonParams = (FrameLayout.LayoutParams) mBackButton.getLayoutParams();
            if (backButtonParams.leftMargin != horizontalMargin || backButtonParams.topMargin != verticalMargin) {
                backButtonParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
                mBackButton.setLayoutParams(backButtonParams);
            }
        }

        if (mTorchButton != null) {
            // set margins for torch button
            FrameLayout.LayoutParams torchButtonParams = (FrameLayout.LayoutParams) mTorchButton.getLayoutParams();
            if (torchButtonParams.leftMargin != horizontalMargin || torchButtonParams.topMargin != verticalMargin) {
                torchButtonParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
                mTorchButton.setLayoutParams(torchButtonParams);
            }
        }
    }
}
