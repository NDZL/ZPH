package com.ndzl.zph;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

/*README
https://developer.android.com/training/articles/direct-boot
* TO RUN AND DEBUG IN WORK PROFILE
* - INSTALLING: EDIT ANDROID STUDIO RUN CONFIGURATION AND SET "INSTALL FOR ALL USERS" FLAG
* - DEBUGGING: MANUALLY START THE BADGED-WORK PROFILE APP, THEN ATTACH DEBUGGER FROM ANDROID STUDIO
* - TO APPLY CHANGES / UPDATING THE APP: ISSUE COMMANDLINE adb uninstall com.ndzl.zph, THEN REINSTALL with android studio
* */


public class MainActivity extends AppCompatActivity {

    static private void copy(InputStream in, File dst) throws IOException {
        FileOutputStream out=new FileOutputStream(dst);
        byte[] buf=new byte[1024];
        int len;

        while ((len=in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvOut = findViewById(R.id.tvout);

        //LOGGING
        String devsignature = ("ZPH|com.ndzl.zph|MainActivity/onCreate" );
        new CallerLog().execute( devsignature );

        //REGISTER  RECEIVER
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");

        filter.addAction("com.ndzl.DW");
        filter.addCategory("android.intent.category.DEFAULT");

        registerReceiver(new IntentsReceiver(), filter);

        IntentFilter userUnlockedFilter = new IntentFilter();

        //userUnlockedFilter.addAction("android.intent.action.USER_UNLOCKED");
        //registerReceiver(new UserUnlockedIntentReceiver(), userUnlockedFilter);
        //Log.d("com.ndzl.zph", "==REGISTERING RECEIVER! 000");

        // creating and reading a file in the Device Encrypted Storage
        String fileNameDPS = "sampleDPS.txt";
        Context ctxDPS = createDeviceProtectedStorageContext();
        String pathDPS = ctxDPS.getFilesDir().getAbsolutePath();
        String pathAndFileDPS= pathDPS+"/"+fileNameDPS;

        String dps_fileContent="N/A";

        try {
            FileOutputStream fos = ctxDPS.openFileOutput(fileNameDPS, MODE_APPEND);  //DO NOT use a fullpath, rather just the filename // in /data/user_de/0/com.ndzl.zph/files or /data/user_de/10/com.ndzl.zph/files
            String _tbw = "\n"+DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" MainActivity/OnCreate/DPS Context "+ UUID.randomUUID()+"\n";
            fos.write(_tbw.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (FileNotFoundException e) {
            dps_fileContent = "FNF EXCP:"+e.getMessage();
        } catch (IOException e) {
            dps_fileContent = "IO WRITE EXCP:"+e.getMessage();
        }


        try {
            dps_fileContent = readFile(ctxDPS, fileNameDPS);
        } catch (IOException e) {
            dps_fileContent = "IO READ EXCP:"+e.getMessage();
        }

       // DevicePolicyManager dmp = new DevicePolicyManager();
        // int ses = dmp.getStorageEncryptionStatus();


        //Toast.makeText(getApplicationContext(), ""+ pathAndFileDPS, Toast.LENGTH_LONG).show();

        //--then creating and reading a file in the Credential Encrypted Storage
        String fileNameCES = "sampleCES.txt";
        Context ctxCES = this;
        String pathCES = ctxCES.getFilesDir().getAbsolutePath();
        String pathAndFileCES= pathCES+"/"+fileNameCES;

        String ces_fileContent="N/A";

        try {
            FileOutputStream fos = ctxCES.openFileOutput(fileNameCES, MODE_APPEND);
            String _tbw = "\n"+DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" MainActivity/OnCreate/CES Context "+ UUID.randomUUID()+"\n";
            fos.write(_tbw.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (FileNotFoundException e) {
            ces_fileContent = "FNF EXCP:"+e.getMessage();
        } catch (IOException e) {
            ces_fileContent = "IO WRITE EXCP:"+e.getMessage();
        }




        try {
            ces_fileContent = readFile(ctxCES, fileNameCES);
        } catch (IOException e) {
            ces_fileContent = "IO READ EXCP:"+e.getMessage();
        }



        tvOut.setText("DEVICE PROTECTED STORAGE\nPrinted at "+DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+"\nFILE:\n"+pathAndFileDPS+"\nCONTENT:\n"+dps_fileContent+"\n\nCREDENTIAL ENCRYPTED STORAGE\nFILE:\n"+pathAndFileCES+"\nCONTENT:\n"+ces_fileContent+"\n");


        ///////////////////////////////////////////
        //TESTING A FILE PROVIDER AS A WAYTO SHARE A FILE TO SSM

        try {
            File cachePath = new File(getCacheDir(), ".");
            cachePath.mkdir();
            File newCacheFile = new File(cachePath, "ndzl4ssm.txt");

            FileOutputStream fos = new FileOutputStream(newCacheFile, false);
            String _tbw = "This is the file content from the originating app's cache\n"+DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" MainActivity/OnCreate/DPS Context "+ UUID.randomUUID()+"\n";
            fos.write(_tbw.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Uri cacheFileContentUri = OriginatingAppFileProvider.getUriForFile(this, "com.ndzl.zph.provider", newCacheFile);

            //READING THE FILE JUST CREATED - FOR DEBUGGING PURPOSE
            String cacheContent = readFileIS(newCacheFile);     //reading from File
            String cph= newCacheFile.getAbsolutePath();         //getting path
            String fpquery = fpQueryFile(cacheFileContentUri); //QUERYING FILE METADATA (filename and size) inside the local fileprovider


            getApplicationContext().grantUriPermission("com.ndzl.sst_companionapp", cacheFileContentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareLocalFileProviderToSSM(cacheFileContentUri, "com.ndzl.sst_companionapp/AAA.txt", "com.ndzl.sst_companionapp", "");


            /*
            //FOLLOWING CODE FOR SHARING A FILE FROM THE LOCAL FILEPROVIDER TO A SPECIFIC APP AND LET IT CONSUME IT
            Intent intent = new Intent().setClassName("com.ndzl.sst_companionapp", "com.ndzl.sst_companionapp.MainActivity");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            ClipData clipData = new ClipData(new ClipDescription("Meshes", new String[]{ClipDescription.MIMETYPE_TEXT_URILIST}), new ClipData.Item(cacheFileContentUri));
            intent.setClipData(clipData);
            startActivity(intent); //this works fine - target app can access this fileprovider!
            */





        } catch (FileNotFoundException e) {
            e.getMessage();
        } catch (IOException e) {
            e.getMessage();
        }


        String packageName = this.getPackageName();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if( !pm.isIgnoringBatteryOptimizations(packageName) ){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }

    }


    private final String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private final String AUTHORITY_FILE = "content://com.zebra.securestoragemanager.securecontentprovider/files/";
    private final String RETRIEVE_AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/file/*";
    private final String COLUMN_DATA_NAME = "data_name";
    private final String COLUMN_DATA_VALUE = "data_value";
    private final String COLUMN_DATA_TYPE = "data_type";
    private final String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private final String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private void shareLocalFileProviderToSSM(Uri _local_file_provider_Uri, String _target_path, String _target_package, String _target_sig) {

        String TAG = "com.ndzl.zph";

        getApplicationContext().grantUriPermission("com.zebra.securestoragemanager", _local_file_provider_Uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); // Needed to grant permission for SSM to read the uri

        StringBuilder _sb = new StringBuilder();
        {
            Uri cpUriQuery = Uri.parse(AUTHORITY_FILE + getPackageName());
            Log.i(TAG, "authority  " + cpUriQuery.toString());

            try {
                ContentValues values = new ContentValues();
                String _package_sig = "{\"pkg\":\""+ _target_package +"\",\"sig\":\"" + _target_sig + "\"}";
                String allPackagesSigs = "{\"pkgs_sigs\":["+ _package_sig  + "]}" ;

                values.put(COLUMN_TARGET_APP_PACKAGE, allPackagesSigs);
                values.put(COLUMN_DATA_NAME, String.valueOf(_local_file_provider_Uri)); //URI instead of file path
                //values.put(COLUMN_DATA_TYPE, "3");
                values.put(COLUMN_DATA_VALUE, _target_path);
                values.put(COLUMN_DATA_PERSIST_REQUIRED, "false");

                Uri createdRow = getContentResolver().insert(cpUriQuery, values);
                Log.i(TAG, "SSM Insert httpsUri: " + createdRow.toString());
                //Toast.makeText(this, "File insert success", Toast.LENGTH_SHORT).show();
                _sb.append("Insert httpsUri Result rows: "+createdRow+"\n" );
            } catch (Exception e) {
                Log.e(TAG, "SSM Insert httpsUri - error: " + e.getMessage() + "\n\n");
                _sb.append("SSM Insert httpsUri - error: " + e.getMessage() + "\n\n");
            }

        }
    }

    String fpQueryFile(Uri _uri_to_be_queried) {
        String TAG="QUERY_LOCAL_FILEPROVIDER";
        Uri uriFile = _uri_to_be_queried;
        //String selection = "target_app_package='com.ndzl.zph'"; //GETS *ALL FILES* FOR THE PACKAGE NO PERSISTANCE FILTER


        String res = "N/A";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uriFile, null, null, null, null);
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
        }
        try {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder strBuild = new StringBuilder();
                String uriString;
                strBuild.append("FILES FOUND: "+cursor.getCount()+"\n");
                while (!cursor.isAfterLast()) {
                    /*
                    //for debug purpose: listing cursor's columns
                    for (int i = 0; i<cursor.getColumnCount(); i++) {
                        Log.d(TAG, "column " + i + "=" + cursor.getColumnName(i));
                    }

                    //column 0=_display_name   column 1=_size

                    //RESULT: THE COLUMN NAMES USED BELOW
                    */
                    String fileName = cursor.getString(cursor.getColumnIndex("_display_name"));
                    String fileSize = cursor.getString(cursor.getColumnIndex("_size"));

                    strBuild.append(fileName+" ");
                    strBuild.append(fileSize+"\n");
                    //strBuild.append("\n ----------------------").append("\n");

                    cursor.moveToNext();
                }
                //Log.d(TAG, "Query File: " + strBuild);
                //Log.d("Client - Query", "Set test to view =  " + System.currentTimeMillis());
                res =strBuild.toString();
            } else {
                res="No files to query for local package "+getPackageName();
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
        InputStream inputStream =   context.openFileInput(uriString);
        InputStreamReader isr = new InputStreamReader(inputStream);

        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line+"\n");
        }
        Log.d("com.ndzl.zph", "full content = " + sb);
        return sb.toString();
    }

    private String readFileIS(File inFile) throws IOException {
        InputStream inputStream =   new FileInputStream(inFile);

        InputStreamReader isr = new InputStreamReader(inputStream);

        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line+"\n");
        }
        Log.d("com.ndzl.zph", "full content = " + sb);
        return sb.toString();
    }

}