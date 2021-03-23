package com.microblink.input;

import com.microblink.blinkinput.entities.parsers.amount.AmountParser;
import com.microblink.blinkinput.entities.parsers.config.fieldbyfield.FieldByFieldElement;
import com.microblink.blinkinput.entities.parsers.date.DateParser;
import com.microblink.blinkinput.entities.parsers.email.EmailParser;
import com.microblink.blinkinput.entities.parsers.raw.RawParser;

public class CustomUIElementConfigurator {
    public static FieldByFieldElement[] createFieldByFieldElements() {
        // here we will create scan configuration for Date, E-Mail and Raw text
        // in Raw text parser we will enable Sieve algorithm which will
        // reuse OCR results from multiple video frames to improve quality

        RawParser rawParser = new RawParser();
        rawParser.setUseSieve(true);

        // create amount parser settings for large amounts - amounts without decimal point should be
        // valid and spaces that separate digit groups (thousands) should be allowed
        AmountParser largeAmountParser = new AmountParser();
        largeAmountParser.setAllowMissingDecimals(true);
        largeAmountParser.setAllowSpaceSeparators(true);

        // in most cases, for best results, use amount parser without setting additional options

        return new FieldByFieldElement[] {
                new FieldByFieldElement(R.string.date_title, R.string.date_msg, new DateParser()),
                new FieldByFieldElement(R.string.large_amount_title, R.string.amount_msg, largeAmountParser),
                new FieldByFieldElement(R.string.email_title, R.string.email_msg, new EmailParser()),
                new FieldByFieldElement(R.string.raw_title, R.string.raw_msg, rawParser)
        };
    }
}
