package com.microblink.input;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.microblink.entities.parsers.Parser;
import com.microblink.entities.parsers.config.fieldbyfield.FieldByFieldElement;
import com.microblink.entities.processors.parserGroup.ParserGroupProcessor;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkinput.BlinkInputRecognizer;
import com.microblink.geometry.Rectangle;
import com.microblink.hardware.SuccessCallback;
import com.microblink.help.HelpActivity;
import com.microblink.ocr.SlidingTabLayout;
import com.microblink.recognition.RecognitionSuccessType;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.exception.NonLandscapeOrientationNotSupportedException;
import com.microblink.view.recognition.RecognizerRunnerView;
import com.microblink.view.recognition.ScanResultListener;

/**
 * Custom field by field scan activity which uses predefined scan elements and scans fields
 * in a loop.
 */
public class CustomFieldByFieldScanActivity extends Activity {

    /** RecognizerRunnerView is the builtin view that controls camera and recognition */
    private RecognizerRunnerView mRecognizerRunnerView;

    /** Recognizer which is used for scanning, uses prepared parser group for performing OCR and
     * and active parser from the group for parsing the OCR result. */
    private BlinkInputRecognizer mBlinkInputRecognizer;
    /** Processor which is used on the input image, performs the OCR and lets parsers from the
     * group to extract data. In this sample, at any moment only one parser is in the group. */
    private ParserGroupProcessor mParserGroupProcessor;

    /** CameraPermissionManager is provided helper class that can be used to obtain the permission to use camera.
     * It is used on Android 6.0 (API level 23) or newer.
     */
    private CameraPermissionManager mCameraPermissionManager;
    /** Button that controls flashlight state. */
    private ImageButton mFlashButton;
    /** Layout which holds scan result. */
    private View mResultView;
    /** Shows scan result string. */
    private TextView mResult;
    /** Flashlight state. */
    private boolean mTorchOn = false;
    /** Shows the message of current scan configuration to user. */
    private TextView mMessage;
    /** Shows the title of current scan configuration to user. */
    private SlidingTabLayout mTitleIndicator;
    /** Array of scan elements. */
    private FieldByFieldElement[] mScanElements = CustomUIElementConfigurator.createFieldByFieldElements();
    /**
     * Last parsed result that is stored for the case when it is needed when "Accept" button is
     * pressed - {@link #onBtnAcceptClicked(View)}. */
    private Parser.Result<?> mLastResultCloned;
    /** Index of currently selected element. */
    private int mSelectedElement = 0;

    private Handler mUiThreadHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_custom_field_by_field);
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

        // obtain references to needed member variables
        mRecognizerRunnerView = findViewById(R.id.rec_view);
        mFlashButton = findViewById(R.id.btnFlash);
        mResultView = findViewById(R.id.layResult);
        mMessage = findViewById(R.id.txtMessage);
        mResult = findViewById(R.id.txtResult);
        mTitleIndicator = findViewById(R.id.indicator);

        // result is not editable
        mResult.setKeyListener(null);

        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new SamplePagerAdapter());

        mTitleIndicator = findViewById(R.id.indicator);
        mTitleIndicator.setViewPager(viewPager);

        // set ViewPager.OnPageChangeListener to enable the layout
        // to update it's scroll position correctly
        mTitleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // update currently selected configuration
                mSelectedElement = position;
                // hide previous result
                // must be post to handler used in onScanningDone, if not race condition may
                // happen which can cause that result view remains visible
                mUiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResultView.setVisibility(View.INVISIBLE);
                    }
                });

                // update message and title based on selected configuration
                // and update recognizer settings (flag is set to true)
                updateUI(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // camera events listener is required as it will receive camera-related events
        // such as startup errors, autofocus callbacks etc.
        mRecognizerRunnerView.setCameraEventsListener(mCameraEventsListener);
        // scan result listener is requires as it will receive recognition results
        mRecognizerRunnerView.setScanResultListener(mScanResultListener);

        // we want camera to use whole available view space by cropping the camera preview
        // instead of letterboxing it
        mRecognizerRunnerView.setAspectMode(CameraAspectMode.ASPECT_FILL);

        mRecognizerRunnerView.setOptimizeCameraForNearScan(true);

        // initialize BlinkInput recognizer with initial field by field element
        // create BlinkInput recognizer object and add parser to it
        FieldByFieldElement initialScanElement = mScanElements[mSelectedElement];

        Parser currentParser = initialScanElement.getParser();
        mParserGroupProcessor = new ParserGroupProcessor(currentParser);
        mRecognizerRunnerView.setRecognizerBundle(createRecognizerBundle(mParserGroupProcessor));

        // define the scanning region of the image that will be scanned.
        // You must ensure that scanning region define here is the same as in the layout
        // The coordinates for scanning region are relative to recognizer view:
        // the following means: rectangle starts at 10% of recognizer view's width and
        // 34% of its height. Rectangle width is 80% of recognizer view's width and
        // 13% of its height.
        // If you do not set this, OCR will be performed on full camera frame and this
        // will result in very poor performance.
        mRecognizerRunnerView.setScanningRegion(new Rectangle(0.1f, 0.34f, 0.8f, 0.13f), false);

        // instantiate the camera permission manager
        mCameraPermissionManager = new CameraPermissionManager(this);
        // get the built in layout that should be displayed when camera permission is not given
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            // add it to the current layout that contains the recognizer view
            ViewGroup vg = findViewById(R.id.custom_segment_scan_root);
            vg.addView(v);
        }

        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerRunnerView.create();
        // update message and title based on selected configuration
        // update of recognizer settings is not needed (flag is set to false)
        updateUI(false);
    }


    /**
     * Updates user interface based on currently selected configuration. Also updates the
     * recognizers configuration if {@code updateRecognizerSettings} is set to {@code true}.
     * @param updateRecognizerSettings Indicates whether the recognizers reconfiguration
     *                                 will be performed, based on current settings.
     */
    private void updateUI (boolean updateRecognizerSettings) {
        mMessage.setText(mScanElements[mSelectedElement].getText(this));
        mTitleIndicator.getViewPager().setCurrentItem(mSelectedElement);

        if (updateRecognizerSettings) {
            FieldByFieldElement scanElement = mScanElements[mSelectedElement];
            Parser currentParser = scanElement.getParser();
            mParserGroupProcessor = new ParserGroupProcessor(currentParser);
            // unlike setRecognitionSettings that needs to be set before calling create, reconfigureRecognizers is designed
            // to be called while recognizer is active.
            mRecognizerRunnerView.reconfigureRecognizers(createRecognizerBundle(mParserGroupProcessor));
        }
    }

    private RecognizerBundle createRecognizerBundle(ParserGroupProcessor parserGroupProcessor) {
        mBlinkInputRecognizer = new BlinkInputRecognizer(parserGroupProcessor);
        return new RecognizerBundle(mBlinkInputRecognizer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // all activity's lifecycle methods must be passed to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.destroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // all activity lifecycle events must be passed on to recognizer view
        if(mRecognizerRunnerView != null) {
            mRecognizerRunnerView.changeConfiguration(newConfig);
        }
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // on API level 23, we need to pass request permission result to camera permission manager
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onBtnExitClicked(View v) {
        finish();
    }

    public void onBtnHelpClicked(View v) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void onBtnFlashClicked(View v) {
        mRecognizerRunnerView.setTorchState(!mTorchOn, new SuccessCallback() {
            @Override
            public void onOperationDone(boolean success) {
                if (success) {
                    mTorchOn = !mTorchOn;

                    mUiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTorchOn) {
                                mFlashButton.setImageResource(R.drawable.flashlight_inverse);
                            } else {
                                mFlashButton.setImageResource(R.drawable.flashlight);
                            }
                        }
                    });
                }
            }
        });
    }

    public void onBtnAcceptClicked(View v) {
        // here you can do something with the latest stored parser result - mLastResultCloned
        Toast.makeText(
                this,
                String.format(
                        "Accepted %s: %s",
                        mScanElements[mSelectedElement].getTitle(this),
                        mLastResultCloned.toString()
                ),
                Toast.LENGTH_SHORT
        ).show();
        // move to next element
        mSelectedElement = (mSelectedElement + 1) % mScanElements.length;
        // hide previous result
        mResultView.setVisibility(View.INVISIBLE);
        updateUI(true);
    }

    private final CameraEventsListener mCameraEventsListener = new CameraEventsListener() {
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
        public void onCameraPreviewStarted() {
            // this method is called when camera preview has started
            // camera is being initialized in background thread and when
            // it is ready, this method is called.
            // You can use it to check camera properties, such as whether
            // torch is supported and then show/hide torch button.
            if (mRecognizerRunnerView != null && mRecognizerRunnerView.isCameraTorchSupported()) {
                mFlashButton.setVisibility(View.VISIBLE);
                mFlashButton.setImageResource(R.drawable.flashlight);
                mTorchOn = false;
            }

            // after camera is started, we can set the metering area for autofocus, white balance
            // and auto exposure measurements
            // we set the same rectangle as for scanning region
            // we also define that this metering area will not follow device orientation changes because
            // we have set non rotatable scanning region
            mRecognizerRunnerView.setMeteringAreas(new RectF[] {new RectF(0.1f, 0.34f, 0.1f + 0.8f, 0.34f + 0.13f)}, false);
        }

        @Override
        public void onCameraPreviewStopped() {
            // this method is called when camera preview has been stopped
        }

        @Override
        public void onError(Throwable throwable) {
            // This method will be called when opening of camera resulted in exception or
            // recognition process encountered an error.
            // The error details will be given in exc parameter.
            Log.e(this, throwable, "Error");
            AlertDialog.Builder ab = new AlertDialog.Builder(CustomFieldByFieldScanActivity.this);
            ab.setCancelable(false)
                    .setTitle("Error")
                    .setMessage(throwable.getMessage())
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(dialog != null) dialog.dismiss();
                            finish();
                        }
                    }).create().show();
            finish();
        }

        @Override
        public void onAutofocusFailed() {
            // This method is called when camera focusing has failed.
            // You should inform user to try scanning under different light.
        }

        @Override
        public void onAutofocusStarted(Rect[] rects) {
            // This method is called when camera starts focusing.
            // Focus areas is array of rectangles that camera uses
            // as focus measure regions.
        }

        @Override
        public void onAutofocusStopped(Rect[] rects) {
            // This method is called when camera finishes focusing.
        }
    };

    private final ScanResultListener mScanResultListener = new ScanResultListener() {

        @Override
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            if (recognitionSuccessType == RecognitionSuccessType.UNSUCCESSFUL) {
                // ignore event if nothing has been scanned
                return;
            }
            FieldByFieldElement scanElement = mScanElements[mSelectedElement];

            // obtain result of the currently active parser
            Parser.Result<?> parserResult = scanElement.getParser().getResult();

            if (parserResult.getResultState() == Parser.Result.State.Valid) {

                final String resultString = parserResult.toString().trim();

                mLastResultCloned = parserResult.clone();
                mUiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResult.setText(resultString);
                        mResultView.setVisibility(View.VISIBLE); }});

                // additionally if you want to process raw OCR result instead of parsed strings, and
                // your license key allows obtaining of the OCR result, you can obtain it like this:
                //  OcrResult ocrResult = mParserGroupProcessor.getResult().getOcrResult();
            }

            // Finally, scanning will be resumed automatically and will reuse
            // results from previous scan to make current scan of better quality.
            // Note that preserving state preserves state of all
            // recognizers, including barcode recognizers (if enabled).
            // If you want to reset internal state call:
            // mRecognizerRunnerView.resetRecognitionState();
        }

    };

    private class SamplePagerAdapter extends PagerAdapter {

        /**
         * @return the number of pages to display
         */
        @Override
        public int getCount() {
            return mScanElements.length;
        }

        /**
         * @return true if the value returned from {@link #instantiateItem(ViewGroup, int)} is the
         * same object as the {@link View} added to the {@link ViewPager}.
         */
        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link SlidingTabLayout}.
         * <p>
         * Here we construct one using the position value, but for real application the title should
         * refer to the item's contents.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return mScanElements[position].getTitle(CustomFieldByFieldScanActivity.this);
        }
        /**
         * Instantiate the {@link View} which should be displayed at {@code position}. Here we
         * inflate a layout from the apps resources and then change the text view to signify the position.
         */
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return new View(CustomFieldByFieldScanActivity.this);
        }

        /**
         * Destroy the item from the {@link ViewPager}. In our case this is simply removing the
         * {@link View}.
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

}
