package com.microblink.detector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.microblink.entities.detectors.Detector;
import com.microblink.entities.detectors.quad.document.DocumentDetector;
import com.microblink.entities.detectors.quad.document.DocumentSpecification;
import com.microblink.entities.detectors.quad.document.DocumentSpecificationPreset;
import com.microblink.entities.detectors.quad.mrtd.MRTDDetector;
import com.microblink.input.R;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import java.util.ArrayList;

public class MenuActivity extends Activity {

    /** List view elements. */
    private ListElement[] mElements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // check if BlinkInput is supported on the device
        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            Toast.makeText(this, "BlinkInput is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
        }

        // build list elements
        mElements = buildListElements();
        ListView lv = findViewById(R.id.detectorList);
        ArrayAdapter<ListElement> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mElements);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(mElements[position].getScanIntent());
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


    /**
     * This method is used to build the array of {@link ListElement} objects.
     * @return Array of {@link ListElement} objects. Each {@link ListElement}
     * object will have its title that will be shown in ListView and prepared intent
     * that can be used to start the {@link DetectorActivity}.
     */
    private ListElement[] buildListElements() {
        ArrayList<ListElement> elements = new ArrayList<>();

        // * Document list entries *
        // define document specification for idCard, use provided preset
        DocumentSpecification idSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_ID1_CARD);
        elements.add(buildDocumentDetectorElement(getString(R.string.id_detector), idSpec));

        // define document specification for cheque, use provided preset
        DocumentSpecification chequeSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_CHEQUE);
        elements.add(buildDocumentDetectorElement(getString(R.string.cheque_detector), chequeSpec));

        // define document specification for A4 portrait document, use provided preset
        DocumentSpecification a4PortraitSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_A4_PORTRAIT);
        elements.add(buildDocumentDetectorElement(getString(R.string.a4_portrait_detector), a4PortraitSpec));

        // define document specification for A4 landscape document, use provided preset
        DocumentSpecification a4LandscapeSpec = DocumentSpecification.createFromPreset(
                DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_A4_LANDSCAPE);
        elements.add(buildDocumentDetectorElement(getString(R.string.a4_landscape_detector), a4LandscapeSpec));

        // * MRTD list entry *
        elements.add(buildMRTDDetectorElement());

        ListElement[] elemsArray = new ListElement[elements.size()];
        return elements.toArray(elemsArray);
    }

    /**
     * Builds the {@link ListElement} with corresponding title and intent that can be
     * used to start the {@link DetectorActivity} with MRTDDetector.
     * @return Built list element.
     */
    private ListElement buildMRTDDetectorElement() {
        MRTDDetector mrtdDetector = new MRTDDetector();
        return new ListElement(getString(R.string.mrtd_detector), buildDetectorIntent(mrtdDetector));
    }

    /**
     * Builds the {@link ListElement} with given title and intent that can be used to start the
     * {@link DetectorActivity} with DocumentDetector.
     * @param title Title that will be shown in list view.
     * @param documentSpec Specification for the document that should be detected (idCard, cheque...)
     * @return Built list element.
     */
    private ListElement buildDocumentDetectorElement(String title, DocumentSpecification documentSpec) {
        // prepare document detector with defined document specification
        DocumentDetector documentDetector = new DocumentDetector(documentSpec);
        // set minimum number of stable detections to return detector result
        documentDetector.setNumStableDetectionsThreshold(3);
        return new ListElement(title, buildDetectorIntent(documentDetector));
    }

    /**
     * Element of {@link ArrayAdapter} for {@link ListView} that holds information about title
     * which should be displayed in list and {@link Intent} that should be started on click.
     */
    private class ListElement {
        private String mTitle;
        private Intent mScanIntent;

        String getTitle() {
            return mTitle;
        }

        Intent getScanIntent() {
            return mScanIntent;
        }

        ListElement(String title, Intent scanIntent) {
            mTitle = title;
            mScanIntent = scanIntent;
        }

        /**
         * Used by array adapter to determine list element text
         */
        @Override
        public String toString() {
            return getTitle();
        }
    }
}