package com.ndzl.zph;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BA_FGS extends Service { //BOOT-AWARE FGS
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    String TAG = "com.ndzl.zph/BA_FGS";

    public void showToast(String message) {
        final String msg = message;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String reverseString(String s){
        return new StringBuilder(s).reverse().toString();  //codeium!!
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificaton Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    void logToSampleDPS( String _tbw) throws IOException {

        Context ctxDPS = getApplicationContext().createDeviceProtectedStorageContext();
        String _wout = _tbw ;//+" "+ ctxDPS.getFilesDir().getAbsolutePath();
        FileOutputStream fos = ctxDPS.openFileOutput("sampleDPS.txt", Context.MODE_APPEND);
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    boolean isCESAvailable(){

        try {
            FileOutputStream fos = getApplicationContext().openFileOutput("probeCES.txt", Context.MODE_PRIVATE);
            //String _wout = "CES PROBED";
            String _wout = "-";
            fos.write(_wout.getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String lengthFileIS(File inFile){
        long flen =-1;

        try {
            InputStream inputStream =   new FileInputStream(inFile);

            flen = ((FileInputStream)inputStream).getChannel().size();

            Log.d("com.ndzl.zph", "file length  = " + flen);

            inputStream.close();
        } catch (IOException e) {
            Log.e("com.ndzl.zph", "no file available");
        }

        return ""+flen;
    }

    private void stressTestReadDPS(){
        String fileNameDPS = "stressDPS.txt";
        Context ctxDPS = createDeviceProtectedStorageContext();
        String pathDPS = ctxDPS.getFilesDir().getAbsolutePath();
        String pathAndFileDPS= pathDPS+"/"+fileNameDPS;

        String dps_fileContent="N/A";


        Log.i("com.ndzl.zph", "large file path " + pathAndFileDPS );

        lengthFileIS( new File(pathAndFileDPS)); //look at the logcat for file length


    }

    private String readDPS(String fileNameDPS){
        Context ctxDPS = createDeviceProtectedStorageContext();
        String pathDPS = ctxDPS.getFilesDir().getAbsolutePath();
        String pathAndFileDPS= pathDPS+"/"+fileNameDPS;

        File inFile = new File(pathAndFileDPS);
        StringBuilder sb = new StringBuilder();
        try {
            InputStreamReader isr = new InputStreamReader( new FileInputStream(inFile) );
            BufferedReader bufferedReader = new BufferedReader(isr);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            bufferedReader.close();

            Log.d("com.ndzl.zph", "file context  = " + sb);

        } catch (IOException e) {
            Log.e("readDPS", "no file available");
        }
        return sb.toString();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        try {logToSampleDPS("\nBA_FGS created!");} catch (IOException e) {}

        //OEMINFO INIT
        new OEMInfoManager(getApplicationContext());

        //SSM
        Context ssmContext = getApplicationContext().createDeviceProtectedStorageContext();
        String _tbw = "\nSSM file content in FGS run after LOCKED_BOOT_COMPLETED: "+ ssmQueryFile(ssmContext, false);
        try {logToSampleDPS(_tbw);} catch (IOException e) {}

        //DW INTENT RECEIVER? TESTING! -== ENSURE THESE ACTION/CATEGORY ARE DECLARED IN THE MANIFEST.XML ==-
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ndzl.DW");
        filter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(new IntentsReceiver(), filter);

        //String devsignature = ("ZPHhat=com.ndzl.zph&any=BA_FSG/onCreate" );
        //new CallerLog().execute( devsignature );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand");
        String _android_id = "A_ID="+ Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        //check if a powerlock is held
        String _is_WL_held = IntentsReceiver.wakeLock.isHeld() ? "Y":"N";
        //check battery optimization
        String packageName = this.getPackageName();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        String _is_battery_optimized = pm.isIgnoringBatteryOptimizations(packageName)? "N":"Y";

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE );
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ZPH - PHONE HOME AGENT {N.DZL}")
                .setContentText(_android_id+"\nWL="+_is_WL_held+"\nBattOpt="+_is_battery_optimized)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        //ZEBRA CALLING HOME
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    go();
                    populateAppsInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 10000, 30000);


        return super.onStartCommand(intent, flags, startId);
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private final String AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/data";
    private final String AUTHORITY_FILE = "content://com.zebra.securestoragemanager.securecontentprovider/files/";
    private final String RETRIEVE_AUTHORITY = "content://com.zebra.securestoragemanager.securecontentprovider/file/*";
    private final String COLUMN_DATA_NAME = "data_name";
    private final String COLUMN_DATA_VALUE = "data_value";
    private final String COLUMN_DATA_TYPE = "data_type";
    private final String COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required";
    private final String COLUMN_TARGET_APP_PACKAGE = "target_app_package";
    private final String signature = "";
    String ssmQueryFile(Context context, boolean isReadFromWorkProfile) {
        Uri uriFile = Uri.parse(RETRIEVE_AUTHORITY);  //original - usually works
        //Uri uriFile = Uri.parse(AUTHORITY_FILE);//test for direct boot
        String selection = "target_app_package='com.ndzl.zph'"; //GETS *ALL FILES* FOR THE PACKAGE NO PERSISTANCE FILTER
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


    void go(){
        getPackagesDetails();

        String _android_id = "A_ID="+ Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        String _sb_who = "["+ Build.MANUFACTURER+","+ Build.MODEL+","+Build.DISPLAY+","+ _android_id +"]";
        String _sb_any_clean = ".";
        String _sb_when = "";
        String pckg = getApplicationContext().getPackageName();

        String sn="-n/a";
        String temperature = "-N/A";
        String BT_ADDR ="-N/A";
        String WIFI_MACADDR ="-N/A";
        String imei ="-N/A";
        String isces=isCESAvailable() ? "CES=Y" : "CES=N";
        String _is_WL_held = IntentsReceiver.wakeLock.isHeld() ? "WL=Y":"WL=N";
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        String _is_battery_optimized = pm.isIgnoringBatteryOptimizations(pckg)? "BATT_OPT=N":"BATT_OPT=Y";

        BatteryManager bm = (BatteryManager) this.getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String _batLevel = "BATT="+batLevel+"%";
        String _serial_no= "S/N="+getDeviceSerialNumber();

        _sb_any_clean = isces+","+_is_WL_held+","+_is_battery_optimized+","+_batLevel+","+_serial_no+","+getInstalledAppsInfo();//sn+temperature+BT_ADDR+WIFI_MACADDR;
        String devsignature = (_sb_who+"|"+pckg+"|"+_sb_any_clean );
        //new CallerLog().execute( devsignature );
        DataWedgeWrapper.registerScanReceiver
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
        bufferedReader.close();
        Log.d(TAG, "full content = " + sb);
        return sb.toString();
    }

    long appInfoLastSentTime = 0;
    private String getInstalledAppsInfo(){
        return  readDPS( "appsInfo.txt" );
    }

    private void populateAppsInfo() {
        if(isCESAvailable()){
            try {
                saveDataToDPS("appsInfo.txt", getPackagesDetails());
            } catch (IOException e) {
                Log.e(TAG, "populateAppsInfo/EXCP "+e.getMessage());
            }
        }
    }
    private String getPackagesDetails() {
        StringBuilder sb = new StringBuilder();
        PackageManager pm = this.getPackageManager();
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        apps.forEach(ai -> {
            if( pm.getLaunchIntentForPackage(ai.packageName) != null ) {
                sb.append("|"+ai.packageName+";");
                try {
                    sb.append(pm.getPackageInfo( ai.packageName, 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "getPackagesDetails/EXCP "+e.getMessage());
                }
                sb.append("|\r\n ");
            }
        } );

        return sb.toString();
    }

    void saveDataToDPS(String file, String _tbw)  throws IOException {
        Context ctxDPS = getApplicationContext().createDeviceProtectedStorageContext();
        String _wout = _tbw ;
        FileOutputStream fos = ctxDPS.openFileOutput(file, Context.MODE_PRIVATE);
        fos.write(_wout.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    String getDeviceSerialNumber(){
        return  readDPS( "uniqueIDs.txt" );
    }

/*
    private void stressTestDPS(){
        String fileNameDPS = "sampleDPS.txt";
        Context ctxDPS = createDeviceProtectedStorageContext();
        String pathDPS = ctxDPS.getFilesDir().getAbsolutePath();
        String pathAndFileDPS= pathDPS+"/"+fileNameDPS;

        String dps_fileContent="N/A";

        try {
            FileOutputStream fos = ctxDPS.openFileOutput(fileNameDPS, MODE_APPEND);  //DO NOT use a fullpath, rather just the filename // in /data/user_de/0/com.ndzl.zph/files or /data/user_de/10/com.ndzl.zph/files
            String _tbw = "\n"+ DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis()))+" MainActivity/OnCreate/DPS Context "+ UUID.randomUUID()+"\n";
            fos.write(_tbw.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (FileNotFoundException e) {
            dps_fileContent = "FNF EXCP:"+e.getMessage();
        } catch (IOException e) {
            dps_fileContent = "IO WRITE EXCP:"+e.getMessage();
        }

    }
*/

}