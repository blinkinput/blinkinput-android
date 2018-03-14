package com.microblink.input;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.detector.DetectorRecognizer;
import com.microblink.results.date.Date;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;
import com.microblink.util.templating.CroatianIDFrontSideTemplatingUtil;

import java.util.ArrayList;
import java.util.Locale;

public class MenuActivity extends Activity {

    /** List view elements. */
    private ListElement[] mElements;

    private static final int REQ_CODE_CROID_FRONT = 123;

    /**
     * Prepares all parsers and templating recognizer for scanning front side of
     * the Croatian ID front side and holds parsers references which are used later for
     * obtaining scan results.
     */
    private CroatianIDFrontSideTemplatingUtil mCroatianIDFrontSideTemplatingUtil;

    private DetectorRecognizer mCroatianIdFrontTemplatingRecognizer;

    /** Reference to bundle is kept, it is used later for loading results from intent */
    private RecognizerBundle mRecognizerBundle;

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
                mElements[position].getAction().run();
            }
        });
    }

    /**
     * This method is used to build the array of {@link ListElement} objects.
     * @return Array of {@link ListElement} objects. Each {@link ListElement}
     * object will have its title that will be shown in ListView and prepared {@link Runnable}
     * action that can be used to start the {@link IDScanActivity}.
     */
    private ListElement[] buildListElements() {
        ArrayList<ListElement> elements = new ArrayList<>();

        // templating API sample (Croatian ID card - front side)
        elements.add(buildCroatianIdFrontElement());

        ListElement[] elemsArray = new ListElement[elements.size()];
        return elements.toArray(elemsArray);
    }


    private ListElement buildCroatianIdFrontElement() {
        mCroatianIDFrontSideTemplatingUtil = new CroatianIDFrontSideTemplatingUtil();

        return new ListElement(getString(R.string.croatian_id_front), new Runnable() {
            @Override
            public void run() {
                mCroatianIdFrontTemplatingRecognizer = mCroatianIDFrontSideTemplatingUtil.getDetectorRecognizer();
                mRecognizerBundle =
                        new RecognizerBundle(mCroatianIdFrontTemplatingRecognizer);
                mRecognizerBundle.setNumMsBeforeTimeout(10_000);
                startScanActivity(mRecognizerBundle, REQ_CODE_CROID_FRONT);
            }
        });
    }

    private void startScanActivity(RecognizerBundle recognizerBundle, int requestCode) {
        Intent intent = new Intent(MenuActivity.this, IDScanActivity.class);
        recognizerBundle.saveToIntent(intent);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == IDScanActivity.RESULT_OK && requestCode == REQ_CODE_CROID_FRONT) {
            // now we can load bundle with scan results, after loading, results will be available
            // through recognizer instances
            mRecognizerBundle.loadFromIntent(data);

            if (mCroatianIdFrontTemplatingRecognizer.getResult().getResultState()
                    == Recognizer.Result.State.Valid) {

                DialogFragment dialogFragment =
                        ResultDialogFragment.newInstance(
                                extractCroatianIdFrontData()
                        );
                dialogFragment.show(getFragmentManager(), "resultDialog");
            }

        }
    }

    private String extractCroatianIdFrontData() {
        StringBuilder sb = new StringBuilder();
        String newline = "\n";

        addEntry(sb, R.string.result_key_first_name,
                mCroatianIDFrontSideTemplatingUtil.getFirstNameParser().getResult().toString());
        sb.append(newline);

        addEntry(sb, R.string.result_key_last_name,
                mCroatianIDFrontSideTemplatingUtil.getLastNameParser().getResult().toString());
        sb.append(newline);

        // either new or old document number parser contains valid result (depends on the scanned document type)
        @StringRes int documentNumberKeyResourceId = R.string.result_key_old_document_number;
        String documentNumber = mCroatianIDFrontSideTemplatingUtil.getOldDocumentNumberParser().getResult().toString();
        if (documentNumber.isEmpty()) {
            documentNumber = mCroatianIDFrontSideTemplatingUtil.getNewDocumentNumberParser().getResult().toString();
            documentNumberKeyResourceId = R.string.result_key_new_document_number;
        }
        addEntry(sb, documentNumberKeyResourceId, documentNumber);
        sb.append(newline);

        addEntry(sb, R.string.result_key_sex,
                mCroatianIDFrontSideTemplatingUtil.getSexParser().getResult().toString());
        sb.append(newline);

        addEntry(sb, R.string.result_key_citizenship,
                mCroatianIDFrontSideTemplatingUtil.getCitizenshipParser().getResult().toString());
        sb.append(newline);

        Date dateOfBirth = mCroatianIDFrontSideTemplatingUtil.getDateOfBirthParser().getResult().getDate().getDate();
        if (dateOfBirth != null) {
            addEntry(sb, R.string.result_key_date_of_birth, formatDate(dateOfBirth));
        }

        return sb.toString();
    }

    private void addEntry(StringBuilder stringBuilder, @StringRes int entryKeyResourceId, @NonNull String value) {
        stringBuilder.append(getString(entryKeyResourceId)).append(": ").append(value);
    }

    private String formatDate(@NonNull Date date) {
        return String.format(Locale.US, "%02d.%02d.%d.", date.getDay(), date.getMonth(), date.getYear());
    }

    /**
     * Element of {@link ArrayAdapter} for {@link ListView} that holds information about title
     * which should be displayed in list and {@link Runnable} action that should be started on click.
     */
    private class ListElement {
        private String mTitle;
        private Runnable mAction;

        @NonNull
        public String getTitle() {
            return mTitle;
        }

        @NonNull
        Runnable getAction() {
            return mAction;
        }

        ListElement(@NonNull String title, @NonNull Runnable action) {
            mTitle = title;
            mAction = action;
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
