package com.microblink.input;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.microblink.blinkinput.BaseMenuActivity;
import com.microblink.blinkinput.MenuListItem;
import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.detector.DetectorRecognizer;
import com.microblink.entities.recognizers.successframe.SuccessFrameGrabberRecognizer;
import com.microblink.results.date.Date;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;
import com.microblink.util.templating.CroatianIDFrontSideTemplatingUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuActivity extends BaseMenuActivity {

    private static final int REQ_CODE_CROID_FRONT = 123;

    /**
     * Prepares all parsers and templating recognizer for scanning front side of
     * the Croatian ID front side and holds parsers references which are used later for
     * obtaining scan results.
     */
    private CroatianIDFrontSideTemplatingUtil mCroatianIDFrontSideTemplatingUtil;

    private DetectorRecognizer mCroatianIdFrontTemplatingRecognizer;
    private SuccessFrameGrabberRecognizer mSuccessFrameGrabberRecognizer;

    /** Reference to bundle is kept, it is used later for loading results from intent */
    private RecognizerBundle mRecognizerBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCroatianIDFrontSideTemplatingUtil = new CroatianIDFrontSideTemplatingUtil();

        // check if BlinkInput is supported on the device
        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            Toast.makeText(this, "BlinkInput is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String getTitleText() {
        return getString(R.string.title_activity_menu);
    }

    @Override
    protected List<MenuListItem> createMenuListItems() {
        ArrayList<MenuListItem> items = new ArrayList<>();

        // templating API sample (Croatian ID card - front side)
        items.add(buildCroatianIdFrontElement());

        return items;
    }

    private MenuListItem buildCroatianIdFrontElement() {
        return new MenuListItem(getString(R.string.croatian_id_front), new Runnable() {
            @Override
            public void run() {
                mCroatianIdFrontTemplatingRecognizer = mCroatianIDFrontSideTemplatingUtil.getDetectorRecognizer();
                mSuccessFrameGrabberRecognizer = new SuccessFrameGrabberRecognizer(mCroatianIdFrontTemplatingRecognizer);

                mRecognizerBundle = new RecognizerBundle(mSuccessFrameGrabberRecognizer);
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

            String resultImagePath;
            try {
                Bitmap successBitmap = mSuccessFrameGrabberRecognizer.getResult().getSuccessFrame().convertToBitmap();
                File resultImage = new File(getFilesDir(), "result.jpg");
                OutputStream os = new BufferedOutputStream(new FileOutputStream(resultImage));
                successBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                resultImagePath = resultImage.getAbsolutePath();
                os.close();
            } catch (Exception e) {
                resultImagePath = null;
            }

            String resultText = extractCroatianIdFrontData();
            if (mCroatianIdFrontTemplatingRecognizer.getResult().getResultState() == Recognizer.Result.State.Valid) {
                startActivity(ResultsActivity.buildIntent(this, resultText, resultImagePath));
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

}
