package com.example.lottomatic.utility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.lottomatic.EscPos.EscPosCharsetEncoding;
import com.example.lottomatic.EscPos.EscPosPrinter;
import com.example.lottomatic.connection.DeviceConnection;
import com.example.lottomatic.exceptions.EscPosBarcodeException;
import com.example.lottomatic.exceptions.EscPosConnectionException;
import com.example.lottomatic.exceptions.EscPosEncodingException;
import com.example.lottomatic.exceptions.EscPosParserException;
import com.example.lottomatic.R;

import java.lang.ref.WeakReference;

public abstract class AsyncEscPosPrint extends AsyncTask<AsyncEscPosPrinter, Integer, AsyncEscPosPrint.PrinterStatus> {
    public final static int FINISH_SUCCESS = 1;
    public final static int FINISH_NO_PRINTER = 2;
    public final static int FINISH_PRINTER_DISCONNECTED = 3;
    public final static int FINISH_PARSER_ERROR = 4;
    public final static int FINISH_ENCODING_ERROR = 5;
    public final static int FINISH_BARCODE_ERROR = 6;

    protected final static int PROGRESS_CONNECTING = 1;
    protected final static int PROGRESS_CONNECTED = 2;
    protected final static int PROGRESS_PRINTING = 3;
    protected final static int PROGRESS_PRINTED = 4;

    protected AlertDialog dialog;
    protected View dialogView;
    protected WeakReference<Context> weakContext;
    protected OnPrintFinished onPrintFinished;
    protected String charset;


    public AsyncEscPosPrint(Context context) {
        this(context, null);
    }

    public AsyncEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        this.weakContext = new WeakReference<>(context);
        this.onPrintFinished = onPrintFinished;
    }
    protected PrinterStatus doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return new PrinterStatus(null, AsyncEscPosPrint.FINISH_NO_PRINTER);
        }

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        AsyncEscPosPrinter printerData = printersData[0];
        EscPosPrinter printer = null;  // Declare the printer variable here

        try {
            DeviceConnection deviceConnection = printerData.getPrinterConnection();

            if (deviceConnection == null) {
                return new PrinterStatus(null, AsyncEscPosPrint.FINISH_NO_PRINTER);
            }

            printer = new EscPosPrinter(
                    deviceConnection,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine(),
                    new EscPosCharsetEncoding("Windows-1252", 1)
            );

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTING);

            String[] textsToPrint = printerData.getTextsToPrint();

            for (String textToPrint : textsToPrint) {
                printer.printFormattedTextAndCut(textToPrint);
                Thread.sleep(1000); // Simulate some delay between prints
            }

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTED);

            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_SUCCESS);

        } catch (EscPosConnectionException e) {
            e.printStackTrace();
            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED);
        } catch (EscPosParserException e) {
            e.printStackTrace();
            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_PARSER_ERROR);
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_ENCODING_ERROR);
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_BARCODE_ERROR);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new PrinterStatus(printerData, AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED);
        } finally {
            if (printer != null) {
                try {
                    // Add a slight delay before disconnecting to ensure printing operations are fully complete
                    Thread.sleep(2000);
                    printer.disconnectPrinter();
                } catch (Exception e) {
                    e.printStackTrace(); // Generic exception handling
                }
            }
        }
    }


    protected void onPreExecute() {
        if (this.dialog == null) {
            Context context = weakContext.get();

            if (context == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            dialogView = View.inflate(context, R.layout.custom_progressdialog, null);
            builder.setView(dialogView);
            builder.setCancelable(false);

            this.dialog = builder.create();
            if (this.dialog.getWindow() != null) {
                this.dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.custom_dialog_bg));
            }
            this.dialog.show();

            TextView progressMessage = dialogView.findViewById(R.id.progressMessage);
            progressMessage.setText("Connecting to printer...");

        }
    }

    protected void onProgressUpdate(Integer... progress) {
        if (dialog == null) return;

        TextView progressMessage = dialogView.findViewById(R.id.progressMessage);

        switch (progress[0]) {
            case AsyncEscPosPrint.PROGRESS_CONNECTING:
                progressMessage.setText("Connecting printer...");
                break;
            case AsyncEscPosPrint.PROGRESS_CONNECTED:
                progressMessage.setText("Printer is connected...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTING:
                progressMessage.setText("Printer is printing...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTED:
                progressMessage.setText("Printer has finished...");
                break;
        }
    }

    protected void onPostExecute(PrinterStatus result) {
        this.dialog.dismiss();
        this.dialog = null;
        Context context = weakContext.get();

        if (context == null) {
            return;
        }

        switch (result.getPrinterStatus()) {
            case AsyncEscPosPrint.FINISH_SUCCESS:
                Dialog customDialog = new Dialog(context);
                customDialog.setContentView(R.layout.custom_errordialog);
                customDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                customDialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.custom_dialog_bg));
                customDialog.setCancelable(false);

                Button positiveButton = customDialog.findViewById(R.id.positiveButton);
                TextView title = customDialog.findViewById(R.id.dialogTitle);
                TextView description = customDialog.findViewById(R.id.dialogDescription);

                title.setText("Printing result:");
                description.setText("Print was successful.");

                positiveButton.setOnClickListener(buttonView -> customDialog.dismiss());

                customDialog.show();
                break;
            case AsyncEscPosPrint.FINISH_NO_PRINTER:
                showRetryDialog(context, result.getAsyncEscPosPrinter(), "No printer", "The application can't find any printer connected.");
                break;
            case AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED:
                showRetryDialog(context, result.getAsyncEscPosPrinter(), "Broken connection", "Unable to connect the printer.");
                break;
            case AsyncEscPosPrint.FINISH_PARSER_ERROR:
                showRetryDialog(context, result.getAsyncEscPosPrinter(), "Invalid formatted text", "It seems to be an invalid syntax problem.");
                break;
            case AsyncEscPosPrint.FINISH_ENCODING_ERROR:
                showRetryDialog(context, result.getAsyncEscPosPrinter(), "Bad selected encoding", "The selected encoding character returning an error.");
                break;
            case AsyncEscPosPrint.FINISH_BARCODE_ERROR:
                showRetryDialog(context, result.getAsyncEscPosPrinter(), "Invalid barcode", "Data send to be converted to barcode or QR code seems to be invalid.");
                break;
        }
        if (this.onPrintFinished != null) {
            if (result.getPrinterStatus() == AsyncEscPosPrint.FINISH_SUCCESS) {
                this.onPrintFinished.onSuccess(result.getAsyncEscPosPrinter());
            } else {
                this.onPrintFinished.onError(result.getAsyncEscPosPrinter(), result.getPrinterStatus());
            }
        }
    }
    private void showRetryDialog(final Context context, final AsyncEscPosPrinter printerData, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);
    }
    public static class PrinterStatus {
        private AsyncEscPosPrinter asyncEscPosPrinter;
        private int printerStatus;

        public PrinterStatus (AsyncEscPosPrinter asyncEscPosPrinter, int printerStatus) {
            this.asyncEscPosPrinter = asyncEscPosPrinter;
            this.printerStatus = printerStatus;
        }

        public AsyncEscPosPrinter getAsyncEscPosPrinter() {
            return asyncEscPosPrinter;
        }

        public int getPrinterStatus() {
            return printerStatus;
        }
    }

    public static abstract class OnPrintFinished {
        public abstract void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException);
        public abstract void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter);
    }
}
