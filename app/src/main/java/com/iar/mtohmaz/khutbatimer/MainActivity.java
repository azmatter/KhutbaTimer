package com.iar.mtohmaz.khutbatimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private static final String FORMAT = "%02d:%02d";
    private String shift = "";
    private String nextShift = "";
    private String statusMsg = "";
    TextView text1;
    TextView text2;
    TextView text3;
    Button menuButton;
    PopupMenu popupMenu;
    CountDownTimer timer = null;

    String TAG = "KhutbaTimer";
    String shiftTimes[];
    KhateebTimesHelper KTHelper;
    private int TIMER_COUNTDOWN_LENGTH;
    private String lastSyncTime = "";
    final boolean testing = false;

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
        menuButton = (Button)findViewById(R.id.menuButton);
        menuButton.setBackgroundColor(Color.TRANSPARENT);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                popupMenu = new PopupMenu(MainActivity.this, v);
                popupMenu.setOnMenuItemClickListener(MainActivity.this);
                popupMenu.inflate(R.menu.app_menu);
                popupMenu.show();
            }
        });

        setDisplay("","Khateeb Timer");

        //Initialize instance variables
        KTHelper = new KhateebTimesHelper(this);
        TIMER_COUNTDOWN_LENGTH = KTHelper.getTIMER_COUNTDOWN_LENGTH();
        lastSyncTime = KTHelper.getLastSyncTime();
        refreshKhutbahTimes();

    }

    private void setDisplay(String txt_timer, String txt_sub) {
        text2.setText(txt_timer);
        text1.setText(txt_sub);
    }

    public void refreshKhutbahTimes(){
        TAG = "refreshTimes";

        Thread refreshTimes = new Thread (){
            @Override
            public void run() {

                try{
                    // Step 1 - try the API first
                    String [] times = KTHelper.parseKhutbaTimesFromAPI();

                    // Step 2 - if the API fails for some reason, try to scrap the website
                    if (times == null){
                        times = KTHelper.parseKhutbaTimesFromWeb();
                    }

                    // if Step 1 or 2 succeeded, then we're good. Store the times in sharedPrefs
                    if (times != null) {
                        shiftTimes = times;
                        lastSyncTime = "" + Calendar.getInstance().getTime();
                        statusMsg = "";
                        KTHelper.storeTimesLocally(shiftTimes,lastSyncTime);
                    }

                    // Step 3 - as a last resort, try to recall previously stored times if they exist
                    else {
                        Log.e(TAG, "Unable to get times from\nAPI URL: " + KTHelper.MASJID_KHUTBA_TIMES_API_URL
                                + "\nWeb URL: " + KTHelper.MASJID_KHUTBA_TIMES_URL
                                + "\nAttempting to use stored times");
                        shiftTimes = KTHelper.getStoredTimes();
                        if(shiftTimes!=null) {
                            Log.i(TAG, "Retrieved stored times " + Arrays.toString(shiftTimes));
                            statusMsg = "Unable to sync times with IAR website. Using times from last sync";
                        }
                        else {
                            statusMsg = "Unable to sync times with IAR website. Please check network settings";
                        }
                    }

                }
                catch (Exception e){
                    Log.e(TAG,e.toString());
                }

                // Update the status text to inform failure (or clear if we got the times)
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text3.setText(statusMsg);
                        launchThread();
                    }
                });
            }
        };
        refreshTimes.start();
    }

    private void launchThread() {
        TAG = "launchThread()";
        Log.e(TAG, "in launch thread");
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(new Date());
                        final boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
                        if (friday || testing) {

                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                            String currentTime = sdf.format(new Date());

                            for (int i = 0; i < shiftTimes.length; i++) {

                                // @TODO:  Refactor this to be more flexible w/ # of shifts
                                if (shiftTimes[i].equals(currentTime)) {
                                    if (i == 0) {
                                        shift = "First";
                                        nextShift = "Next shift at " + KTHelper.convertTo12Hour(shiftTimes[1]);
                                    }
                                    else if (i == 1) {
                                        shift = "Second";
                                        nextShift = "Next shift at " + KTHelper.convertTo12Hour(shiftTimes[2]);
                                    }
                                    else if (i == 2 ){
                                        shift = "Third";
                                        nextShift = "Next shift at " + KTHelper.convertTo12Hour(shiftTimes[3]);
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
        CountDownTimer fiveTime = new CountDownTimer(!testing? KhateebTimesHelper.TIME05:KhateebTimesHelper.TIME01, 1000) {
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
                text3.setText("");
            }
        }.start();
    }


    private void startTimer() {
        Log.d("startTimer", "TIMER COUNTDOWN STARTED! " + shift + " Shift, Mins="+ (TIMER_COUNTDOWN_LENGTH/60000));

        if (timer == null) {
            text1.setText(shift + " Shift");
            text3.setText("");
            timer = new CountDownTimer(!testing? TIMER_COUNTDOWN_LENGTH: KhateebTimesHelper.TIME01, 1000) {

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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        TAG = "Alert Dialog";
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        String message = "";

        switch(menuItem.getItemId()) {
            case R.id.viewTimes:
            {
                dialog.setTitle("View Khutbah Times");
                if (shiftTimes == null) {   //error scenario
                    message += "There are no stored times";
                } else {
                    message = "";
                    for (int i = 0; i < shiftTimes.length; i++) {
                        message += "Shift " + (i + 1) + ": " + shiftTimes[i] + "\n";
                    }
                    if (lastSyncTime.equals(""))
                        message = "Last web sync unsuccessful. Using default times\n\n" + message;
                    else message += "\nLast Sync to web: " + lastSyncTime;
                }

                dialog.setMessage(message);
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                dialog.setPositiveButton("Sync Times from Web", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.this.refreshKhutbahTimes();
                        Toast.makeText(MainActivity.this, "Syncing times from website...", Toast.LENGTH_SHORT).show();
                    } });
                dialog.show();
                return true;
            }
            case R.id.timerLength: {
                dialog.setTitle("Khutba Timer Length");
                int currTimerLength = MainActivity.this.KTHelper.getTIMER_COUNTDOWN_LENGTH()/60000;
                message = "Current Countdown (mins): "+ (currTimerLength);

                final EditText edittext = new EditText(this);
                edittext.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                edittext.setText(""+currTimerLength);
                edittext.setHint("minutes");

                dialog.setMessage(message);
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                    } });

                dialog.setPositiveButton("Set New Timeout", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String input = edittext.getText().toString();
                        Log.d(TAG,"onClick: Text input: " + input);
                        try {
                            int newtimer = Integer.parseInt(input);
                            MainActivity.this.KTHelper.setNewTIMER_COUNTDOWN_LENGTH(newtimer);
                            Toast.makeText(MainActivity.this, "Updating timer countodwn...", Toast.LENGTH_SHORT).show();
                        }
                        catch (NumberFormatException e){
                            Log.e(TAG,input + " is not a number");
                        }
                    } });

                dialog.setView(edittext);

                dialog.show();
                return true;
            }

            case R.id.exit: {
                MainActivity.this.finish();
            }
        }
        return false;
    }
}

