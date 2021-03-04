package com.iar.mtohmaz.khutbatimer_20min;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends Activity {

    private static final String FORMAT = "%02d:%02d";
    private String shift = "";
    private String nextShift = "";
    TextView text1;
    TextView text2;
    CountDownTimer timer = null;
    String shiftTimes[];

    int TIME30 = 1800000, TIME20 = 1200000, TIME15=900000, TIME05=300000, TIME01=60000, TIME5sec=5000;
    int COUNTDOWN_TIME_MS = TIME20;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
        text1 = (TextView)findViewById(R.id.textView1);

        text2 = (TextView)findViewById(R.id.textView2);

        setDisplay();

        launchThread();
    }

    private void setDisplay() {
        text2.setText("");
        text1.setText("Khateeb Timer");
    }

    private String[] getKhutbaTimesFromWeb() {

        String TAG = "getKhutbaTimesFromWeb";
        String shiftTimes[] = null;

        try {
            Document doc = Jsoup.connect("http://www.raleighmasjid.org/m").get();
            Elements list = doc.getElementsByClass("time");

            shiftTimes = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                String token = StringUtils.right(list.get(i).text(), 5).trim();
                String temp[] = token.split(":", 2);
                String  tempHour = temp[0],
                        tempMin = temp[1];
                Log.d(TAG, "Shift " + (i+1) + ": " + tempHour + tempMin);
                shiftTimes[i] = convertTo24Hour(Integer.parseInt(tempHour)) + ":" + tempMin;
            }
            Log.i(TAG, "Retrieved " + shiftTimes.length + " shifts from website " + Arrays.toString(shiftTimes));

        } catch (Exception e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }
        return shiftTimes;
    }

    /*
        Provides hardcoded times as backup in case getKhutbaTimesFromWeb() is unable to read the masjid website.
        Adjust the number of shifts accordingly.
     */
    private String[] getExistingKhutbaTimes() {
        final String[] goodTimes = {"11:00", "12:00", "13:00", "14:15"};
        return goodTimes;
    }

    @Override
    protected void onStart() {
        super.onStart();

        /*
            Retrieve Khutbah times from web only when activity is starting rather than constantly
         */
        Thread refreshTimes = new Thread (){
            @Override
            public void run() {
                try{
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date());
                    final boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
                    String times[] = getKhutbaTimesFromWeb();
                    if (times != null) {
                        shiftTimes = times;
                    }
                    else {  // if unable to get times from website, then use stored or hardcoded times
                        shiftTimes = getExistingKhutbaTimes();
                        Log.e("onStart():", "JSoup unable to retrieve times from web, using hardcoded times "+ Arrays.toString(shiftTimes));
                    }
                }
                catch (Exception e){
                    Log.e("onStart():",e.toString());
                }
            }
        };
        refreshTimes.start();

    }

    private void launchThread() {

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(new Date());
                        final boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
                        if (friday) {

                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                            String currentTime = sdf.format(new Date());

                            for (int i = 0; i < shiftTimes.length; i++) {

                                if (shiftTimes[i].equals(currentTime)) {
                                    if (i == 0) {
                                        shift = "First";
                                        nextShift = "Next shift at " + convertTo12Hour(shiftTimes[1]);
                                    }
                                    else if (i == 1) {
                                        shift = "Second";
                                        nextShift = "Next shift at " + convertTo12Hour(shiftTimes[2]);
                                    }
                                    else if (i == 2 ){
                                        shift = "Third";
                                        nextShift = "Next shift at " + convertTo12Hour(shiftTimes[3]);
                                    }
                                    else {
                                        shift = "Fourth";
                                        nextShift = "Khateeb Timer";
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // update TextView here!
                                            startTimer();
                                        }
                                    });
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    //restart app in case of interruption
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            }
        };

        t.start();
    }

    private void startFiveTimer() {
        CountDownTimer fiveTime = new CountDownTimer(300000, 1000) {
            boolean blink = true;
            public void onTick(long millisUntilFinished) {
                if (blink) {
                    text2.setText("Salah!");
                    blink = false;
                }
                else {
                    text2.setText("");
                    blink = true;
                }
                text1.setText(nextShift);
            }

            public void onFinish() {
                text2.setText("");
            }
        }.start();
    }


    private void startTimer() {
        Log.d("Timer:run()", "today is Friday and its time for khutbah shift");
        Log.d("Timer:run()", "TIMER STARTED! Length="+ (COUNTDOWN_TIME_MS/60000) + "mins");

        if (timer == null) {
            text1.setText(shift + " Shift");
            timer = new CountDownTimer(COUNTDOWN_TIME_MS, 1000) {

                boolean blink = true;
                public void onTick(long millisUntilFinished) {

                    // When 5 mins remaining, turn time text red
                    if (millisUntilFinished <= 300000) {
                        text2.setTextColor(0xffff0000);
                    }

                    // When 1 min remaining, blink time text
                    if (millisUntilFinished <= 60000) {
                        if (blink) {
                            text2.setText("" + String.format(FORMAT,
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                            TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                            blink = false;
                        }
                        else {
                            text2.setText("");
                            blink = true;
                        }
                    }
                    else {
                        text2.setText("" + String.format(FORMAT,
                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                    }

                }

                public void onFinish() {
                    timer = null;
                    text2.setTextColor(0xffffffff);
                    startFiveTimer();
                    shift = "";
                }
            }.start();
        }
    }

    private int convertTo24Hour(int time) {
        if (time < 10) {
            return time + 12;
        }
        return time;
    }

    private String convertTo12Hour(String time) {
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
}

