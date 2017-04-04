package com.example.rishsingh.myapplication2;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import static com.example.rishsingh.myapplication2.DisplayMessageActivity.editText;
import static com.example.rishsingh.myapplication2.DisplayMessageActivity.isSent;

public class Receiver  extends Thread{
    public static final String ANDROID_REQUEST = "android";
    public static DatagramSocket serverSocket; // Receives CO2values from server
    private byte[] receiveData;
    private static final int PORT = 6799; // port that we (android) is listening on
    private int dstPORT = 6777; // send ac messages to this port
    private InetAddress IPAddress;// dst address
    private int count; // counts packet received from server
    private int CO2_VALUE = 0;
    private DatagramPacket AckReceivePacket;
    private boolean initalizeConnection;
    DatagramPacket receivePacket;
    DatagramSocket ListeningSocket;


    public Receiver() throws IOException{
        this.receiveData = new byte[512]; // data received in a 512 byte array from server
        this.count = 0;
        IPAddress = InetAddress.getByName("192.168.43.214");
        serverSocket = new DatagramSocket(PORT);
        ListeningSocket = new DatagramSocket(6888);
        ListeningSocket.setSoTimeout(1000);
        initalizeConnection = true;
    }

    public void setIPAddress(String ip) throws UnknownHostException {
        IPAddress = InetAddress.getByName(ip);
    }

    public void setInitalizeConnection(boolean bool){
        initalizeConnection = bool;
    }

    public DatagramPacket getReceivePacket(){
        return receivePacket;
    }

    /**
     *  takes in data and creates a data-gram packet and sends it to destination
     * @param sendData data to be sent
     * @throws IOException
     */
    public void sendPacket(byte[] sendData) throws IOException{
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, dstPORT);
        ListeningSocket.send(sendPacket);
    }
    /**
     * creates a packet the excepts data from the server
     * @throws IOException
     */
    public void receivePacket() throws IOException {
        AckReceivePacket = new DatagramPacket(receiveData, receiveData.length);
        ListeningSocket.receive(AckReceivePacket);
        Arrays.fill(receiveData, (byte) 1 );
    }

    private boolean processPacket(DatagramPacket packet){
        String message = new String(packet.getData());
        if(message.startsWith("ok")){
            return true;
        }
        return false;
    }

    public void startConnection(){
        try {
            System.out.println("sending a connection request" );
            sendPacket(Receiver.ANDROID_REQUEST.getBytes());
            System.out.println("Waiting for connection" );
            receivePacket();
            System.out.println("Waiting for connection..");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Runs the thread which will be  waiting to receive values from the server
     */
    public void run(){
        while(true){
            if(initalizeConnection){
                startConnection();
                initalizeConnection = false;
            }
            receivePacket = new DatagramPacket(this.receiveData, this.receiveData.length);
            try {
                serverSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Packet [" + this.count + "] arrived with length " + receivePacket.getData().length + " from : ip " + receivePacket.getAddress() + " port : "+ receivePacket.getPort());
            this.count++;
            String str = new String(receivePacket.getData()).trim();
            CO2_VALUE = Integer.parseInt(str);
            Log.d("myTag", str + " ppm");

            if(isSent && !(AckReceivePacket.getPort() == -1)){
                String message = "Ack message says " + "'" + editText.getText().toString()+ "'";
                byte[] sendData = message.getBytes();

                if(!serverSocket.isClosed()){
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, AckReceivePacket.getAddress(), AckReceivePacket.getPort());
                    try {
                        serverSocket.send(sendPacket);
                        isSent = false;
                        //receivePacket();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("Server has closed its connection" );
                    serverSocket.close();
                    break;
                }
            }

            if(!serverSocket.isBound()){
                break;
            }
        }
        System.out.println("\n Finished receving Data...");
        serverSocket.close();
    }

    public int getCO2(){ return CO2_VALUE;}

}
