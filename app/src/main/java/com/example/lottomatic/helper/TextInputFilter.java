package com.example.lottomatic.helper;

import android.text.InputFilter;
import android.text.Spanned;

public class TextInputFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        // Don't filter if replacing existing text
        if (dend - dstart > 0) {
            return null;
        }

        // Don't allow typing if we already have 5 characters
        if (dest.length() >= 5) {
            return "";
        }

        // Automatically insert asterisk after 2 characters
        if (dest.length() == 2 && dstart == 2) {
            return "*" + source;
        }
        return null;
    }
}
