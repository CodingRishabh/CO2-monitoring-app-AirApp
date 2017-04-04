package com.example.rishsingh.myapplication2;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.io.IOException;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;
import android.widget.Toast;
import static com.example.rishsingh.myapplication2.MainActivity.*;

//***********    https://github.com/rishsingh/SYSC-3010-project

public  class DisplayMessageActivity extends AppCompatActivity{

    private LineGraphSeries<DataPoint> series; // contains all the points being plotted on graph
    private GraphView graph;
    private int lastX = 0; // x values on the graph starts from 0
    private boolean scrollToEnd = false; // scrolls the graph in real time as new values arrive
    private int CO2; // acts as Y value on the plot
    private Button Ack_message; // sends Ack messages when pressed
    public static EditText editText; // (optional) sens a message with acknowledgement
    public static boolean isSent;// Monitors if ack message has been sent recently
    private static final int THERSHOLD = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        initGraph(); // display graph
        editText = (EditText) findViewById(R.id.edit_message);
        Ack_message = (Button) findViewById(R.id.ack_message);
        Ack_message.setEnabled(false);
    }

    // add data points to the graph as they are being received from the Server
    private void addEntry(int co2) {
        CO2 = co2;
        if(lastX > 100){ // grid automatically scrolls after 100 data-points on grid
            scrollToEnd = true;
        }
        // We choose to display max 100 points on the viewport(plot) then we scroll to the right
        series.appendData(new DataPoint(lastX++, CO2), scrollToEnd, 100);
    }

    /**
     * Takes in CO2 value and enables the Ack button if it is above thersehold
     * and  if notification has not been sent recently
     * @param CO2 concentration as input
     */
    public void AckButtonStatus(int CO2){
        if(CO2 > THERSHOLD && !NotificationSent){
            Ack_message.setEnabled(true);
        }else{
            Ack_message.setEnabled(false);
        }
    }

    /**
     * Starts plotting value on the graph as the user navigates to this Activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        // we're going to simulate real time with thread that append data to the graph
        new Thread(new Runnable() {

            @Override
            public void run() {
                for (;;) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            addEntry(getCO2_concentration());
                            AckButtonStatus(getCO2_concentration());
                        }
                    });
                    // sleep to slow down the add of entries
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        System.out.println("Error occoured: " + e);
                    }
                }
            }
        }).start();
    }

    public void initGraph(){
        graph = (GraphView) findViewById(R.id.graph); // grid displayed to user
        series = new LineGraphSeries<DataPoint>(); // array of points on the plot
        graph.addSeries(series);
        Viewport viewport = graph.getViewport();// Graph window that is visible to user at any moment

        viewport.setMinY(0);
        viewport.setMinX(0);
        viewport.setMaxY(1000);
        viewport.setMaxX(100);
        viewport.setYAxisBoundsManual(true);
        viewport.setXAxisBoundsManual(true);

        viewport.setScrollable(true); //graph automatically scrolls to right
        viewport.setScalableY(true);

        GridLabelRenderer gridLabel = graph.getGridLabelRenderer(); // add labels to grid axis
        gridLabel.setHorizontalAxisTitle("Time (sec)");
        gridLabel.setHorizontalAxisTitleTextSize(60);
        gridLabel.setHorizontalAxisTitleColor(Color.RED);

        gridLabel.setVerticalAxisTitle("CO2 (ppm)");
        gridLabel.setVerticalAxisTitleTextSize(60);
        gridLabel.setVerticalAxisTitleColor(Color.RED);

        gridLabel.setLabelsSpace(15);
    }

    /**
     * Takes a string and turns it into a toast message
     * @param text message as input
     */
    public void toastMessage(CharSequence text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 450); // shift the toast message 450 units down from center
        toast.show();
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) throws IOException {
        NotificationSent = true; // enable notification sent flag
        SecondsTillAck = 0; // restart the timer as ack button is pressed
        Ack_message.setEnabled(false);
        toastMessage("Message sent and Alarm Disabled"); // Display toast message after Ack Button has been pressed
        isSent = true;
    }
}
