package com.example.rishsingh.myapplication2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Handler;

import java.io.IOException;
import java.net.UnknownHostException;

//*******   https://github.com/rishsingh/SYSC-3010-project

public class MainActivity extends AppCompatActivity {

    private Button btnDisplay;// takes the user to another activity
    private ToggleButton toggleButton2;// turn on/off notification
    private TextView textView = null; // display CO2 value on mainActivity
    private Handler handler = new Handler();// Queues Co2 values received from server and sends them to runnable
    private Ringtone Alarm;// warning noise co2 level exceeded thereshold
    private Receiver Receiver; // Thread that receives CO2 values and sends ack messages to Server
    private Button ip_button;
    private EditText ip_field;
    public static boolean NotificationSent; // if true then notification has been sent
    public static int CO2_concentration;
    public static int SecondsTillAck = 0; // Tracks the time after ACK button has been pressed

    private static final int THERSHOLD = 250;

    /**
     * Initalizes the buttons, textfields and textviwes, alarmSound ringtone.
     * the server.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Receiver = new Receiver();
            Receiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


        ip_field = (EditText) findViewById(R.id.ip_field);
        ip_button = (Button) findViewById(R.id.ip_button);

        textView = (TextView) findViewById(R.id.text_id); // Display Co2 values in real time
        textView.setTextSize(25);
        toggleButton2 = (ToggleButton) findViewById(R.id.toggleButton2);
        btnDisplay = (Button) findViewById(R.id.btnDisplay);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
        Alarm = RingtoneManager.getRingtone(getApplicationContext(), notification);
        NotificationSent = false;

        //Runs a background thread using handler that constantly
        // updates the textView with the latest CO2 values sent from server
        Runnable r=new Runnable() {
            public void run() {
                handler.postDelayed(this, 1000);
                CO2_concentration = Receiver.getCO2();

                textView.setText("CO2 Levels: " + CO2_concentration + " ppm\n\n");

                SecondsTillAck++; // increments the timer until it is reset by ack button press

                if(CO2_concentration > THERSHOLD && !NotificationSent && (toggleButton2).isChecked()){
                    notification();
                }else if(Alarm.isPlaying() && (CO2_concentration < THERSHOLD || NotificationSent|| !(toggleButton2).isChecked())){
                    Alarm.stop();
                }
                if(SecondsTillAck >= 10){ // wait 10 seconds until ack notification button activates again
                    NotificationSent = false;
                }
                System.out.println("noti status - "+ NotificationSent);
                System.out.println("seconds till pressed : "+ SecondsTillAck);
            }
        };
        handler.postDelayed(r, 1000); // 1 sec delay to simulate values from the server
    }

    /**
     * Builds the notification and plays the warning alarm
     */
    public void notification(){
        NotificationCompat.Builder builder =
                (android.support.v7.app.NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("(WARNING) " + "CO2 level: " + CO2_concentration)
                        .setContentText("Warning CO2 levels have reached: " + CO2_concentration);


        // tapping notification will display real time graph of values in DisplayMessageActivity
        Intent notificationIntent = new Intent(this, DisplayMessageActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);
        builder.setLights(Color.BLUE, 500, 500);
        long[] pattern = {500, 500};// interval between notification
        builder.setVibrate(pattern);
        builder.setStyle(new NotificationCompat.InboxStyle());

        Alarm.play();
        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    public static int getCO2_concentration(){
        return CO2_concentration;
    }

    /**
     * Takes user to the graph activity when display button is pressed
     * @param view
     */
    public void displayMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        startActivity(intent);
    }

    public void setIP(View view) throws UnknownHostException {
        String checkIP = ip_field.getText().toString();
        if(validIP(checkIP)){
            Receiver.setIPAddress(checkIP);
            Receiver.setInitalizeConnection(true);
        }else {
            toastMessage("Invalid Ip");
        }

    }

    /**
     * Takes a string and turns it into a toast message
     * @param text message as input
     */
    public void toastMessage(CharSequence text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 600); // shift the toast message 450 units down from center
        toast.show();
    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
