package com.microblink.input;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.microblink.blinkinput.entities.recognizers.blinkinput.documentcapture.DocumentCaptureRecognizer;
import com.microblink.blinkinput.entities.recognizers.blinkinput.documentcapture.DocumentCaptureRecognizerTransferable;
import com.microblink.blinkinput.fragment.RecognizerRunnerFragment;
import com.microblink.blinkinput.fragment.overlay.ScanningOverlay;
import com.microblink.blinkinput.fragment.overlay.documentcapture.DocumentCaptureOverlayController;
import com.microblink.blinkinput.fragment.overlay.documentcapture.DocumentCaptureOverlaySettings;
import com.microblink.blinkinput.fragment.overlay.documentcapture.detectionui.DetectionOverlayStrings;
import com.microblink.blinkinput.fragment.overlay.documentcapture.detectionui.DetectionOverlayView;
import com.microblink.blinkinput.fragment.overlay.verification.OverlayTorchStateListener;
import com.microblink.blinkinput.recognition.RecognitionSuccessType;
import com.microblink.blinkinput.view.recognition.ScanResultListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

public class CustomDocumentCaptureActivity extends AppCompatActivity implements
        RecognizerRunnerFragment.ScanningOverlayBinder {

    private MenuItem torchButton;

    private DocumentCaptureOverlayController overlayController;
    private DocumentCaptureRecognizerTransferable recognizerTransferable;
    private RecognizerRunnerFragment recognizerRunnerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // DocumentCaptureRecognizerTransferable can be created here or loaded from intent if it has
        // been saved to the intent which has been used to start this activity.
        // To load it from the intent, use:
        // recognizerTransferable = DocumentCaptureRecognizerTransferable.createFromIntent(getIntent());

        DocumentCaptureRecognizer documentCaptureRecognizer = new DocumentCaptureRecognizer();
        documentCaptureRecognizer.setReturnFullDocumentImage(true);
        recognizerTransferable = new DocumentCaptureRecognizerTransferable(documentCaptureRecognizer);

        // overlay controller must be initialised before calling super.onCreate() because it implements
        // ScanningOverlay interface and callback ScanningOverlay.onRecognizerRunnerFragmentAttached
        overlayController = createOverlayController(recognizerTransferable);
        overlayController.setTorchStateListener(torchStateListener);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_document_capture);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbarScan));

        if (null == savedInstanceState) {
            // create fragment transaction to replace R.id.recognizer_runner_view_container with RecognizerRunnerFragment
            recognizerRunnerFragment = new RecognizerRunnerFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.recognizer_runner_view_container, recognizerRunnerFragment);
            fragmentTransaction.commit();
        } else {
            // obtain reference to fragment restored by Android within super.onCreate() call
            recognizerRunnerFragment = (RecognizerRunnerFragment) getSupportFragmentManager().findFragmentById(R.id.recognizer_runner_view_container);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        torchButton = menu.findItem(R.id.actionTorch);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.actionTorch) {
            overlayController.onTorchButtonClicked();
            return true;
        }
        return false;
    }

    private DocumentCaptureOverlayController createOverlayController(
            DocumentCaptureRecognizerTransferable recognizerTransferable
    ) {
        return new DocumentCaptureOverlayController(
                new DocumentCaptureOverlaySettings.Builder(recognizerTransferable).build(),
                resultListener,
                createDetectionOverlayView()
        );
    }


    private DetectionOverlayView createDetectionOverlayView() {
        // here you can customise strings in the detection overlay
        DetectionOverlayStrings strings = new DetectionOverlayStrings.Builder(this).build();
        // constructor: DetectionOverlayView(DetectionOverlayStrings strings, int style, boolean showTopButtons)
        // We are using default style and pass false for the third argument to hide top buttons in
        // the overlay (close and torch button)
        // In this custom activity, we are using torch button from the options menu
        return new DetectionOverlayView(strings, 0, false);
    }

    @NonNull
    @Override
    public ScanningOverlay getScanningOverlay() {
        return overlayController;
    }


    private ScanResultListener resultListener = new ScanResultListener() {
        @Override
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            // pause scanning to prevent new results while activity is being shut down
            recognizerRunnerFragment.getRecognizerRunnerView().pauseScanning();

            Intent intent = new Intent();

            switch (recognitionSuccessType) {
                case SUCCESSFUL:
                case PARTIAL:
                    setResult(Activity.RESULT_OK, intent);
                    break;
                case UNSUCCESSFUL:
                    setResult(Activity.RESULT_CANCELED);
                    break;
            }

            recognizerTransferable.saveToIntent(intent);
            finish();
        }

        @Override
        public void onUnrecoverableError(@NonNull Throwable throwable) {
            Toast.makeText(CustomDocumentCaptureActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private OverlayTorchStateListener torchStateListener = new OverlayTorchStateListener() {
        @Override
        public void onTorchStateInitialised(boolean cameraSupportsTorch) {
            if (!cameraSupportsTorch) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        torchButton.setVisible(false);
                    }
                });
            }
        }

        @Override
        public void onTorchStateChanged(boolean torchOn) {
            final int torchIconResource = torchOn ? R.drawable.mb_ic_flash_on_24dp : R.drawable.mb_ic_flash_off_24dp;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    torchButton.setIcon(torchIconResource);
                }
            });
        }
    };
}
