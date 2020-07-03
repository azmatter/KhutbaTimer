package com.iar.mtohmaz.khutbatimer_15min;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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

        String shiftTimes[] = new String[3];
        try {
            Document doc = Jsoup.connect("http://www.raleighmasjid.org").get();
            Elements list = doc.getElementsByClass("time");
            int firstHour = Integer.parseInt(list.get(0).text().substring(7, 9).replaceAll(":", ""));
            firstHour = convertTo24Hour(firstHour);
            String firstMinute = list.get(0).text().substring(9, 12).replaceAll(":", "");
            int secondHour = Integer.parseInt(list.get(1).text().substring(7, 9).replaceAll(":", ""));
            secondHour = convertTo24Hour(secondHour);
            String secondMinute = list.get(1).text().substring(9, 12).replaceAll("\\s+","");
            int thirdHour = Integer.parseInt(list.get(2).text().substring(7, 9).replaceAll(":", ""));
            thirdHour = convertTo24Hour(thirdHour);
            String thirdMinute = list.get(2).text().substring(9, 12).replaceAll("\\s+", "");
            shiftTimes[0] = firstHour + ":" + firstMinute;
            shiftTimes[1] = secondHour + ":" + secondMinute;
            shiftTimes[2] = thirdHour + ":" + thirdMinute;
        } catch (Exception e) {
            shiftTimes = null;
            e.printStackTrace();
        }
        return shiftTimes;
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
                                    else {
                                        shift = "Third";
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

        if (timer == null) {
            text1.setText(shift + " Shift");
            timer = new CountDownTimer(900000, 1000) {

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

