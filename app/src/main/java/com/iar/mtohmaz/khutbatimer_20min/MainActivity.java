package com.iar.mtohmaz.khutbatimer_20min;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
    private String lastSyncTime = "";
    TextView text1;
    TextView text2;
    TextView text3;
    CountDownTimer timer = null;
    String shiftTimes[];
    String TAG = "KhutbaTimer";

    int TIME30 = 1800000, TIME20 = 1200000, TIME15=900000, TIME10=600000, TIME05=305000, TIME01=60000, TIME5sec=5000;
    final int TIMER_COUNTDOWN_LENGTH = TIME20;
    final String MASJID_KHUTBA_TIMES_URL = "http://www.raleighmasjid.org/m";

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        text1 = (TextView)findViewById(R.id.textView1);

        text2 = (TextView)findViewById(R.id.textView2);
        text3 = (TextView)findViewById(R.id.textView3);

        setDisplay("","Khateeb Timer");

        launchThread();
    }

    private void setDisplay(String txt_timer, String txt_sub) {
        text2.setText(txt_timer);
        text1.setText(txt_sub);
    }

    private String[] getKhutbaTimesFromWeb() {

        TAG = "getKhutbaTimesFromWeb";
        String shiftTimes[] = null;

        try {
            Document doc = Jsoup.connect(MASJID_KHUTBA_TIMES_URL).get();
            Elements list = doc.getElementsByClass("time");

            shiftTimes = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                String token = StringUtils.right(list.get(i).text(), 5).trim();
                String temp[] = token.split(":", 2);
                String  tempHour = "" + temp[0],
                        tempMin = "" + temp[1];
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
                        lastSyncTime = "Khutbah times last retrieved: " + Calendar.getInstance().getTime();
//                        setDisplay("","Next shift Friday at " + convertTo12Hour(shiftTimes[0]));
                        text3.setText(lastSyncTime);
                    }
                    else {  // if unable to get times from website, then use stored or hardcoded times
                        shiftTimes = getExistingKhutbaTimes();
                        text3.setText("IAR site not reachable, using backup times");
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
                } catch (Exception e) {
                    Log.e("launchThread","Ran into unknown exception: "+ e.toString());
                }
            }
        };

        t.start();
    }

    private void startSalahWarningTimer() {
        Log.d("post-Timer", "TIMER COUNTDOWN EXPIRED!  Displaying Salah! for " + shift + " Shift");
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
                text3.setText(lastSyncTime);
            }
        }.start();
    }


    private void startTimer() {
        Log.d("startTimer", "TIMER COUNTDOWN STARTED! " + shift + " Shift, Mins="+ (TIMER_COUNTDOWN_LENGTH/60000));

        if (timer == null) {
            text1.setText(shift + " Shift");
            text3.setText("");
            timer = new CountDownTimer(TIMER_COUNTDOWN_LENGTH, 1000) {

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
                    startSalahWarningTimer();
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

