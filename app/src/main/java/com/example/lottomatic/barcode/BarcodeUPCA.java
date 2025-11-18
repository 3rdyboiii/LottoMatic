package com.example.lottomatic.barcode;

import com.example.lottomatic.EscPos.EscPosPrinterCommands;
import com.example.lottomatic.EscPos.EscPosPrinterSize;
import com.example.lottomatic.exceptions.EscPosBarcodeException;

public class BarcodeUPCA extends BarcodeNumber {

    public BarcodeUPCA(EscPosPrinterSize printerSize, String code, float widthMM, float heightMM, int textPosition) throws EscPosBarcodeException {
        super(printerSize, EscPosPrinterCommands.BARCODE_TYPE_UPCA, code, widthMM, heightMM, textPosition);
    }

    @Override
    public int getCodeLength() {
        return 12;
    }
}
