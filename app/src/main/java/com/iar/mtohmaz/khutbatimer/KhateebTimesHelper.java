package com.iar.mtohmaz.khutbatimer;

import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;
import android.app.Activity;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class KhateebTimesHelper {

    String TAG = "KhutbaTimer";
    Activity MainActivity;

    public static final int TIME30 = 1800000,   // 30 minutes (in ms)
            TIME20 = 1200000,   // 20 minutes
            TIME15=900000,      // 15 minutes
            TIME10=600000,      // 10 minutes
            TIME05=305000,      // 5 minutes
            TIME01=60000,       // 1 minute
            TIME5sec=5000;      // 5 seconds
    private int TIMER_COUNTDOWN_LENGTH;
    private String lastSyncTime = "";
    public final String MASJID_KHUTBA_TIMES_URL = "http://www.raleighmasjid.org/m";
    public final String MASJID_KHUTBA_TIMES_API_URL = "https://raleighmasjid.org/API/fridayPrayer";


    public KhateebTimesHelper (Activity context){
        MainActivity = context;
    }

    /**
     * Method for parsing times from JSON API.
     * Assues JSON response format:  {"fridayShifts":["11:00","12:00","13:00","15:00"]}
     * @return string array of times, each formatted as "HH:MM"
     */
    public String[] parseKhutbaTimesFromAPI(){
        TAG = "parseKhutbaTimesFromAPI";
        String [] goodTimes = null;
        String data = null;

        try {
            // get full JSON response body
            data = Jsoup.connect(MASJID_KHUTBA_TIMES_API_URL).ignoreContentType(true).execute().body();
            Log.d(TAG, "Data : " + data);

            // extract all shift times and separate individual shifts
            JSONObject object = new JSONObject(data);
            String timeString = object.getString("fridayShifts");
            JSONArray jsonArray = (JSONArray) object.get("fridayShifts");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                builder.append(jsonArray.getString(i)).append(",");
            }
            timeString = builder.toString();
            Log.d(TAG, "transformed timeString " + timeString);

            goodTimes = timeString.split(",");
            for (int i = 0; i < goodTimes.length; i++) {
                Log.i(TAG, "got Shift " + (i + 1) + ": " + goodTimes[i]);
            }
        } catch (IOException e){
            Log.e(TAG, "Caught IOException: " + e.toString());
            return null;
        } catch (Exception e){
            Log.e(TAG,"Caught Exception: " + e.toString());
            return null;
        }
        return goodTimes;
    }

    /**
     * Method for parsing times from website.
     * Assuming this is the time format being retrieved from the website:  "Khutba 11:00 AM"
     * @return string array of times, each formatted as "HH:MM"
     */
    public String[] parseKhutbaTimesFromWeb() {

        TAG = "parseKhutbaTimesFromWeb";
        String shiftTimes[] = null;
        boolean parserError = false;

        try {
            Document doc = Jsoup.connect(MASJID_KHUTBA_TIMES_URL).get();
            Elements list = doc.getElementsByClass("time");

            shiftTimes = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                // see javadoc above for expected time formatting. If the website changes the expected format, this will fail.
                String token = StringUtils.right(list.get(i).text(), 8).trim();
                String temp[] = token.split(":|\\s+", 3);

                String  tempHour = "" + temp[0],
                        tempMin = "" + temp[1];
                Log.d(TAG, "Shift " + (i+1) + ": " + tempHour + ":" + tempMin);
                shiftTimes[i] = convertTo24Hour(Integer.parseInt(tempHour)) + ":" + tempMin;
                if(shiftTimes[i].equals(null)){
                    parserError = true;
                    break;
                }
            }
            Log.i(TAG, "Retrieved " + shiftTimes.length + " shifts from website " + Arrays.toString(shiftTimes));
            if(parserError){
                Log.e(TAG,"Parsing failed, check times format on website");
            }

        } catch (Exception e) {
            Log.e(TAG,e.toString());
            return null;
        }

        return shiftTimes;
    }

    public int convertTo24Hour(int time) {
        if (time < 10) {
            return time + 12;
        }
        return time;
    }

    public String convertTo12Hour(String time) {
        String ret = "";
        try {
            String _24HourTime = time;
            SimpleDateFormat _24HourSDF = new SimpleDateFormat("HH:mm");
            SimpleDateFormat _12HourSDF = new SimpleDateFormat("hh:mm a");
            Date _24HourDt = _24HourSDF.parse(_24HourTime);
            ret = _12HourSDF.format(_24HourDt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String getLastSyncTime(){
        TAG = "getLastSyncTime";
        SharedPreferences sharedPreferences = MainActivity.getPreferences(Context.MODE_PRIVATE);
        String ret = sharedPreferences.getString("lastSyncTime","");
        Log.i(TAG,"stored lastSyncTime="+ret);
        return ret;
    }

    // This is where the default timer countdown length will be initialized.
    public int getTIMER_COUNTDOWN_LENGTH(){
        TAG = "getTimerLength";
        SharedPreferences sharedPreferences = MainActivity.getPreferences(Context.MODE_PRIVATE);
        int ret = sharedPreferences.getInt("countdownLength",TIME20);
        Log.i(TAG,"countdownLength setting (mins): " + ret/60000);
        return ret;
    }

    public void setNewTIMER_COUNTDOWN_LENGTH(int newMins){
        TAG = "setNewTimerLength";
        int newTime = newMins*60000;
        Log.i(TAG, "Updating COUNTDOWN TIMER LENGTH\noldLength=" + this.TIMER_COUNTDOWN_LENGTH/60000 + ", newLength="+newMins);

        // update instance variable
        this.TIMER_COUNTDOWN_LENGTH = newTime;

        // update shared prefs
        SharedPreferences sharedPreferences = MainActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sp = sharedPreferences.edit();
        sp.putInt("countdownLength",newTime);
        sp.apply();
    }

    public void storeTimesLocally(String [] times, String lastSyncTime){
        TAG = "storeTimesLocally";
        SharedPreferences sharedPreferences = MainActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sp = sharedPreferences.edit();
        StringBuilder sb = new StringBuilder();

        // Store the # shifts
        sp.putInt("numShifts",times.length);

        // store the shift times as one string
        for(int i =0;i<times.length;i++){
            sb.append(times[i]).append(",");
        }
        sp.putString("shiftTimes",sb.toString());
        Log.d(TAG,"putString: "+ sb.toString());

        // store the lastSyncTime
        sp.putString("lastSyncTime",lastSyncTime);

        boolean tf = sp.commit();
        if(tf)
            Log.i(TAG,"Successfull wrote times to sharedPreferences");
        else Log.e(TAG, "Error while writing times to sharedPreferences");

    }

    /*
        Provides stored times as backup in case getKhutbaTimesFromWeb() is unable to read the masjid website.
     */
    public String[] getStoredTimes() {
        TAG = "getStoredTimes";
        String [] goodTimes;
        int numShifts = 0;
        String str="";

        try {
            SharedPreferences sharedPreferences = MainActivity.getPreferences(Context.MODE_PRIVATE);
            numShifts = sharedPreferences.getInt("numShifts", 0);
            str = sharedPreferences.getString("shiftTimes", null);
        }
        catch (Exception e)
        {
            Log.e(TAG,e.toString());
        }
        Log.i(TAG,"Received numShifts: " + numShifts +"\nstoredTimes: " + str);

        if(numShifts==0 || str ==null) {
            Log.e(TAG,"Nothing stored in sharedPreferences");
            return null;
        }
        goodTimes = str.split(",");
        Log.i(TAG,"Parsed times: " + Arrays.toString(goodTimes));
        return goodTimes;
    }

}
