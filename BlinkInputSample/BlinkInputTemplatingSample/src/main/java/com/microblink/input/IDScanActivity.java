package com.microblink.input;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.microblink.blinkinput.entities.recognizers.RecognizerBundle;
import com.microblink.blinkinput.hardware.SuccessCallback;
import com.microblink.blinkinput.hardware.orientation.Orientation;
import com.microblink.blinkinput.metadata.MetadataCallbacks;
import com.microblink.blinkinput.metadata.detection.FailedDetectionCallback;
import com.microblink.blinkinput.metadata.detection.points.DisplayablePointsDetection;
import com.microblink.blinkinput.metadata.detection.points.PointsDetectionCallback;
import com.microblink.blinkinput.metadata.detection.points.PointsType;
import com.microblink.blinkinput.metadata.detection.quad.DisplayableQuadDetection;
import com.microblink.blinkinput.metadata.detection.quad.QuadDetectionCallback;
import com.microblink.blinkinput.recognition.RecognitionSuccessType;
import com.microblink.blinkinput.util.CameraPermissionManager;
import com.microblink.blinkinput.util.Log;
import com.microblink.blinkinput.view.CameraEventsListener;
import com.microblink.blinkinput.view.OnSizeChangedListener;
import com.microblink.blinkinput.view.OrientationAllowedListener;
import com.microblink.blinkinput.view.exception.NonLandscapeOrientationNotSupportedException;
import com.microblink.blinkinput.view.ocrResult.OcrResultDotsView;
import com.microblink.blinkinput.view.recognition.RecognizerRunnerView;
import com.microblink.blinkinput.view.recognition.ScanResultListener;
import com.microblink.blinkinput.view.viewfinder.points.IDisplayablePointsView;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewManager;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewPreset;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public class IDScanActivity extends Activity {

    private RecognizerRunnerView mRecognizerRunnerView;
    private RecognizerBundle mRecognizerBundle = new RecognizerBundle();
    private QuadViewManager mQuadViewManager;
    private IDisplayablePointsView mOcrResultView;
    private CameraPermissionManager mCameraPermissionManager;
    private ImageButton mBackButton;
    private ImageButton mTorchButton;
    private boolean mTorchEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_idscan);
        } catch (InflateException ie) {
            Throwable cause = ie.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof NonLandscapeOrientationNotSupportedException) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                }
                return;
            } else {
                throw ie;
            }
        }

        Intent intent = getIntent();
        // since mRecognizerBundle does not contain any recognizers, loadFromIntent will create
        // new recognizers from intent data and automatically bundle them inside mRecognizerBundle
        mRecognizerBundle.loadFromIntent(intent);

        mRecognizerRunnerView = findViewById(R.id.recognizerRunnerView);

        mRecognizerRunnerView.setCameraEventsListener(mCameraEventsListener);
        mRecognizerRunnerView.setScanResultListener(mScanResultListener);
        mRecognizerRunnerView.setOnSizeChangedListener(mOnSizeChangedListener);
        mRecognizerRunnerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });

        mRecognizerRunnerView.setRecognizerBundle(mRecognizerBundle);

        mCameraPermissionManager = new CameraPermissionManager(this);
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            ViewGroup vg = findViewById(R.id.idscan_screen_root);
            vg.addView(v);
        }

        mRecognizerRunnerView.create();

        View overlay = getLayoutInflater().inflate(R.layout.idscan_camera_overlay, mRecognizerRunnerView, false);
        mRecognizerRunnerView.addChildView(overlay, true);

        mBackButton =  overlay.findViewById(R.id.btnCancel);
        mTorchButton = overlay.findViewById(R.id.btnFlash);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mQuadViewManager = QuadViewManagerFactory.createQuadViewFromPreset(
                mRecognizerRunnerView,
                QuadViewPreset.DEFAULT_CORNERS_FROM_PHOTOPAY_ACTIVITY
        );
        mOcrResultView = new OcrResultDotsView(
                this,
                mRecognizerRunnerView.getHostScreenOrientation(),
                mRecognizerRunnerView.getInitialOrientation()
        );
        mRecognizerRunnerView.addChildView(mOcrResultView.getView(), false);

        MetadataCallbacks metadataCallbacks = new MetadataCallbacks();
        metadataCallbacks.setQuadDetectionCallback(new QuadDetectionCallback() {
            @Override
            public void onQuadDetection(
                    @NonNull DisplayableQuadDetection displayableQuadDetection) {
                mQuadViewManager.animateQuadToDetectionPosition(displayableQuadDetection);

            }
        });
        metadataCallbacks.setPointsDetectionCallback(new PointsDetectionCallback() {
            @Override
            public void onPointsDetection(
                    @NonNull DisplayablePointsDetection displayablePointsDetection) {
                if (displayablePointsDetection.getPointsType() == PointsType.OCR_RESULT) {
                    mOcrResultView.addDisplayablePointsDetection(displayablePointsDetection);
                }
            }
        });
        metadataCallbacks.setFailedDetectionCallback(new FailedDetectionCallback() {
            @Override
            public void onDetectionFailed() {
                mQuadViewManager.animateQuadToDefaultPosition();
                mOcrResultView.clearDisplayedContent();
            }
        });
        mRecognizerRunnerView.setMetadataCallbacks(metadataCallbacks);
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
        /*
         * Clear temporary file created in onSaveInstanceState in case no activity restart happened
         * after call to onSaveInstanceState. If restart happened and temporary file was consumed
         * by loadFromBundle method in onCreate, then this method will do nothing.
         */
        mRecognizerBundle.clearSavedState();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecognizerRunnerView.pause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
         /*
         * If using IntentDataTransferMode.PERSISTED_OPTIMISED transfer mode for RecognitionBundle,
         * then it is backed by temporary file which gets deleted each time loadFromBundle is called.
         * This can cause crash if your activity gets restarted by the Android. To prevent that crash
         * you should save RecognizerBundle's state in your onSaveInstanceState method. This will
         * ensure that bundle is written back to temporary file that will be available for loadFromBundle
         * method if activity gets restarted. However, if no restart occur, you must ensure this
         * temporary file gets deleted. Therefore, you must call clearSavedState in your onResume callback.
         */
        mRecognizerBundle.saveState();
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
        mRecognizerRunnerView.changeConfiguration(newConfig);
        mQuadViewManager.configurationChanged(mRecognizerRunnerView, newConfig);
        mOcrResultView.setHostActivityOrientation(mRecognizerRunnerView.getHostScreenOrientation());
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final ScanResultListener mScanResultListener = new ScanResultListener() {
        @Override
        @WorkerThread
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            if (recognitionSuccessType == RecognitionSuccessType.SUCCESSFUL) {
                // pause scanning to prevent new results while activity is being shut down
                mRecognizerRunnerView.pauseScanning();

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                mRecognizerBundle.saveToIntent(intent);
                finish();
            }
        }

        @Override
        public void onUnrecoverableError(@NonNull Throwable throwable) {
            Toast.makeText(IDScanActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
            finish();
        }

    };

    private final OnSizeChangedListener mOnSizeChangedListener = new OnSizeChangedListener() {
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
    };

    private final CameraEventsListener mCameraEventsListener = new CameraEventsListener() {
        @Override
        public void onCameraPermissionDenied() {
            mCameraPermissionManager.askForCameraPermission();
        }

        @Override
        public void onCameraPreviewStarted() {
            enableTorchButtonIfPossible();
        }

        @Override
        public void onCameraPreviewStopped() {

        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(IDScanActivity.this, throwable, "Error on initialisation");
            Toast.makeText(
                    IDScanActivity.this,
                    "Error on init. See ADB log for details!",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
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

    private void enableTorchButtonIfPossible() {
        if (mRecognizerRunnerView.isCameraTorchSupported() && mTorchButton != null) {
            mTorchButton.setVisibility(View.VISIBLE);
            mTorchButton.setImageResource(R.drawable.mb_ic_flash_off_24dp);
            mTorchEnabled = false;
            mTorchButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mRecognizerRunnerView.setTorchState(!mTorchEnabled, new SuccessCallback() {
                        @Override
                        public void onOperationDone(final boolean success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(IDScanActivity.this, "Setting torch to {}. Success: {}", !mTorchEnabled, success);
                                    if (success) {
                                        mTorchEnabled = !mTorchEnabled;
                                        if (mTorchEnabled) {
                                            mTorchButton.setImageResource(R.drawable.mb_ic_flash_on_24dp);
                                        } else {
                                            mTorchButton.setImageResource(R.drawable.mb_ic_flash_off_24dp);
                                        }
                                        mTorchButton.requestLayout();
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

}
