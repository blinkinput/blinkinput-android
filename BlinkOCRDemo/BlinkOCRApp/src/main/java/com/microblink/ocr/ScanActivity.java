package com.microblink.ocr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.microblink.geometry.Rectangle;
import com.microblink.hardware.SuccessCallback;
import com.microblink.help.HelpActivity;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.ocr.blinkocr.BlinkOCRRecognitionResult;
import com.microblink.recognizers.ocr.blinkocr.BlinkOCRRecognizerSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.util.Log;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.NotSupportedReason;
import com.microblink.view.recognition.RecognitionType;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;

import java.util.HashSet;
import java.util.Set;

import it.sephiroth.android.library.widget.AdapterView;
import it.sephiroth.android.library.widget.HListView;


public class ScanActivity extends Activity implements CameraEventsListener, ScanResultListener {

    private RecognizerView mRecognizerView;
    private ImageButton mFlashButton;
    private View mResultView;
    private EditText mResult;
    private boolean mTorchOn = false;
    private TextView mMessage;
    private HListView mTitleIndicator;
    private ScanConfigurationListAdapter mTitleAdapter;
    private ScanConfiguration[] mConfiguration = Configurator.createScanConfigurations();
    private int mSelectedConfiguration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.setLogLevel(Log.LogLevel.LOG_VERBOSE);
        setContentView(R.layout.activity_scan);

        for(ScanConfiguration conf : mConfiguration) {
            conf.loadTitle(this);
        }

        mRecognizerView = (RecognizerView) findViewById(R.id.rec_view);
        mFlashButton = (ImageButton) findViewById(R.id.btnFlash);
        mResultView = findViewById(R.id.layResult);
        mMessage = (TextView) findViewById(R.id.txtMessage);
        mResult = (EditText) findViewById(R.id.txtResult);
        mTitleIndicator = (HListView) findViewById(R.id.indicator);

        mTitleAdapter = new ScanConfigurationListAdapter();
        mTitleIndicator.setAdapter(mTitleAdapter);

        Display display = getWindowManager().getDefaultDisplay();
        int screenWidth = display.getWidth();

        mTitleIndicator.setDividerWidth(screenWidth / 4);
        mTitleIndicator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedConfiguration = i;
                setupMessage(true);
            }
        });

        // camera events listener is required as it will receive camera-related events
        // such as startup errors, autofocus callbacks etc.
        mRecognizerView.setCameraEventsListener(this);
        // scan result listener is requires as it will receive recognition results
        mRecognizerView.setScanResultListener(this);
        // we want camera to use whole available view space by cropping the camera preview
        // instead of letterboxing it
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);
        // license key is required for recognizer to work.
        mRecognizerView.setLicenseKey("DB2H5WX3-T2MN76CJ-ZIO5SIWW-MKYTEYZT-UGBW36CJ-ZIMR3WJC-2ZRLCMTD-GPKSSHGK");
        mRecognizerView.setOptimizeCameraForNearScan(true);

        // create BlinkOCR recognizer settings object and add parser to it
        BlinkOCRRecognizerSettings settings = new BlinkOCRRecognizerSettings();
        settings.addParser(mConfiguration[mSelectedConfiguration].getParserName(), mConfiguration[mSelectedConfiguration].getParserSettings());
        // add BlinkOCR recognizer settings object to array of all recognizer settings and initialize
        // recognizer with that array
        mRecognizerView.setRecognitionSettings(new RecognizerSettings[]{settings});
        // define the scanning region of the image that will be scanned.
        // You must ensure that scanning region define here is the same as in the layout
        // The coordinates for scanning region are relative to recognizer view:
        // the following means: rectangle starts at 10% of recognizer view's width and
        // 34% of its height. Rectangle width is 80% of recognizer view's width and
        // 13% of its height.
        // If you do not set this, OCR will be performed on full camera frame and this
        // will result in very poor performance.
        mRecognizerView.setScanningRegion(new Rectangle(0.1f, 0.34f, 0.8f, 0.13f), false);

        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.create();

        setupMessage(false);
    }



    private void setupMessage(boolean updateRecognizerView) {
        mMessage.setText(mConfiguration[mSelectedConfiguration].getTextResource());

        mTitleAdapter.notifySelectedItemChanged();

        mTitleIndicator.setSelection(mSelectedConfiguration);

        if(updateRecognizerView) {
            BlinkOCRRecognizerSettings settings = new BlinkOCRRecognizerSettings();
            settings.addParser(mConfiguration[mSelectedConfiguration].getParserName(), mConfiguration[mSelectedConfiguration].getParserSettings());
            // unlike setRecognitionSettings that needs to be set before calling create, reconfigureRecognizers is designed
            // to be called while recognizer is active.
            mRecognizerView.reconfigureRecognizers(new RecognizerSettings[] {settings});
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.pause();
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
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // all activity's lifecycle methods must be passed to recognizer view
        mRecognizerView.changeConfiguration(newConfig);
    }

    @Override
    public void onCameraPreviewStarted() {
        // this method is called when camera preview has started
        // camera is being initialized in background thread and when
        // it is ready, this method is called.
        // You can use it to check camera properties, such as whether
        // torch is supported and then show/hide torch button.
        if (mRecognizerView != null && mRecognizerView.isCameraTorchSupported()) {
            mFlashButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStartupError(Throwable exc) {
        // this method is called when error happens whilst loading RecognizerView
        // this can be either because camera is busy and cannot be opened
        // or native library could not be loaded because of unsupported processor architecture
        Log.e(this, exc, "On startup error!");
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setCancelable(false)
          .setTitle("Error")
          .setMessage("Error while loading camera or library: " + exc.getMessage())
          .setNeutralButton("OK", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                  if(dialog != null) dialog.dismiss();
                  finish();
              }
          }).create().show();
    }

    @Override
    public void onNotSupported(NotSupportedReason reason) {
        // this method is called when RecognizerView detects that device is not
        // supported and describes the not supported reason via enum
        Log.e(this, "Not supported reason: {}", reason);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setCancelable(false)
                .setTitle("Feature not supported")
                .setMessage("Feature not supported! Reason: " + reason.name())
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(dialog != null) dialog.dismiss();
                        finish();
                    }
                }).create().show();
    }

    @Override
    public void onAutofocusFailed() {
        // This method is called when camera focusing has failed.
        // You should inform user to try scanning under different light.
    }

    @Override
    public void onAutofocusStarted(Rect[] focusAreas) {
        // This method is called when camera starts focusing.
        // Focus areas is array of rectangles that camera uses
        // as focus measure regions.
    }

    @Override
    public void onAutofocusStopped(Rect[] focusAreas) {
        // This method is called when camera finishes focusing.
    }

    @Override
    public void onScanningDone(BaseRecognitionResult[] dataArray, RecognitionType recognitionType) {
        // we've enabled only one recognizer, so we expect only one element in dataArray
        if (dataArray != null && dataArray.length == 1) {
            if (dataArray[0] instanceof BlinkOCRRecognitionResult) {
                BlinkOCRRecognitionResult result = (BlinkOCRRecognitionResult) dataArray[0];
                String scanned = result.getParsedResult(mConfiguration[mSelectedConfiguration].getParserName());
                if(scanned != null && !scanned.isEmpty()) {
                    mResult.setText(scanned);
                    mResultView.setVisibility(View.VISIBLE);
                }
                // additionally if you want to process raw OCR result of default parser group
                // instead of parsed strings you can obtain it like this
                // OcrResult ocrResult = result.getOcrResult();

                // to obtain raw OCR result for certain parser group, give a name of the parser
                // group to getOcrResult method
            }
        }
        mRecognizerView.resumeScanningWithoutStateReset();
    }

    public void onBtnExitClicked(View v) {
        finish();
    }

    public void onBtnHelpClicked(View v) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
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
        // do something with data from mResult
        mSelectedConfiguration = (mSelectedConfiguration + 1) % mConfiguration.length;

        mResultView.setVisibility(View.INVISIBLE);
        setupMessage(true);
    }

    /***** Title's list adapter *****/

    private class ScanConfigurationListAdapter implements ListAdapter {

        private Set<DataSetObserver> mObservers = new HashSet<DataSetObserver>();

        /**
         * Indicates whether all the items in this adapter are enabled. If the
         * value returned by this method changes over time, there is no guarantee
         * it will take effect.  If true, it means all items are selectable and
         * clickable (there is no separator.)
         *
         * @return True if all items are enabled, false otherwise.
         * @see #isEnabled(int)
         */
        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        /**
         * Returns true if the item at the specified position is not a separator.
         * (A separator is a non-selectable, non-clickable item).
         * <p/>
         * The result is unspecified if position is invalid. An {@link ArrayIndexOutOfBoundsException}
         * should be thrown in that case for fast failure.
         *
         * @param position Index of the item
         * @return True if the item is not a separator
         * @see #areAllItemsEnabled()
         */
        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        /**
         * Register an observer that is called when changes happen to the data used by this adapter.
         *
         * @param observer the object that gets notified when the data set changes.
         */
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mObservers.add(observer);
        }

        /**
         * Unregister an observer that has previously been registered with this
         * adapter via {@link #registerDataSetObserver}.
         *
         * @param observer the object to unregister.
         */
        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mObservers.remove(observer);
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return mConfiguration.length;
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return mConfiguration[position];
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Indicates whether the item ids are stable across changes to the
         * underlying data.
         *
         * @return True if the same id always refers to the same object.
         */
        @Override
        public boolean hasStableIds() {
            return true;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                setupView(position, convertView);
                return convertView;
            } else {
                View v = getLayoutInflater().inflate(R.layout.page_element, null);
                setupView(position, v);
                return v;
            }
        }

        private void setupView(int position, View v) {
            TextView tv = (TextView) v.findViewById(R.id.txtTitle);
            tv.setText(mConfiguration[position].getTitleResource());
            tv.setTextColor(getResources().getColor((position == mSelectedConfiguration ? R.color.text_white : R.color.text_gray)));
        }

        /**
         * Get the type of View that will be created by {@link #getView} for the specified item.
         *
         * @param position The position of the item within the adapter's data set whose view type we
         *                 want.
         * @return An integer representing the type of View. Two views should share the same type if one
         * can be converted to the other in {@link #getView}. Note: Integers must be in the
         * range 0 to {@link #getViewTypeCount} - 1. {@link #IGNORE_ITEM_VIEW_TYPE} can
         * also be returned.
         * @see #IGNORE_ITEM_VIEW_TYPE
         */
        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        /**
         * <p>
         * Returns the number of types of Views that will be created by
         * {@link #getView}. Each type represents a set of views that can be
         * converted in {@link #getView}. If the adapter always returns the same
         * type of View for all items, this method should return 1.
         * </p>
         * <p>
         * This method will only be called when when the adapter is set on the
         * the {@link AdapterView}.
         * </p>
         *
         * @return The number of types of Views that will be created by this adapter
         */
        @Override
        public int getViewTypeCount() {
            return 1;
        }

        /**
         * @return true if this adapter doesn't contain any data.  This is used to determine
         * whether the empty view should be displayed.  A typical implementation will return
         * getCount() == 0 but since getCount() includes the headers and footers, specialized
         * adapters might want a different behavior.
         */
        @Override
        public boolean isEmpty() {
            return mConfiguration.length == 0;
        }

        public void notifySelectedItemChanged() {
            for(DataSetObserver dso : mObservers) {
                dso.onChanged();
            }
        }
    }
}
