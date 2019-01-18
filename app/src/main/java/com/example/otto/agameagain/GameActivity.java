package com.example.otto.agameagain;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;

public class GameActivity extends FragmentActivity implements GamePadListener {

    private static final float maxSpeed = 60.0f;
    private static final float minSpeed = -30.0f;
    SpeedChanger speedChanger = new SpeedChanger();
    private MapView mapView;
    private Gamepad gamepad;
    private ImageView car;
    private ObjectAnimator carRotator;
    private volatile boolean upOn = false;
    private volatile boolean downOn = false;
    private volatile boolean fireOn = false;
    private boolean leftOn = false;
    private boolean rightOn = false;
    private volatile float speed = 0;
    private MapboxMap mMap;
    private Layer backgroundLayer;
    private int actColor = Color.GREEN;

    private Handler handler = new Handler();
    Runnable cameraUpdater = new Runnable() {
        @Override
        public void run() {
            updateCameraPosition();
            handler.postDelayed(this, 100);
        }
    };
    private Handler sirenHandler = new Handler(Looper.getMainLooper());
    MediaPlayer mp = null;

    Runnable copSirenSimulator = new Runnable() {
        @Override
        public void run() {
            if (fireOn) {
                if (mp == null) {
                    mp = MediaPlayer.create(GameActivity.this, R.raw.police);
                    mp.setLooping(true);
                    mp.start();
                }

                if (actColor != Color.RED) {
                    actColor = Color.RED;
                    gamepad.setRedLight();
                } else {
                    actColor = Color.BLUE;
                    gamepad.setBlueLight();
                }
                if (backgroundLayer == null) backgroundLayer = mMap.getLayer("background");
                backgroundLayer.setProperties(PropertyFactory.backgroundColor(actColor));
                sirenHandler.postDelayed(this, 1000);
            } else {
                backgroundLayer.setProperties(PropertyFactory.backgroundColor(Color.BLACK));
                gamepad.resetLight();
                mp.release();
                mp = null;
            }
        }
    };

    private void updateCameraPosition() {
        double rotation = Math.toRadians(car.getRotation());
        float vx = (float) (Math.sin(rotation) * speed);
        float vy = (float) (Math.cos(rotation) * -speed);
        mMap.animateCamera(CameraUpdateFactory.scrollBy(vx, vy), 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_maps_mapbox);
        initMapAndCar(savedInstanceState);
        connectGamePad();
    }

    private void initMapAndCar(Bundle savedInstanceState) {
        mapView = (MapView) findViewById(R.id.mapView);
        car = findViewById(R.id.car);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            mMap = mapboxMap;
            LatLng mimuw = new LatLng(52.211804, 20.982356);
            UiSettings settings = mMap.getUiSettings();
            settings.setCompassEnabled(false);
            settings.setZoomControlsEnabled(false);
            settings.setAllGesturesEnabled(false);
            settings.setRotateGesturesEnabled(false);
            settings.setScrollGesturesEnabled(false);
            settings.setTiltGesturesEnabled(false);
            settings.setZoomGesturesEnabled(false);
            mapView.setStyleUrl("mapbox://styles/gaspardiv/cjqu9koop19v82st0ev3n3z8g");
            CameraPosition cameraPosition = new CameraPosition.Builder().target(mimuw).zoom(18).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            car.setVisibility(View.VISIBLE);
            speedChanger.start();
            handler.postDelayed(cameraUpdater, 100);
        });

    }

    private void connectGamePad() {
        if (getIntent().getExtras() != null) {
            BluetoothDevice bluetoothDevice = getIntent().getExtras().getParcelable(Constants.BT_DEVICE);
            gamepad = new Gamepad(bluetoothDevice, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (gamepad != null) gamepad.disconnect();
        speedChanger.stopThread();
    }

    @Override
    public void onMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFireOn() {
//        sirenHandler.postDelayed(copSirenSimulator, 100);
//        fireOn = true;

        if (fireOn) {
            fireOn = false;
        } else {
            sirenHandler.postDelayed(copSirenSimulator, 100);
            fireOn = true;
        }
    }

    @Override
    public void onFireOff() {
//        fireOn = false;
    }

    @Override
    public void onUpOn() {
        upOn = true;
    }

    @Override
    public void onUpOff() {
        upOn = false;
    }

    @Override
    public void onDownOn() {
        downOn = true;
    }

    @Override
    public void onDownOff() {
        downOn = false;
    }

    @Override
    public void onRightOn() {
        rightOn = true;
        startCarRotating(false);
    }

    @Override
    public void onRightOff() {
        rightOn = false;
        stopRotating();
    }

    @Override
    public void onLeftOn() {
        leftOn = true;
        startCarRotating(true);
    }

    @Override
    public void onLeftOff() {
        leftOn = false;
        stopRotating();
    }


    void startCarRotating(boolean left) {
        if (speed != 0) {
            stopRotating();
            carRotator = ObjectAnimator.ofFloat(car, "rotation", car.getRotation(), car.getRotation() + (left ? -360f : 360));
            carRotator.setInterpolator(new LinearInterpolator());
            carRotator.setRepeatCount(ValueAnimator.INFINITE);
            carRotator.setRepeatMode(ValueAnimator.RESTART);
            carRotator.setDuration(3000); // miliseconds
            carRotator.start();
        }
    }

    void stopRotating() {
        if (carRotator != null) carRotator.cancel();
    }

    public void upClicked(View view) {
        if (upOn) {
            onUpOff();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            onUpOn();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    public void downClicked(View view) {
        if (downOn) {
            onDownOff();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            onDownOn();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    public void rightClicked(View view) {
        if (rightOn) {
            onRightOff();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            onRightOn();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    public void leftClicked(View view) {
        if (leftOn) {
            onLeftOff();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            onLeftOn();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    public void fireClicked(View view) {
        if (fireOn) {
            onFireOff();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            onFireOn();
            ((FloatingActionButton) view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    private class SpeedChanger extends Thread {

        private boolean isRunning = true;

        void stopThread() {
            isRunning = false;
        }

        private void sleep() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isRunning) {
                float oldSpeed = speed;
                float newSpeed;
                if (oldSpeed > 0 && downOn) {
                    newSpeed = oldSpeed - 5f;
                } else {
                    newSpeed = oldSpeed + (upOn ? 3f : (downOn ? -2f : 0));
                }

                if (oldSpeed > 0 && newSpeed <= 0) {
                    speed = 0;
                    sleep();
                    sleep();
                    sleep();
                    sleep();
                    sleep();
                    sleep();
                }
                if (newSpeed < maxSpeed && newSpeed > minSpeed) {
                    speed = newSpeed;
                }
                sleep();
            }
        }
    }
}
