package com.example.lottomatic.textparser;

import com.example.lottomatic.EscPos.EscPosPrinterCommands;
import com.example.lottomatic.exceptions.EscPosConnectionException;
import com.example.lottomatic.exceptions.EscPosEncodingException;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}
