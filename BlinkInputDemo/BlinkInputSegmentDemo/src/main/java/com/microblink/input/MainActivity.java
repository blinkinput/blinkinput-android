package com.microblink.input;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.microblink.activity.FieldByFieldScanActivity;
import com.microblink.entities.ocrengine.legacy.BlinkOCREngineOptions;
import com.microblink.entities.parsers.amount.AmountParser;
import com.microblink.entities.parsers.config.fieldbyfield.FieldByFieldBundle;
import com.microblink.entities.parsers.config.fieldbyfield.FieldByFieldElement;
import com.microblink.entities.parsers.iban.IbanParser;
import com.microblink.entities.parsers.regex.RegexParser;
import com.microblink.entities.parsers.topup.TopUpParser;
import com.microblink.entities.parsers.topup.TopUpPreset;
import com.microblink.help.HelpActivity;
import com.microblink.results.ocr.OcrFont;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.FieldByFieldUISettings;

public class MainActivity extends Activity {

    private static final int BLINK_INPUT_REQUEST_CODE = 100;
    private static final int BLINK_INPUT_VIN_REQUEST_CODE = 101;
    private static final int BLINK_INPUT_TOPUP_REQUEST_CODE = 102;

    // parsers are member variables because it will be used for obtaining results
    private AmountParser mTotalAmountParser;
    private AmountParser mTaxParser;
    private IbanParser mIbanParser;
    private RegexParser mVinParser;
    private TopUpParser mTopUpParser;

    /** Reference to bundle is kept, it is used later for loading results from intent */
    private FieldByFieldBundle mFieldByFieldBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called as handler for "custom scan ui integration" button.
     */
    public void advancedIntegration(View v) {
        // advanced integration example is given in CustomFieldByFieldScanActivity source code
        Intent intent = new Intent(this, CustomFieldByFieldScanActivity.class);
        startActivity(intent);
    }

    /**
     * Called as handler for "simple integration" button.
     */
    public void simpleIntegration(View v) {
        /*
         * In this simple example we will use BlinkInput SDK and provided scan activity to scan
         * invoice fields: amount, tax amount and IBAN to which amount has to be paid.
         */

        // parsers are used later for obtaining results
        mTotalAmountParser = new AmountParser();
        mTaxParser = new AmountParser();
        mIbanParser = new IbanParser();

        // we need to scan 3 items, so we will create bundle with 3 elements
        mFieldByFieldBundle = new FieldByFieldBundle(
                // each scan element contains two string resource IDs: string shown in title bar
                // and string shown in text field above scan box. Besides that, it contains parser
                // that will extract data from the OCR result.
                new FieldByFieldElement(R.string.amount_title, R.string.amount_msg, mTotalAmountParser),
                new FieldByFieldElement(R.string.tax_title, R.string.tax_msg, mTaxParser),
                new FieldByFieldElement(R.string.iban_title, R.string.iban_msg, mIbanParser)
        );

        // we use FieldByFieldUISettings - settings for FieldByFieldScanActivity
        FieldByFieldUISettings scanActivitySettings = new FieldByFieldUISettings(mFieldByFieldBundle);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity
        scanActivitySettings.setHelpIntent(new Intent(this, HelpActivity.class));

        // this helper method should be used for starting the provided activities with prepared
        // scan settings
        ActivityRunner.startActivityForResult(this, BLINK_INPUT_REQUEST_CODE, scanActivitySettings);
    }

    /**
     * Called as handler for "regex example" button
     */
    public void regexExample(View v) {
        /*
         * In this example we will use default FieldByFieldScanActivity to drive the recognition,
         * but here we will show how to setup a Regex parser. Regex parser allows configuring
         * custom regular expression which should be extracted from OCR result.
         *
         * In this example we will show how to setup Regex parser to scan Vehicle Identification Numbers
         * (VINs) also known as Chassis numbers of a car. The VIN is 17-character string consisting
         * of digits and uppercase letters.
         */

        // same as in simple integration example, we will use FieldByFieldUISettings

        // now let's setup OCR engine parameters for scanning VIN:
        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        // only uppercase chars and digits are allowed. Don't waste time on classifying other characters as we
        // do not need them.
        engineOptions.addAllDigitsToWhitelist(OcrFont.OCR_FONT_ANY).addUppercaseCharsToWhitelist(OcrFont.OCR_FONT_ANY);
        // do not bother with text lines that are smaller than 40 pixels
        engineOptions.setMinimumLineHeight(40);
        // we expect the VIN to be black text, so we can drop all colors from image - this will give better accuracy
        // because coloured text will be automatically discarded.
        engineOptions.setColorDropoutEnabled(true);

        // now let's create a RegexParser
        mVinParser = new RegexParser("[A-Z0-9]{17}", engineOptions);

        // same as in simple integration, create a scan element bundle
        mFieldByFieldBundle = new FieldByFieldBundle(
                new FieldByFieldElement(R.string.vin_title, R.string.vin_msg, mVinParser)
        );

        // we use FieldByFieldUISettings - settings for FieldByFieldScanActivity
        FieldByFieldUISettings scanActivitySettings = new FieldByFieldUISettings(mFieldByFieldBundle);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity
        scanActivitySettings.setHelpIntent(new Intent(this, HelpActivity.class));

        // this helper method should be used for starting the provided activities with prepared
        // scan settings
        ActivityRunner.startActivityForResult(this, BLINK_INPUT_VIN_REQUEST_CODE, scanActivitySettings);
    }

    /**
     * Called as handler for "TopUp example" button
     */
    public void topUpExample(View view) {
        /*
         * In this simple example we will use BlinkInput SDK to create a sample
         * that scans prepaid mobile coupon codes.
         */

        // create top up parser, and enable scanning of codes with prefix "*123*"
        mTopUpParser = new TopUpParser();
        mTopUpParser.setTopUpPreset(TopUpPreset.TOP_UP_PRESET_123);
        // it is possible set expected prefix and USSD length for codes in form *prefixString*USSDCodeLength_digits#
        //  mTopUpParser.setPrefixAndUssdCodeLength("01", 14);

        // allow codes without prefix, but prefix "*123*" will be added, because preset
        // for prefix *123* is used
        mTopUpParser.setAllowNoPrefix(true);

        // we need to scan 1 item, so we will add 1 scan element to bundle
        mFieldByFieldBundle = new FieldByFieldBundle(
                new FieldByFieldElement(R.string.topup_title, R.string.topup_msg, mTopUpParser)
        );

        // we use FieldByFieldUISettings - settings for FieldByFieldScanActivity
        FieldByFieldUISettings scanActivitySettings = new FieldByFieldUISettings(mFieldByFieldBundle);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity
        scanActivitySettings.setHelpIntent(new Intent(this, HelpActivity.class));


        // this helper method should be used for starting the provided activities with prepared
        // scan settings
        ActivityRunner.startActivityForResult(this, BLINK_INPUT_TOPUP_REQUEST_CODE, scanActivitySettings);
    }

    /**
     * This method is called whenever control is returned from scan activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // first we need to check that we have indeed returned from FieldByFieldScanActivity with success
        if (resultCode == FieldByFieldScanActivity.RESULT_OK) {
            // now we can load bundle with scan results, after loading, results will be available
            // in parser instances
            mFieldByFieldBundle.loadFromIntent(data);
            switch (requestCode) {
                case BLINK_INPUT_REQUEST_CODE:
                    // each field is available through its parser instance
                    String totalAmount = mTotalAmountParser.getResult().toString();
                    String taxAmount = mTaxParser.getResult().toString();
                    String iban = mIbanParser.getResult().toString();
                    Toast.makeText(
                            this,
                            String.format("To IBAN: %s we will pay total %s, tax: %s", iban, totalAmount, taxAmount),
                            Toast.LENGTH_LONG
                    ).show();
                    break;
                case BLINK_INPUT_VIN_REQUEST_CODE:
                    String vin = mVinParser.getResult().toString();
                    Toast.makeText(
                            this,
                            "Vehicle identification number is: " + vin,
                            Toast.LENGTH_LONG
                    ).show();
                    break;
                case BLINK_INPUT_TOPUP_REQUEST_CODE:
                    String topUpCode = mTopUpParser.getResult().toString();
                    Toast.makeText(
                            this,
                            "TopUp code is: " + topUpCode,
                            Toast.LENGTH_LONG
                    ).show();
                    break;
            }
        }
    }
}
