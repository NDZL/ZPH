package com.ndzl.zph;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

public class UserUnlockedIntentReceiver extends BroadcastReceiver  {

    void logToSampleDPS(Context context, String _tbw) throws IOException {
        Context ctxDPS = context.createDeviceProtectedStorageContext();
        String _wout = _tbw +" "+ ctxDPS.getFilesDir().getAbsolutePath();
        FileOutputStream fos = ctxDPS.openFileOutput("sampleDPS.txt", Context.MODE_APPEND);
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    void logToSampleCES(Context context, String _tbw) throws IOException {
        FileOutputStream fos = context.openFileOutput("sampleCES.txt", Context.MODE_APPEND);
        String _wout = _tbw +" "+ context.getFilesDir().getAbsolutePath();
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.d("com.ndzl.zph", "==RECEIVED USER_UNLOCKED==! 123");
            Context ctxDPS = context.createDeviceProtectedStorageContext();
            try {
                FileOutputStream fos = ctxDPS.openFileOutput("sampleDPS.txt", Context.MODE_APPEND);
                String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\n==USER_UNLOCKED== received!\n";
                fos.write(_tbw.getBytes(StandardCharsets.UTF_8));
                fos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Toast.makeText(context.getApplicationContext(), "USER_UNLOCKED", Toast.LENGTH_LONG).show();

    }
}