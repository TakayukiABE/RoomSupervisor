package com.example.hypersonicstab.roomsupervisor;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Log.d("test",String.format("%02d:%02d", hourOfDay,minute));
                    }
                },
                hour,minute,true);

        Button wakeOnLanButton = (Button) findViewById(R.id.wake_on_lan_button);
        Button lightOnButton = (Button) findViewById(R.id.light_on_button);
        Button lightOffButton = (Button) findViewById(R.id.light_off_button);
        Button setTimeButton = (Button) findViewById(R.id.set_time_button);

        wakeOnLanButton.setOnClickListener(new MyListener(getString(R.string.base_url)+getString(R.string.wake_on_lan)));
        lightOnButton.setOnClickListener(new MyListener(getString(R.string.base_url)+getString(R.string.light_on)));
        lightOffButton.setOnClickListener(new MyListener(getString(R.string.base_url)+getString(R.string.light_off)));

        TextView wakeUpTime = (TextView) findViewById(R.id.wake_up_time_text);
        wakeUpTime.setText(getString(R.string.wake_up_time, "00:00"));


        /*
        lightOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL("http://192.168.11.10:4567/light_on");
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            String str = InputStreamToString(con.getInputStream());
                            Log.d("HTTP", str);
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }
                }).start();
            }
        });
        */

        lightOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL("http://192.168.11.10:4567/light_off");
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            String str = InputStreamToString(con.getInputStream());
                            Log.d("HTTP", str);
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }
                }).start();
            }
        });
    }

    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    public static class MyListener implements View.OnClickListener {
        private String url;

        public MyListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(MyListener.this.url);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        String str = InputStreamToString(con.getInputStream());
                        Log.d("HTTP", str);
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }).start();
        }
    }
}
