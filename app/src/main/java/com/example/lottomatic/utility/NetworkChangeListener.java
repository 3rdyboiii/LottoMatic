package com.example.lottomatic.utility;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.example.lottomatic.LoginActivity;
import com.example.lottomatic.R;
import com.example.lottomatic.utility.Common;

public class NetworkChangeListener extends BroadcastReceiver {
    private Handler handler;
    private Runnable checkInternetRunnable;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Common.isConnectedToInternet(context)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View layout_dialog = LayoutInflater.from(context).inflate(R.layout.check_internet_dialog, null);
            builder.setView(layout_dialog);

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);

            handler = new Handler();
            checkInternetRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Common.isConnectedToInternet(context)) {
                        // Dismiss the dialog if internet is connected
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        // Stop checking
                        handler.removeCallbacks(this);
                        // Navigate to LoginActivity
                        Intent loginIntent = new Intent(context, LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(loginIntent);
                    } else {
                        // Continue checking
                        handler.postDelayed(this, 1000); // Check every 1 second
                    }
                }
            };
            // Start periodic checking
            handler.post(checkInternetRunnable);
        }
    }
}
