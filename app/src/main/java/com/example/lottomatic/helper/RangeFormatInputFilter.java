package com.example.lottomatic.helper;

import android.text.InputFilter;
import android.text.Spanned;

public class RangeFormatInputFilter implements InputFilter {

    private final int min;
    private final int max;

    public RangeFormatInputFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        try {
            String newVal = dest.toString().substring(0, dstart)
                    + source.toString().substring(start, end)
                    + dest.toString().substring(dend);

            if (newVal.isEmpty()) return null;

            int value = Integer.parseInt(newVal);

            if (value >= min && value <= max) return null;

        } catch (NumberFormatException ignored) {}

        return "";
    }
}


