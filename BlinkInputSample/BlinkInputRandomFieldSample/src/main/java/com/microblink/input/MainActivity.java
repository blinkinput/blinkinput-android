package com.microblink.input;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.microblink.activity.RandomFieldScanActivity;
import com.microblink.entities.parsers.amount.AmountParser;
import com.microblink.entities.parsers.config.randomfield.RandomFieldBundle;
import com.microblink.entities.parsers.config.randomfield.RandomFieldElement;
import com.microblink.entities.parsers.config.randomfield.RandomFieldElementGroup;
import com.microblink.entities.parsers.date.DateParser;
import com.microblink.entities.parsers.email.EMailParser;
import com.microblink.entities.parsers.iban.IbanParser;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.RandomFieldUISettings;

public class MainActivity extends Activity {

    private static final int SINGLE_GROUP_REQ_CODE = 123;
    private static final int MULTIPLE_GROUPS_REQ_CODE = 234;

    // parsers are member variables because it will be used for obtaining results
    private DateParser mDateParser;
    private AmountParser mAmountParser;
    private IbanParser mIbanParser;
    private EMailParser mEMailParser;

    /** Reference to bundle is kept, it is used later for loading results from intent */
    private RandomFieldBundle mRandomFieldBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called as handler for "Single parser group" button.
     */
    public void onScanSingleGroup(View view) {

        /*
         * In this sample we will use BlinkInput SDK to create a sample that scans an amount,
         * IBAN and date from invoice. We will use provided RandomFieldScanActivity to perform scan.
         * In this case all parsers can be placed in the same parser group.
         *
         * OCR is performed on the input image once for each parser group, so there is a tradeoff
         * between speed and accuracy.
         *
         * If all parsers are in the same parser group, recognition will be faster, but sometimes
         * merged OCR engine options may cause that some parsers are unable to extract valid data
         * from the scanned text. Putting each parser into its own group will give better accuracy,
         * but will perform OCR on input image for each parser which can consume a lot of processing
         * time.
         */

        // parsers are used later for obtaining results
        mIbanParser = new IbanParser();
        mAmountParser = new AmountParser();
        mDateParser = new DateParser();

        // Each scan element holds following scan settings:
        // resource ID (or string) for title displayed in scan activity,
        // and parser defining how the data will be extracted.
        RandomFieldElement dateElement = new RandomFieldElement(R.string.date_title, mDateParser);
        // element can be optional, which means that result can be returned without scanning that
        // element, by default, all elements are required
        dateElement.setOptional(true);

        mRandomFieldBundle = new RandomFieldBundle(
                // all elements are in the same group
                new RandomFieldElementGroup(
                        new RandomFieldElement(R.string.iban_title, mIbanParser),
                        new RandomFieldElement(R.string.amount_title, mAmountParser),
                        dateElement
                )
        );

        // we use RandomFieldUISettings - settings for RandomFieldScanActivity
        RandomFieldUISettings scanActivitySettings = new RandomFieldUISettings(mRandomFieldBundle);

        // it is possible to set the resource ID of the sound to be played when the scan element
        // is recognized
        scanActivitySettings.setBeepSoundResourceID(R.raw.beep);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity
//        scanActivitySettings.setHelpIntent(new Intent(this, HelpActivity.class));

        // It is possible to  to change default scan message that is displayed above
        // the scanning window. You can use the following code snippet to set scan message string
//        scanActivitySettings.setScanMessageResourceID(<your_message_resource_id>);

        // this helper method should be used for starting the provided activities with prepared
        // scan settings
         ActivityRunner.startActivityForResult(this, SINGLE_GROUP_REQ_CODE, scanActivitySettings);
    }

    /**
     * Called as handler for "Multiple parser groups" button.
     */
    public void onScanMultipleGroups(View view) {
        /*
         * This example is similar to "Single parser group" example. Email element is added
         * which causes that some parsers are unable to parse valid element data if all parsers
         * are placed in the same parser group. Because of this, we will put parsers in the two parser
         * groups and scanning will be slower because OCR of image will be performed for each parser
         * group.
         */

        // parsers are used later for obtaining results
        mIbanParser = new IbanParser();
        mAmountParser = new AmountParser();
        mDateParser = new DateParser();
        mEMailParser = new EMailParser();

        // Each scan element holds following scan settings:
        // resource ID (or string) for title displayed in scan activity,
        // and parser defining how the data will be extracted.
        RandomFieldElement dateElement = new RandomFieldElement(R.string.date_title, mDateParser);
        // element can be optional, which means that result can be returned without scanning that
        // element, by default, all elements are required
        dateElement.setOptional(true);

        RandomFieldElement emailElement = new RandomFieldElement(R.string.email_title, mEMailParser);
        emailElement.setOptional(true);

        mRandomFieldBundle = new RandomFieldBundle(
                // we will put elements in two groups
                // 1) IBAN, amount and date parsers
                // 2) email parser
                new RandomFieldElementGroup(
                        new RandomFieldElement(R.string.iban_title, mIbanParser),
                        new RandomFieldElement(R.string.amount_title, mAmountParser),
                        dateElement
                ),
                new RandomFieldElementGroup(
                        emailElement
                )
        );

        // we use RandomFieldUISettings - settings for RandomFieldScanActivity
        RandomFieldUISettings scanActivitySettings = new RandomFieldUISettings(mRandomFieldBundle);

        scanActivitySettings.setBeepSoundResourceID(R.raw.beep);

        // this helper method should be used for starting the provided activities with prepared
        // scan settings
        ActivityRunner.startActivityForResult(this, MULTIPLE_GROUPS_REQ_CODE, scanActivitySettings);

    }

    /**
     * This method is called whenever control is returned from scan activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // first we need to check that we have indeed returned from RandomFieldScanActivity with
        // success
        if (resultCode == RandomFieldScanActivity.RESULT_OK) {
            // now we can load bundle with scan results, after loading, results will be available
            // in parser instances
            mRandomFieldBundle.loadFromIntent(data);
            String iban = mIbanParser.getResult().toString();
            String amount = mAmountParser.getResult().toString();
            String date = mDateParser.getResult().toString();
            String resultFormat = "IBAN: %s%nAmount: %s%nDate: %s";
            switch (requestCode) {
                case SINGLE_GROUP_REQ_CODE:
                    Toast.makeText(
                            this,
                            String.format(resultFormat, iban, amount, date),
                            Toast.LENGTH_LONG
                    ).show();
                    break;
                case MULTIPLE_GROUPS_REQ_CODE:
                    String email = mEMailParser.getResult().toString();
                    Toast.makeText(
                            this,
                            String.format(resultFormat + "%nEmail: %s", iban, amount, date, email),
                            Toast.LENGTH_LONG
                    ).show();
                    break;
            }
        }
    }
}
