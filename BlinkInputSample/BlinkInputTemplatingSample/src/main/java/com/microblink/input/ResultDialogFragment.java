package com.microblink.input;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

public class ResultDialogFragment extends DialogFragment {

    private static final String KEY_TEXT_CONTENT = "textContent";

    public static ResultDialogFragment newInstance(@NonNull String textContent) {
        ResultDialogFragment frag = new ResultDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_TEXT_CONTENT, textContent);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String textContent = getArguments().getString(KEY_TEXT_CONTENT);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder
                .setTitle(R.string.result_dialog_title)
                .setMessage(textContent)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create();
    }
}
