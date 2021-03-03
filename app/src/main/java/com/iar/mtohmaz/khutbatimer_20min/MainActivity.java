package com.iar.mtohmaz.khutbatimer_20min;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    private String[] getKhutbaTimes() {
        String TAG = "getKhutbaTimes()";

        String shiftTimes[] = new String[4];
        try {
            Document doc = Jsoup.connect("http://www.raleighmasjid.org").get();
            if(doc == null)
                Log.e(TAG,"Unable to reach website");
            Elements list = doc.getElementsByClass("time");
            // Shift 1
            String Time1 = StringUtils.right(list.get(0).text(),5).trim();
            int firstHour = Integer.parseInt(Time1.substring(0,2).replaceAll(":",""));
            firstHour = convertTo24Hour(firstHour);
            String firstMinute = StringUtils.right(Time1,2);
            Log.d(TAG,"1st Shift: " + firstHour + firstMinute);

            // Shift 2
            String Time2 = StringUtils.right(list.get(1).text(),5).trim();
            int secondHour = Integer.parseInt(Time2.substring(0,2).replaceAll(":",""));
            secondHour = convertTo24Hour(secondHour);
            String secondMinute = StringUtils.right(Time2,2);
            Log.d(TAG,"2nd Shift: " + secondHour + secondMinute);

            // Shift 3
            String Time3 = StringUtils.right(list.get(2).text(),5).trim();
            int thirdHour = Integer.parseInt(Time3.substring(0,1).replaceAll(":",""));
            thirdHour = convertTo24Hour(thirdHour);
            String thirdMinute = StringUtils.right(Time3,2);
            Log.d(TAG,"3rd Shift: " + thirdHour + thirdMinute);


            // Shift 4
            String Time4 = StringUtils.right(list.get(3).text(),5).trim();
            int fourthHour = Integer.parseInt(Time4.substring(0,2).replaceAll(":",""));
            fourthHour = convertTo24Hour(fourthHour);
            String fourthMinute = StringUtils.right(Time4,2);
            Log.d(TAG,"4th Shift: " + fourthHour + fourthMinute);

            shiftTimes[0] = firstHour + ":" + firstMinute;
            shiftTimes[1] = secondHour + ":" + secondMinute;
            shiftTimes[2] = thirdHour + ":" + thirdMinute;
            shiftTimes[3] = fourthHour + ":" + fourthMinute;

        } catch (Exception e) {
            shiftTimes = null;
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }
        return shiftTimes;
    }

    /*
        Provides hardcoded times as backup in case getKhutbaTimes() is unable to read the IAR website.
        Adjust the number of shifts accordingly.
     */
    private String[] getHardcodedKhutbaTimes() {
        String goodTimes[] = new String[4];
        goodTimes[0] = "11:00";
        goodTimes[1] = "12:00";
        goodTimes[2] = "13:00";
        goodTimes[3] = "15:15";
        return goodTimes;
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

                            String times[] = getKhutbaTimes();

                            if (times != null) {
                                shiftTimes = times;
                            }
                            else {  // need an else case.. if unable to get times for website, then use hardcoded times
                                shiftTimes = getHardcodedKhutbaTimes();
                                Log.e("MainActivity:run()", "JSoup unable to retrieve times, using hardcoded times");
                            }

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
/*
    30 mins = 1800000 ms
    20 mins = 1200000 ms
    15 mins = 900000 ms
 */

        if (timer == null) {
            text1.setText(shift + " Shift");
            timer = new CountDownTimer(1200000, 1000) {

                boolean blink = true;
                public void onTick(long millisUntilFinished) {
                    if (millisUntilFinished <= 300000) {
                        text2.setTextColor(0xffff0000);
                    }

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

