package com.microblink.detector;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.microblink.blinkinput.BaseMenuActivity;
import com.microblink.blinkinput.MenuListItem;
import com.microblink.blinkinput.entities.detectors.Detector;
import com.microblink.blinkinput.entities.detectors.quad.document.DocumentDetector;
import com.microblink.blinkinput.entities.detectors.quad.document.DocumentSpecification;
import com.microblink.blinkinput.entities.detectors.quad.document.DocumentSpecificationPreset;
import com.microblink.blinkinput.entities.detectors.quad.mrtd.MRTDDetector;
import com.microblink.input.R;
import com.microblink.blinkinput.util.RecognizerCompatibility;
import com.microblink.blinkinput.util.RecognizerCompatibilityStatus;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends BaseMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if BlinkInput is supported on the device
        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            Toast.makeText(this, "BlinkInput is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected List<MenuListItem> createMenuListItems() {
        ArrayList<MenuListItem> items = new ArrayList<>();

        // * Document list entries *
        // define document specification for idCard, use provided preset
        DocumentSpecification idSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_ID1_CARD);
        items.add(buildDocumentDetectorElement(getString(R.string.id_detector), idSpec));

        // define document specification for cheque, use provided preset
        DocumentSpecification chequeSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_CHEQUE);
        items.add(buildDocumentDetectorElement(getString(R.string.cheque_detector), chequeSpec));

        // define document specification for A4 portrait document, use provided preset
        DocumentSpecification a4PortraitSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_A4_PORTRAIT);
        items.add(buildDocumentDetectorElement(getString(R.string.a4_portrait_detector), a4PortraitSpec));

        // define document specification for A4 landscape document, use provided preset
        DocumentSpecification a4LandscapeSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_A4_LANDSCAPE);
        items.add(buildDocumentDetectorElement(getString(R.string.a4_landscape_detector), a4LandscapeSpec));

        // * MRTD list entry *
        items.add(buildMRTDDetectorElement());

        return items;
    }

    @Override
    protected String getTitleText() {
        return getString(R.string.title_activity_menu);
    }

    private MenuListItem buildMRTDDetectorElement() {
        final MRTDDetector mrtdDetector = new MRTDDetector();
        return new MenuListItem(getString(R.string.mrtd_detector), new Runnable() {
            @Override
            public void run() {
                startActivity(buildDetectorIntent(mrtdDetector));
            }
        });
    }

    private MenuListItem buildDocumentDetectorElement(String title, DocumentSpecification documentSpec) {
        // prepare document detector with defined document specification
        final DocumentDetector documentDetector = new DocumentDetector(documentSpec);
        // set minimum number of stable detections to return detector result
        documentDetector.setNumStableDetectionsThreshold(3);
        return new MenuListItem(title, new Runnable() {
            @Override
            public void run() {
                startActivity(buildDetectorIntent(documentDetector));
            }
        });
    }

    /**
     * Builds intent that can be used to start the {@link DetectorActivity} with given detector.
     * @param detector Detector that will be used.
     * @return Intent that can be used to start the {@link DetectorActivity} with given detector.
     */
    private Intent buildDetectorIntent(Detector detector) {
        Intent intent = new Intent(this, DetectorActivity.class);
        // pass prepared detector to activity
        intent.putExtra(DetectorActivity.EXTRAS_DETECTOR, detector);
        return intent;
    }

}