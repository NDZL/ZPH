package com.ndzl.zph;

import static android.content.Context.POWER_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

//adb shell am broadcast -a com.ndzl.DW -c android.intent.category.DEFAULT --es com.symbol.datawedge.data_string spriz  com.ndzl.zph/.IntentsReceiver
public class IntentsReceiver extends BroadcastReceiver{

    void logToSampleDPS(Context context, String _tbw) throws IOException {
        Context ctxDPS = context.createDeviceProtectedStorageContext();
        String _wout = _tbw ;//+" "+ ctxDPS.getFilesDir().getAbsolutePath();
        FileOutputStream fos = ctxDPS.openFileOutput("sampleDPS.txt", Context.MODE_APPEND);
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    void logToSampleCES(Context context, String _tbw) throws IOException {
        FileOutputStream fos = context.openFileOutput("sampleCES.txt", Context.MODE_APPEND);
        String _wout = _tbw;// +" "+ context.getFilesDir().getAbsolutePath();
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    public static PowerManager.WakeLock wakeLock;
    @Override
    public void onReceive(final Context context, Intent intent) {

        //https://developer.android.com/guide/components/broadcast-exceptions
        if (intent != null && intent.getAction().equals("android.intent.action.LOCKED_BOOT_COMPLETED")) {

            //TRY ACCESSING THE DPS WHILE IN DIRECT BOOT
            try {
                String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\n==LOCKED_BOOT_COMPLETED== received! onReceive/DPS context\n";
                logToSampleDPS(context, _tbw);

                //TESTING SSM
                _tbw = "\nSSM data records while in direct boot: "+ ssm_notpersisted_countRecords(context);
                logToSampleDPS(context, _tbw);

                _tbw = "\nSSM file content while in direct boot: "+ ssmQueryFile(context, false);
                logToSampleDPS(context, _tbw);
            } catch (IOException e) {
                Toast.makeText(context.getApplicationContext(), "\nIOException-logToSampleDPS", Toast.LENGTH_LONG).show();
                //throw new RuntimeException(e);
            }


            //TRY ACCESSING THE CES WHILE IN DIRECT BOOT
            try {
                String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\n==LOCKED_BOOT_COMPLETED== received! onReceive/CES context\n";
                logToSampleCES(context, _tbw); //causing fatal exc
            } catch (IOException e) {
                try {
                    logToSampleDPS(context, "\nIOException 111 while logToSampleCES()"); //trying logging in the DPS when a exception occurs - it should be always accessible!
                } catch (IOException ex) {
                    //throw new RuntimeException(ex);
                }
                //throw new RuntimeException(e);
            }

            //starting a FGS after LOCKED_BOOT_COMPLETED receive is a valid exception!
            //After the device reboots and receives the ACTION_BOOT_COMPLETED, ACTION_LOCKED_BOOT_COMPLETED, or ACTION_MY_PACKAGE_REPLACED intent action in a broadcast receiver.
            // Constant Value: "android.intent.action.LOCKED_BOOT_COMPLETED"

            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZPH::WLTag");
            wakeLock.acquire(); //wakeLock.acquire(10*60*1000L /*10 minutes*/);

            Intent fgsi = new Intent(context.getApplicationContext(), BA_FGS.class);
            try {
                context.getApplicationContext().startForegroundService(fgsi);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
/*
            Intent bgsi = new Intent(context, DW_BGS.class);
            context.startService( bgsi );

*/
        }

        //https://developer.android.com/training/articles/direct-boot#notification
        if (intent != null && intent.getAction().equals("android.intent.action.USER_UNLOCKED")) {
            Log.d("com.ndzl.zph", "==RECEIVED USER_UNLOCKED==! 456");

            Context ctxDPS = context.createDeviceProtectedStorageContext();
            try {
                String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\n==USER_UNLOCKED== received!\n";
                logToSampleDPS(context, _tbw);
                logToSampleCES(context, _tbw);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Toast.makeText(context.getApplicationContext(), "USER_UNLOCKED", Toast.LENGTH_LONG).show();
        }

        //BOOT_COMPLETED IS MANIFEST-DECLARED
        if (intent != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("com.ndzl.zph", "==RECEIVED BOOT_COMPLETED==! 927");

            try {
                String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\n==BOOT COMPLETED== received!\n";
                logToSampleDPS(context, _tbw);
                logToSampleCES(context, _tbw);

                _tbw = "\nSSM file content after BOOT_COMPLETED: "+ ssmQueryFile(context, false);
                logToSampleCES(context, _tbw);
            } catch (IOException e) {
                try {
                    logToSampleDPS(context, "\nIOException 222 while logToSampleCES()");
                } catch (IOException ex) {}
            }

            Toast.makeText(context.getApplicationContext(), "BOOT COMPLETED!", Toast.LENGTH_LONG).show();
            //BOOT-COMPLETED INTENT RECEIVED ON TC21-A11 40 SECONDS AFTER UNLOCKING WITH PIN (AND SECURE STARTUP ENABLED), BOTH DPS AND CES AVAILABLE
            //A13, RECEIVED AFTER 40 SECS MORE OR LESS. TOO BUT CES NOT AVAILABLE IN THAT MOMENT
        }

        if (intent != null && intent.getAction().equals("com.ndzl.DW")){
            String barcode_value = intent.getStringExtra("com.symbol.datawedge.data_string");
            String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" - com.ndzl.DW received via BROADCAST intent <"+barcode_value+">";
            try {
                logToSampleCES(context, _tbw);
                //TESTING SSM
                _tbw = "\nSSM data records after DW scan: "+ssm_notpersisted_countRecords(context);
                logToSampleDPS(context, _tbw);

                _tbw = "\nSSM file content after DW scan: "+ ssmQueryFile(context, false);
                logToSampleDPS(context, _tbw);




            } catch (IOException e) {

            }

        }



    }

    //TESTING ZEBRA SSM
    private final String SSM_DATA_AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private final String SSM_FILE_AUTHORITY_FILE = "content://com.zebra.securestoragemanager.securecontentprovider/files/";
    private final String SSM_FILE_RETRIEVE_AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/file/*";
    private final String COLUMN_DATA_NAME = "data_name";
    private final String COLUMN_DATA_VALUE = "data_value";
    private final String COLUMN_DATA_TYPE = "data_type";
    private final String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private final String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private static final String TAG = "com.ndzl.zph";
    String ssm_notpersisted_countRecords(Context context) {
        Uri cpUriQuery = Uri.parse(SSM_DATA_AUTHORITY + "/[" + context.getPackageName() + "]");
        String selection = COLUMN_TARGET_APP_PACKAGE + " = '" + context.getPackageName() + "'" + " AND " + COLUMN_DATA_PERSIST_REQUIRED + " = 'false'" + " AND " + COLUMN_DATA_TYPE + " = '" + "1" + "'";

        int _count=0;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(cpUriQuery, null, selection, null, null);
            _count = cursor.getCount();
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return  _count+"\n SSM Data Uri="+cpUriQuery.toString()+" ";
    }

    private final String signature = "";
    String ssmQueryFile(Context context, boolean isReadFromWorkProfile) {
        Uri uriFile = Uri.parse(SSM_FILE_RETRIEVE_AUTHORITY);  //original - usually works
        //Uri uriFile = Uri.parse(AUTHORITY_FILE);//test for direct boot
        String selection = "target_app_package='com.ndzl.zph'"; //GETS *ALL FILES* FOR THE PACKAGE NO PERSISTENCE FILTER
        Log.i(TAG, "File selection " + selection);
        Log.i(TAG, "File cpUriQuery " + uriFile.toString());

        String res = "N/A";
        Cursor cursor = null;
        try {
            Log.i(TAG, "Before calling query API Time");
            cursor = context.getContentResolver().query(uriFile, null, selection, null, null);
            Log.i(TAG, "After query API called TIme");
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
        }
        try {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                String uriString;
                strBuild.append("FILES FOUND: "+cursor.getCount()+"\n");
                while (!cursor.isAfterLast()) {

                    uriString = cursor.getString(cursor.getColumnIndex("secure_file_uri"));
                    if(isReadFromWorkProfile)
                        uriString = uriString.replace("/0/", "/10/"); //ATTEMPT TO  ACCESS WORK PROFILE SSM FROM MAIN USER => Permission Denial: reading com.zebra.securestoragemanager.SecureFileProvider uri content://com.zebra.securestoragemanager.SecureFileProvider/user_de/data/user_de/10/com.zebra.securestoragemanager/files/com.ndzl.sst_companionapp/enterprise.txt from pid=19235, uid=10216 requires the provider be exported, or grantUriPermission()

                    String fileName = cursor.getString(cursor.getColumnIndex("secure_file_name"));
                    String isDir = cursor.getString(cursor.getColumnIndex("secure_is_dir"));
                    String crc = cursor.getString(cursor.getColumnIndex("secure_file_crc"));
                    strBuild.append("\n");
                    strBuild.append("URI - " + uriString).append("\n").append("FileName - " + fileName).append("\n").append("IS Directory - " + isDir)
                            .append("\n").append("CRC - " + crc).append("\n").append("FileContent - ").append(readFile(context, uriString));
                    Log.i(TAG, "File cursor " + strBuild);
                    strBuild.append("\n ----------------------").append("\n");

                    cursor.moveToNext();
                }
                Log.d(TAG, "Query File: " + strBuild);
                Log.d("Client - Query", "Set test to view =  " + System.currentTimeMillis());
                res =strBuild.toString();
            } else {
                res="No files to query for local package "+context.getPackageName();
            }
        } catch (Exception e) {
            Log.d(TAG, "Files query data error: " + e.getMessage());
            res="EXCP-"+e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }



    private String readFile(Context context, String uriString) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(uriString));
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        Log.d(TAG, "full content = " + sb);
        return sb.toString();
    }

}