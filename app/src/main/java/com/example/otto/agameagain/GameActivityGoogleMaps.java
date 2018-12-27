package com.example.otto.agameagain;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

public class GameActivityGoogleMaps extends FragmentActivity implements OnMapReadyCallback, GamePadListener {

    private static final float maxSpeed = 60.0f;
    private static final float minSpeed = -30.0f;
    SpeedChanger speedChanger = new SpeedChanger();
    private GoogleMap mMap;
    private Gamepad gamepad;
    private ImageView car;
    private ObjectAnimator carRotator;
    private volatile boolean upOn = false;
    private volatile boolean downOn = false;
    private volatile boolean fireOn = false;
    private boolean leftOn = false;
    private boolean rightOn = false;
    //    private volatile double acceleration = 0;
    private volatile float speed = 0;
    private volatile boolean isGameOn = true;

    private GoogleMap.CancelableCallback driveCallBack = new GoogleMap.CancelableCallback() {
        @Override
        public void onFinish() {
            double rotation = Math.toRadians(car.getRotation());
            float vx = (float) (Math.sin(rotation) * speed);
            float vy = (float) (Math.cos(rotation) * -speed);
            mMap.animateCamera(CameraUpdateFactory.scrollBy(vx, vy), 100, driveCallBack);
        }

        @Override
        public void onCancel() {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_google);
        initMapAndCar();
        connectGamePad();
    }

    private void initMapAndCar() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        car = findViewById(R.id.car);
    }

    public static Bitmap loadBitmapFromView(View v, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width , height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
        v.draw(c);
        return b;
    }

    GoogleMap.SnapshotReadyCallback snapshotReadyCallback = new GoogleMap.SnapshotReadyCallback() {
        @Override
        public void onSnapshotReady(Bitmap snapshot) {
//            Log.d("asd", "onSnapshotReady: " + car.getX() + " " + car.getY() + " " + car.getRotation());
//            int [] coords = new int[2];
//            car.getLocationInWindow(coords);
//            Log.d("xded", "onSnapshotReady: " + coords[0] + " " + coords[1]);
//            car.getLocationOnScreen(coords);
//            Log.d("xded3", "onSnapshotReady: " + coords[0] + " " + coords[1]);
            Bitmap cared = Bitmap.createBitmap(snapshot, snapshot.getWidth()/2-15, snapshot.getHeight()/2-15, 30, 30);
            mMap.snapshot(this);
        }
    };

    private void connectGamePad() {
        if (getIntent().getExtras() != null) {
            BluetoothDevice bluetoothDevice = getIntent().getExtras().getParcelable(Constants.BT_DEVICE);
            gamepad = new Gamepad(bluetoothDevice, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gamepad.disconnect();
        speedChanger.stopThread();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng mimuw = new LatLng(52.211804, 20.982356);
        mMap.setBuildingsEnabled(true);
        UiSettings settings = mMap.getUiSettings();
        settings.setCompassEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setAllGesturesEnabled(false);
        settings.setIndoorLevelPickerEnabled(false);
        settings.setMapToolbarEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setRotateGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setZoomGesturesEnabled(false);

        boolean success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_xd));


        CameraPosition cameraPosition = new CameraPosition.Builder().target(mimuw).zoom(20).tilt(10).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        car.setVisibility(View.VISIBLE);
        speedChanger.start();
        driveCallBack.onFinish();
//        startSnaping();
    }

    private void startSnaping() {
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.snapshot(snapshotReadyCallback);
                mMap.setOnMapLoadedCallback(null);
            }
        });
    }

    @Override
    public void onMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFireOn() {

    }

    @Override
    public void onFireOff() {

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
        if (carRotator!= null) carRotator.pause();
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
//        startSnaping();
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
