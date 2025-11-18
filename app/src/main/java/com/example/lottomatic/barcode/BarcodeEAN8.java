package com.example.lottomatic.barcode;

import com.example.lottomatic.EscPos.EscPosPrinterCommands;
import com.example.lottomatic.EscPos.EscPosPrinterSize;
import com.example.lottomatic.exceptions.EscPosBarcodeException;

public class BarcodeEAN8 extends BarcodeNumber {
    public BarcodeEAN8(EscPosPrinterSize printerSize, String code, float widthMM, float heightMM, int textPosition) throws EscPosBarcodeException {
        super(printerSize, EscPosPrinterCommands.BARCODE_TYPE_EAN8, code, widthMM, heightMM, textPosition);
    }

    @Override
    public int getCodeLength() {
        return 8;
    }
}
