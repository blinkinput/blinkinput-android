package com.microblink.input.util;

import com.microblink.blinkinput.entities.recognizers.Recognizer;

public class ResultFormater {

    public static String stringifyRecognitionResults(Recognizer<?>[] recognizers) {
        StringBuilder sb = new StringBuilder();
        if (recognizers == null) {
            return "";
        }
        for (Recognizer<?> rec : recognizers) {
            if (rec.getResult().getResultState() != Recognizer.Result.State.Empty) {
                sb.append(rec.getResult().getClass().getSimpleName());
                sb.append(":\n");
                sb.append(rec.getResult().toString());
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }
}
