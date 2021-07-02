package com.example.bluetooth_test;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.UUID;


public class Connection {
    private static final String TAG = "ConnectionServ";
    private static final String appName = "Emulator";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("bf8cabba-4c20-460b-83a6-f8ba6a4c7fae");
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private AcceptThread mInSecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    static ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;


    public Connection(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }
    public class AcceptThread extends Thread{
        //local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            //new listening server socket
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName,MY_UUID_INSECURE);
                Log.d(TAG,"AcceptThread: Setting up server using: "+MY_UUID_INSECURE+"--------->>");
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG,"run: AcceptThread Running");
            BluetoothSocket socket = null;
            try{
                Log.d(TAG,"run: RFCOMM server socket start");
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (socket != null){
                Log.d(TAG,"run: Socket connected successfully------------------------------socket");
                connected(socket);
            }
            Log.i(TAG,"End mAcceptThread");
        }
        public void cancel(){
            Log.d(TAG,"cancel: Cancelling AcceptThread");
            try{
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG,"ConnectThread: Started");
            mmDevice = device;
            deviceUUID = uuid;
        }
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG,"RUN mConnectThread");

            try{
                Log.d(TAG,"ConnectThread: Trying to create InsecureRfcommSocket using UUID:"+MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;

            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d(TAG,"run:ConnectThread connected");
            } catch (IOException e) {
                e.printStackTrace(); ///ADDED EXTRA FOR CHECKS
                try{
                    mmSocket.close();
                    Log.d(TAG,"run:SocketClosed");
                } catch (IOException ioException) {
                    Log.d(TAG,"run:ConnectThread unable to connect");
                }
                Log.d(TAG,"run:ConnectThread unable to connect to uuid");
            }
            connected(mmSocket);
        }
        public void cancel(){
            try{
                Log.d(TAG,"cancel:Closing Client Socket");
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG,"cancel:close() of mmSocket in Connection failed"+e.getMessage());
            }
        }
    }


    public synchronized void start(){
        Log.d(TAG,"start---------------------------------------------------------------");
        if (mConnectThread != null){
            mConnectThread.cancel();
            Log.d(TAG,"inside !=null --------------------------------------------------------------");
            mConnectThread = null;
        }
        if (mInSecureAcceptThread == null){
            mInSecureAcceptThread = new AcceptThread();
            mInSecureAcceptThread.start();
        }
    }
    public void startClient(BluetoothDevice device,UUID uuid){
        Log.d(TAG,"startClient:Started");
        mProgressDialog = ProgressDialog.show(mContext,"Progress","Connecting Bluetooth,Please wait...",true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG,"ConnectedThread: Starting.");

            mmSocket = socket;
            Log.d(TAG,"The socket is : " + mmSocket.toString());
            Log.d(TAG,"Socket connection status  : " + mmSocket.isConnected());

            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                mProgressDialog.dismiss();

            }catch (NullPointerException e){
                e.printStackTrace();
            }


            try{
                tempIn = mmSocket.getInputStream();
                tempOut = mmSocket.getOutputStream();
                Log.d(TAG,"i/p o/p streams created....");
                Log.d(TAG,"i/p stream : " + tempIn.toString());
                Log.d(TAG,"o/p stream : " + tempOut.toString());

            } catch (IOException e){
                e.printStackTrace();
                Log.d(TAG,"Error in creating i/p o/p streams....");

            }
            mmInStream = tempIn;
            mmOutStream = tempOut;
        }
        public void run(){
            String s ="LED1";
            byte[] buffer = s.getBytes();
            int bytes;
            Log.d(TAG, "Running ConnectedThread....");
            while(true){
                try{
                    System.out.println("Hallelujah");
                    bytes = mmInStream.read(buffer);
                    System.out.println("Hallelujah");
                    String incomingMessage = new String(buffer,0,bytes);
                    System.out.println("Hallelujah");
                    Log.d(TAG, "Input Stream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent(incomingMessage);
                    incomingMessageIntent.putExtra("theMessage",incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG,"Write: Error reading from i/p stream : "+e.getMessage());

                    break;
                }
            }
        }
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"Write:Writing to OutputStream: "+text);
            try{
                mmOutStream.write(bytes);
            }catch (IOException e){
                Log.e(TAG,"Write: Error writing to o/p stream : "+e.getMessage());
            }
        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){}
        }
    }
    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG,"connected: Starting");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }
    public void write(byte[] out){
        ConnectedThread r;
        Log.d(TAG,"Write: write called");
        mConnectedThread.write(out);
    }
}