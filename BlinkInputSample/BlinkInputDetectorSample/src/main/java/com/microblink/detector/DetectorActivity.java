package com.microblink.detector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.microblink.blinkinput.entities.detectors.quad.QuadWithSizeDetector;
import com.microblink.blinkinput.entities.processors.imageReturn.ImageReturnProcessor;
import com.microblink.blinkinput.entities.recognizers.RecognizerBundle;
import com.microblink.blinkinput.entities.recognizers.detector.DetectorRecognizer;
import com.microblink.blinkinput.entities.recognizers.templating.ProcessorGroup;
import com.microblink.blinkinput.entities.recognizers.templating.TemplatingClass;
import com.microblink.blinkinput.entities.recognizers.templating.dewarpPolicies.DPIBasedDewarpPolicy;
import com.microblink.blinkinput.geometry.Rectangle;
import com.microblink.blinkinput.hardware.SuccessCallback;
import com.microblink.blinkinput.hardware.orientation.Orientation;
import com.microblink.input.R;
import com.microblink.blinkinput.metadata.MetadataCallbacks;
import com.microblink.blinkinput.metadata.detection.FailedDetectionCallback;
import com.microblink.blinkinput.metadata.detection.quad.DisplayableQuadDetection;
import com.microblink.blinkinput.metadata.detection.quad.QuadDetectionCallback;
import com.microblink.blinkinput.recognition.RecognitionSuccessType;
import com.microblink.blinkinput.util.CameraPermissionManager;
import com.microblink.blinkinput.util.Log;
import com.microblink.blinkinput.view.BaseCameraView;
import com.microblink.blinkinput.view.CameraEventsListener;
import com.microblink.blinkinput.view.OnSizeChangedListener;
import com.microblink.blinkinput.view.OrientationAllowedListener;
import com.microblink.blinkinput.view.exception.NonLandscapeOrientationNotSupportedException;
import com.microblink.blinkinput.view.recognition.RecognizerRunnerView;
import com.microblink.blinkinput.view.recognition.ScanResultListener;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewAnimationListener;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewManager;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.blinkinput.view.viewfinder.quadview.QuadViewPreset;

import androidx.annotation.NonNull;

public class DetectorActivity extends Activity {

    /** Intent extras key for setting the {@link QuadWithSizeDetector} that will be used. */
    public static final String EXTRAS_DETECTOR = "EXTRAS_DETECTOR";

    private final int MY_STORAGE_REQUEST_CODE = 6969;

    /** Builtin view that controls camera and recognition */
    private RecognizerRunnerView mRecognizerRunnerView;
    /** View which holds scan result. */
    private View mResultView;
    /** Shows result image. */
    private ImageView mImageView;
    /** This is BlinkInput's built-in helper for built-in view that draws detection location */
    private QuadViewManager mQuadViewManager;
    /** Currently shown bitmap created from dewarped image. */
    private Bitmap mShownBitmap;
    /** This is a torch control button */
    private ImageButton mTorchButton;
    /** Is torch currently enabled? */
    private boolean mTorchEnabled = false;
    /** Back button from recognizer overlay */
    private ImageButton mBackButton;
    /** Save button in result view */
    private Button mBtnSave;

    private boolean mActivityBooting = true;
    /** Indicates whether the new result should be shown */
    private boolean mHaveResult = false;
    /** CameraPermissionManager is provided helper class that can be used to obtain the permission to use camera.
     * It is used on Android 6.0 (API level 23) or newer.
     */
    private CameraPermissionManager mCameraPermissionManager;

    /**
     * Processor that is used to obtain images of the detected document.
     */
    private ImageReturnProcessor mImageReturnProcessor;

    private enum ActivityState {
        DESTROYED,
        CREATED,
        STARTED,
        RESUMED
    }
    private ActivityState mActivityState = ActivityState.DESTROYED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_detector);
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

        mActivityState = ActivityState.CREATED;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        QuadWithSizeDetector detector = null;

        if (extras != null) {
            detector = extras.getParcelable(EXTRAS_DETECTOR);
        }

        if (detector == null) {
            Toast.makeText(this, "EXTRAS_DETECTOR intent extra not set! Please set " +
                    "detector that you want to to use.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mRecognizerRunnerView = findViewById(R.id.rec_view);

        DetectorRecognizer detectorRecognizer;
        // setup detector recognizer
        {
            detectorRecognizer = new DetectorRecognizer(detector);
            // processor that will simply save obtained image
            mImageReturnProcessor = new ImageReturnProcessor();
            // processor group that will be executed on the detected document location
            ProcessorGroup processorGroup = new ProcessorGroup(
                    // process entire detected location
                    new Rectangle(0.f, 0.f, 1.f, 1.f),
                    // dewarp height will be calculated based on actual physical size of detected
                    // location and requested DPI
                    new DPIBasedDewarpPolicy(200),
                    // only image is needed
                    mImageReturnProcessor
            );

            // Templating class is used to define how specific document type should be processed.
            // Only image should be returned, which means that classification of the document
            // based on the processed data is not needed, so only one document class is defined.
            TemplatingClass documentClass = new TemplatingClass();
            // prepared processor group is added to classification processor groups because
            // they are executed before classification
            documentClass.setClassificationProcessorGroups(processorGroup);

            detectorRecognizer.setTemplatingClasses(documentClass);
        }

        mRecognizerRunnerView.setRecognizerBundle( new RecognizerBundle(detectorRecognizer) );

        // camera events listener receives events such as when camera preview has started
        // or there was an error while starting the camera
        mRecognizerRunnerView.setCameraEventsListener(mCameraEventsListener);
        // scan result listener will be notified when scan result gets available
        mRecognizerRunnerView.setScanResultListener(mScanResultListener);
        mRecognizerRunnerView.setPinchToZoomAllowed(true);
        // on size changed listener will be notified when camera view changes the size of itself
        // and its children.
        mRecognizerRunnerView.setOnSizeChangedListener(mOnSizeChangedListener);
        // allow all orientations
        mRecognizerRunnerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });

        MetadataCallbacks metadataCallbacks = new MetadataCallbacks();

        metadataCallbacks.setFailedDetectionCallback(mFailedDetectionCallback);
        metadataCallbacks.setQuadDetectionCallback(mQuadDetectionCallback);

        mRecognizerRunnerView.setMetadataCallbacks(metadataCallbacks);

        mCameraPermissionManager = new CameraPermissionManager(this);
        // get the built in layout that should be displayed when camera permission is not given
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            ViewGroup vg = findViewById(R.id.main_root);
            vg.addView(v);
        }

        mRecognizerRunnerView.create();

        mResultView = getLayoutInflater().inflate(R.layout.detector_detection_result, mRecognizerRunnerView, false);
        mResultView.setVisibility(View.INVISIBLE);
        mImageView = mResultView.findViewById(R.id.imgDewarped);

        Button btnCancel = mResultView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // release Bitmap
                mImageView.setImageResource(android.R.color.transparent);
                mShownBitmap = null;
                // hide resultView
                mResultView.setVisibility(View.INVISIBLE);
                mRecognizerRunnerView.invalidate();

                // resume scanning
                if (mRecognizerRunnerView != null && mActivityState == ActivityState.RESUMED && mRecognizerRunnerView.getCameraViewState() == BaseCameraView.CameraViewState.RESUMED) {
                    mRecognizerRunnerView.resumeScanning(true);
                }
            }
        });

        mBtnSave = mResultView.findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save image to gallery
                String url = MediaStore.Images.Media.insertImage(DetectorActivity.this.getContentResolver(), mShownBitmap, "Detected Image", "");
                Log.i(DetectorActivity.this, "Image saved to URL {}", url);

                // release Bitmap
                mImageView.setImageResource(android.R.color.transparent);
                mShownBitmap = null;
                // hide resultView
                mResultView.setVisibility(View.INVISIBLE);
                mRecognizerRunnerView.invalidate();

                // resume scanning
                if (mRecognizerRunnerView != null && mActivityState == ActivityState.RESUMED && mRecognizerRunnerView.getCameraViewState() == BaseCameraView.CameraViewState.RESUMED) {
                    mRecognizerRunnerView.resumeScanning(true);
                }
            }
        });

        ViewGroup cameraOverlay = (ViewGroup) getLayoutInflater().inflate(R.layout.detector_camera_overlay, mRecognizerRunnerView, false);

        mTorchButton = cameraOverlay.findViewById(R.id.btnFlash);
        mBackButton = cameraOverlay.findViewById(R.id.btnExit);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });


        // initialize QuadViewManager
        // Use provided factory method from QuadViewManagerFactory that can instantiate the
        // QuadViewManager based on several presets defined in QuadViewPreset enum. Details about
        // each of them can be found in javadoc. This method automatically adds the QuadView as a
        // child of RecognizerView.
        // Here we use preset which sets up quad view for scanning ID documents
        mQuadViewManager = QuadViewManagerFactory.createQuadViewFromPreset(mRecognizerRunnerView, QuadViewPreset.DEFAULT_CORNERS_FROM_PHOTOPAY_ACTIVITY);

        // set animation listener to quad view manager that will show result when animation ends
        mQuadViewManager.setAnimationListener(new QuadViewAnimationListener() {
            @Override
            public void onAnimationStart() {

            }

            @Override
            public void onAnimationEnd() {
                if(mHaveResult) {
                    showImage();
                }
            }
        });

        cameraOverlay.addView(mResultView);
        mRecognizerRunnerView.addChildView(cameraOverlay, true);

        mActivityBooting = true;
    }

    @Override
    protected void onStart() {
        mActivityState = ActivityState.STARTED;
        // all activity lifecycle events must be passed to RecognizerView
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.start();
        }
        mActivityBooting = true;
        super.onStart();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        mActivityState = ActivityState.RESUMED;
        // all activity lifecycle events must be passed to RecognizerView
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.resume();
        }
        mActivityBooting = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mActivityState = ActivityState.STARTED;
        // all activity lifecycle events must be passed to RecognizerView
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.pause();
        }
        mActivityBooting = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        mActivityState = ActivityState.CREATED;
        // all activity lifecycle events must be passed to RecognizerView
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.stop();
        }
        mActivityBooting = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mActivityState = ActivityState.DESTROYED;
        // all activity lifecycle events must be passed to RecognizerView
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.destroy();
        }
        mActivityBooting = false;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // change configuration of scanner's internal views
        if (mRecognizerRunnerView != null) {
            mRecognizerRunnerView.changeConfiguration(newConfig);
        }
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_STORAGE_REQUEST_CODE) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mBtnSave.setVisibility(View.VISIBLE);
            }
        }
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showImage() {
        if(mImageReturnProcessor.getResult().getRawImage() != null) {
            // create bitmap out of last dewarped image
            mShownBitmap = mImageReturnProcessor.getResult().getRawImage().convertToBitmap();
            // display bitmap
            mImageView.setImageBitmap(mShownBitmap);
            // display overlay
            mResultView.setVisibility(View.VISIBLE);
            mHaveResult = false;

            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    mBtnSave.setVisibility(View.INVISIBLE);
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_STORAGE_REQUEST_CODE);
                }
            }
        }
    }

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

    private final ScanResultListener mScanResultListener = new ScanResultListener() {
        @Override
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            mRecognizerRunnerView.pauseScanning();
            if(!mQuadViewManager.isAnimationInProgress()) {
                // if animation has ended, show result
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showImage();
                    }
                });
            } else {
                // else result will be shown when animation ends (animation listener)
                mHaveResult = true;
            }
        }

        @Override
        public void onUnrecoverableError(@NonNull Throwable throwable) {
            Toast.makeText(DetectorActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private final CameraEventsListener mCameraEventsListener = new CameraEventsListener() {
        @Override
        public void onCameraPermissionDenied() {
            // this method is called on Android 6.0 and newer if camera permission was not given
            // by user

            // ask user to give a camera permission. Provided manager asks for
            // permission only if it has not been already granted.
            // on API level < 23, this method does nothing
            mCameraPermissionManager.askForCameraPermission();
        }

        @Override
        public void onCameraPreviewStarted() {
            if (mActivityState == ActivityState.RESUMED) {
                enableTorchButtonIfPossible();
            }
        }

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
                                        Log.d(DetectorActivity.this, "Setting torch to {}. Success: {}", !mTorchEnabled, success);
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

        @Override
        public void onCameraPreviewStopped() {

        }

        @Override
        public void onError(Throwable exc) {
            showErrorDialog(exc.getMessage());
        }

        private void showErrorDialog(String message) {
            if (mActivityBooting) {
                AlertDialog.Builder ab = new AlertDialog.Builder(DetectorActivity.this);
                ab.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).setTitle("Problem").setMessage(message).setCancelable(false).create().show();
            } else {
                Log.w(this, "Cannot show dialog because activity is exiting!");
            }
        }

        @Override
        public void onAutofocusFailed() {
            Toast.makeText(DetectorActivity.this, "Camera cannot autofocus. Please try scanning under better light", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAutofocusStarted(Rect[] focusAreas) {

        }

        @Override
        public void onAutofocusStopped(Rect[] focusAreas) {

        }
    };

    private final QuadDetectionCallback mQuadDetectionCallback = new QuadDetectionCallback() {
        @Override
        public void onQuadDetection(@NonNull DisplayableQuadDetection quadDetection) {
            mQuadViewManager.animateQuadToDetectionPosition(quadDetection);
        }
    };

    private final FailedDetectionCallback mFailedDetectionCallback = new FailedDetectionCallback() {
        @Override
        public void onDetectionFailed() {
            mQuadViewManager.animateQuadToDefaultPosition();
        }
    };

}
