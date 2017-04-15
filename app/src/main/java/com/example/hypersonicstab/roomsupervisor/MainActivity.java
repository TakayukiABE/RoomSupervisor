package com.example.hypersonicstab.roomsupervisor;

import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity implements ConnectionReceiver.Observer {

    private boolean inHome = false;
    SSLContext sslcontext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getString(R.string.user_name), getString(R.string.pass_word).toCharArray());
            }
        });

        updateWakeUpTimeText();

        ConnectionReceiver receiver = new ConnectionReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(receiver, intentFilter);

        updateNetworkState();

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        final SharedPreferences data = getSharedPreferences("WakeUpTime", Context.MODE_PRIVATE);
        int wakeUpHour = data.getInt("hour", hour);
        int wakeUpMinute = data.getInt("minute", minute);
        final TimePickerDialog dialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        SharedPreferences.Editor editor = data.edit();
                        editor.putInt("hour", hourOfDay);
                        editor.apply();
                        editor.putInt("minute", minute);
                        editor.apply();
                        updateWakeUpTimeText();
                        Log.d("test",String.format("%02d:%02d", hourOfDay,minute));
                    }
                },
                wakeUpHour,wakeUpMinute,true);

        Button wakeOnLanButton = (Button) findViewById(R.id.wake_on_lan_button);
        Button lightOnButton = (Button) findViewById(R.id.light_on_button);
        Button lightOffButton = (Button) findViewById(R.id.light_off_button);
        Button setTimeButton = (Button) findViewById(R.id.set_time_button);
        Button setAlarmButton = (Button) findViewById(R.id.alarm_button);
        Button removeButton = (Button) findViewById(R.id.remove_button);

        wakeOnLanButton.setOnClickListener(new MyListener(getString(R.string.wake_on_lan)));
        lightOnButton.setOnClickListener(new MyListener(getString(R.string.light_on)));
        lightOffButton.setOnClickListener(new MyListener(getString(R.string.light_off)));
        setAlarmButton.setOnClickListener(new AtListener(getString(R.string.alarm)));
        removeButton.setOnClickListener(new MyListener(getString(R.string.remove_all)));

        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });


        //////////////////////////////////////////////
//オレオレ証明書によるSSLサーバー接続でもエラーをスルーできるようにする

        try {
            //証明書情報 全て空を返す
            //証明書情報　全て空を返す
            TrustManager[] tm = {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }//function
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }//function
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }//function
                    }//class
            };
            sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, tm, null);
            //ホスト名の検証ルール　何が来てもtrueを返す
            HttpsURLConnection.setDefaultHostnameVerifier(
                    new HostnameVerifier(){
                        @Override
                        public boolean verify(String hostname,
                                              SSLSession session) {
                            return true;
                        }//function
                    }//class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }//try

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

    }

    public void updateNetworkState() {
        String ssid = null;
        inHome = false;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            WifiManager wifiManager = (WifiManager) getSystemService (Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo ();
            ssid = info.getSSID();
        }
        if (ssid != null) {
            if (ssid.equals('"' + (getString(R.string.home_ssid)) + '"')) {
                Log.v("equal", "equal!");
                Log.v("SSID!", ssid);
                inHome = true;
            }
        }
    }

    public void updateWakeUpTimeText() {
        SharedPreferences data = getSharedPreferences("WakeUpTime", Context.MODE_PRIVATE);
        int wakeUpHour = data.getInt("hour", 00);
        int wakeUpMinute = data.getInt("minute", 00);
        TextView wakeUpTime = (TextView) findViewById(R.id.wake_up_time_text);
        wakeUpTime.setText(getString(R.string.wake_up_time, String.format("%2d:%2d", wakeUpHour, wakeUpMinute)));
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

    @Override
    public void onConnect() {
        updateNetworkState();
        Log.v("connect", "C");
    }

    @Override
    public void onDisconnect() {
        Log.v("disconnect", "DC");
    }

    public class MyListener implements View.OnClickListener {
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
                        String urlString;
                        if (inHome) {
                            urlString = getString(R.string.base_url_in_home);
                        } else {
                            urlString = getString(R.string.base_url);
                        }
                        URL url = new URL(urlString+MyListener.this.url);

                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        con.setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });


                        con.setSSLSocketFactory(MainActivity.this.sslcontext.getSocketFactory());

                        String str = InputStreamToString(con.getInputStream());
                        Log.d("HTTP", str);
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }).start();
        }
    }
    public class AtListener extends  MyListener {
        public AtListener(String url) {
            super(url);
        }
        @Override
        public void onClick(View v) {
            final Handler mainHandler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SharedPreferences data = getSharedPreferences("WakeUpTime", Context.MODE_PRIVATE);
                        final int wakeUpHour = data.getInt("hour", 00);
                        Log.v("wakeUpHour", String.valueOf(wakeUpHour));
                        final int wakeUpMinute = data.getInt("minute", 00);
                        Log.v("wakeUpMinute", String.valueOf(wakeUpMinute));
                        String urlString;
                        if (inHome) {
                            urlString = getString(R.string.base_url_in_home);
                        } else {
                            urlString = getString(R.string.base_url);
                        }
                        URL url = new URL(urlString+getString(R.string.alarm)+String.format("%02d", wakeUpHour)+String.format("%02d", wakeUpMinute));
                        Log.v("url", String.valueOf(url));
                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        con.setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });


                        con.setSSLSocketFactory(MainActivity.this.sslcontext.getSocketFactory());




                        String str = InputStreamToString(con.getInputStream());
                        Log.d("HTTP", str);
                        mainHandler.post(new Runnable() {
                            public void run() {
                                Log.v("hoge", "thread name:" + Thread.currentThread().getName());
                                Toast.makeText(MainActivity.this, wakeUpHour+"時"+wakeUpMinute+"分に目覚ましを設定しました", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }).start();
        }
    }
}

class ConnectionReceiver extends BroadcastReceiver {
    private Observer mObserver;

    public ConnectionReceiver(Observer observer) {
        mObserver = observer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null) {
            mObserver.onDisconnect();
        }else {
            mObserver.onConnect();
        }
    }

    //----- コールバックを定義 -----
    interface Observer {
        void onConnect();
        void onDisconnect();
    }
}
