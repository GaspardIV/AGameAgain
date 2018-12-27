package com.example.otto.agameagain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;

public class BtService {
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public BtService(Handler mHandler, BluetoothDevice bluetoothDevice) {
        this.mHandler = mHandler;
        ParcelUuid[] idArray = bluetoothDevice.getUuids();
        mConnectThread = new ConnectThread(bluetoothDevice, idArray);
        mConnectThread.start();
    }


    private void onConnected(BluetoothSocket mmSocket) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connected");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }


    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device disconnected");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            r = mConnectedThread;
        }
        r.write(out);
    }

    private class ConnectThread extends Thread {
        final String TAG = "ConnectThread";

        private final BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device, ParcelUuid[] uuids) {
            BluetoothSocket tmp = null;
            for (ParcelUuid parcelUuid : uuids) {
                try {
                    tmp = device.createRfcommSocketToServiceRecord(parcelUuid.getUuid());
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Socket's create() method failed", e);
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            onConnected(mmSocket);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }

    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(Constants.FROM_DEVICE, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(Constants.SENT, -1, -1, buffer)
//                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
