package com.example.otto.agameagain;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

public class Gamepad {
    private BtService btService;
    private GamePadListener listener;

    public Gamepad(BluetoothDevice bluetoothDevice, GamePadListener listener) {
        connectAndStartCommunication(bluetoothDevice);
        this.listener = listener;
    }

    public void disconnect() {
        if (btService != null) btService.stop();

    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.FROM_DEVICE:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    for (char c: readMessage.toCharArray()) {
                        switch (c){
                            case 'F':
                                listener.onFireOn();
                                break;
                            case 'f':
                                listener.onFireOff();
                                break;
                            case 'U':
                                listener.onUpOn();
                                break;
                            case 'u':
                                listener.onUpOff();
                                break;
                            case 'R':
                                listener.onRightOn();
                                break;
                            case 'r':
                                listener.onRightOff();
                                break;
                            case 'L':
                                listener.onLeftOn();
                                break;
                            case 'l':
                                listener.onLeftOff();
                                break;
                            case 'D':
                                listener.onDownOn();
                                break;
                            case 'd':
                                listener.onDownOff();
                                break;
                        }
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    listener.onMessage(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    public void setRedLight() {
        btService.write( new byte[]{'g', 'b', 'R'});
    }

    public void setYellowLight() {
        btService.write( new byte[]{'G', 'b', 'R'});
    }

    public void setGreenLight() {
        btService.write( new byte[]{'G', 'b', 'r'});
    }

    public void resetLight() {
        btService.write( new byte[]{'g', 'b', 'r'});
    }

    private void connectAndStartCommunication(BluetoothDevice bluetoothDevice) {
        btService = new BtService( mHandler, bluetoothDevice);
    }
}
