package com.example.otto.agameagain;

interface GamePadListener {

    void onMessage(String string);

    void onFireOn();

    void onFireOff();

    void onUpOn();

    void onUpOff();

    void onRightOn();

    void onRightOff();

    void onLeftOn();

    void onLeftOff();

    void onDownOn();

    void onDownOff();
}
