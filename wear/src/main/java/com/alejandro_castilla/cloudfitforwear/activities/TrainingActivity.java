package com.alejandro_castilla.cloudfitforwear.activities;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.adapters.TrainingActivityGridPagerAdapter;
import com.alejandro_castilla.cloudfitforwear.data.ACCData;
import com.alejandro_castilla.cloudfitforwear.data.GPSLocation;
import com.alejandro_castilla.cloudfitforwear.data.HeartRate;
import com.alejandro_castilla.cloudfitforwear.data.WearableTraining;
import com.alejandro_castilla.cloudfitforwear.data.exercises.Exercise;
import com.alejandro_castilla.cloudfitforwear.data.exercises.Rest;
import com.alejandro_castilla.cloudfitforwear.data.exercises.Running;
import com.alejandro_castilla.cloudfitforwear.services.bluetooth.BluetoothService;
import com.alejandro_castilla.cloudfitforwear.services.zephyrsensor.ZephyrService;
import com.alejandro_castilla.cloudfitforwear.utilities.StaticVariables;
import com.alejandro_castilla.cloudfitforwear.utilities.Utilities;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TrainingActivity extends WearableActivity implements View.OnClickListener,
        SensorEventListener {

    private final String TAG = TrainingActivity.class.getSimpleName();
    private final int PERMISSIONS_REQUEST_CODE = 0xFF;

    private PowerManager.WakeLock wakeLock;

    /*Fields for the views used on the layout*/

    private Chronometer chronometer;
    private ImageView resumeActionImgView, pauseActionImgView, exitActionImgView;
    private TextView
            heartRateTextView,
            distanceTextView,
            infoTextView,
            exerciseInfoTextView,
            heartSensorStatusTextView,
            locationStatusTextView,
            resumeActionTextView,
            pauseActionTextView;
    private Button finishExerciseButton;
    private GridViewPager gridViewPager;

    /* Preferences fields */

    private boolean heartRateEnabled;
    private boolean zephyrEnabled;
    private boolean locationEnabled;

    /* Status fields */

    private boolean sessionPaused = false;

    /* Chronometer fields */

    private long timeWhenPaused;
    private boolean chronoAllowedToStart = true;

    /* Internal sensors fields */

    private SensorManager sensorManager;

    /* Data fields */

    private WearableTraining training;
    private Exercise currentExercise;
    private int currentExerciseIndex;
    private double maxDistance;
    private long accReferenceTimeStamp;
    private int heartRateMax;
    private int heartRateMin;

    private ArrayList<Exercise> exercisesCompleted;
    private ArrayList<HeartRate> heartRateList;
    private ArrayList<GPSLocation> GPSLocationsList;
    private ArrayList<Location> locations;
    private ArrayList<ACCData> accDataList;

    /* Location fields */

    private LocationManager locManager;
    private LocationListener locListener;
    private float totalDistance;
    private boolean firstLocationReceived;

    /* Fields to connect to services */

    private BluetoothService bluetoothService;
    private boolean bluetoothServiceBinded = false;
    private ZephyrService zephyrService;
    private boolean zephyrServiceBinded = false;

    private ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.BluetoothServiceBinder bluetoothServiceBinder =
                    (BluetoothService.BluetoothServiceBinder) service;
            bluetoothService = bluetoothServiceBinder.getService();
            bluetoothService.findBluetoothDevice("C8:3E:99:0D:DD:43"); //Zephyr Bioharness 3 MAC
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection zephyrServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ZephyrService.ZephyrServiceBinder zephyrServiceBinder =
                    (ZephyrService.ZephyrServiceBinder) service;
            zephyrService = zephyrServiceBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /*Messenger fields*/

    private final Handler messageHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case StaticVariables.DEVICE_FOUND:
                    Log.d(TAG, "Device received on " + TAG);
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    zephyrService.connectToZephyr(device);
                    break;
                case StaticVariables.ZEPHYR_HEART_RATE:
                    //Start chronometer when heart rate sensor is ready
                    startChronometerAndUpdateInfo(chronoAllowedToStart,
                            SystemClock.elapsedRealtime());
                    chronoAllowedToStart = false; //Starts chronometer only one time

                    String heartRateString = msg.getData().getString("heartratestring");
                    long timeMark = SystemClock.elapsedRealtime() - chronometer.getBase();
                    int heartRateInt = Integer.parseInt(heartRateString);
                    heartRateTextView.setText(heartRateString);

                    if (!sessionPaused) {
                        checkHeartRate(heartRateInt);
                        saveHeartRate(timeMark, heartRateInt);
                    }
                    break;
                case StaticVariables.DEVICE_NOT_FOUND:
                    Intent intent = new Intent (TrainingActivity.this, MainActivity.class);
                    startActivity(intent);
                    Utilities.showConfirmation(TrainingActivity.this,
                            "No se ha podido conectar con el sensor Zephyr",
                            ConfirmationActivity.FAILURE_ANIMATION);
                    finish();
            }
            super.handleMessage(msg);
        }
    };

    private Messenger trainingActivityMessenger = new Messenger(messageHandler);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                chronometer = (Chronometer) findViewById(R.id.practiceChronometer);
                heartRateTextView = (TextView) findViewById(R.id.heartRateText);
                distanceTextView = (TextView) findViewById(R.id.distanceText);
                infoTextView = (TextView) findViewById(R.id.infoText);

                /*Sensors info layout items*/

                heartSensorStatusTextView = (TextView) findViewById(R.id.heartSensorInfoText);
                locationStatusTextView = (TextView) findViewById(R.id.locationInfoText);

                /*Exercise info layout items*/

                exerciseInfoTextView = (TextView) findViewById(R.id.exerciseInfoText);
                finishExerciseButton = (Button) findViewById(R.id.finishExerciseButton);

                /*Actions items*/

                resumeActionImgView = (ImageView) findViewById(R.id.practiceResumeActionImg);
                pauseActionImgView = (ImageView) findViewById(R.id.practicePauseActionImg);
                pauseActionTextView = (TextView) findViewById(R.id.practicePauseActionText);
                resumeActionTextView = (TextView) findViewById(R.id.practiceResumeActionText);
                exitActionImgView = (ImageView) findViewById(R.id.practiceExitActionImg);

                //Pause action listener is set later
                resumeActionImgView.setOnClickListener(TrainingActivity.this);
                exitActionImgView.setOnClickListener(TrainingActivity.this);

                gridViewPager = (GridViewPager) stub.findViewById(R.id.practicePager);
                gridViewPager.setAdapter(new TrainingActivityGridPagerAdapter());
                gridViewPager.setOffscreenPageCount(4);
                DotsPageIndicator dotsPageIndicator = (DotsPageIndicator)
                        findViewById(R.id.practicePageIndicator);
                dotsPageIndicator.setPager(gridViewPager);

                checkSharedPreferencesAndParseTraining();
                if (isFinishing()) return; //Avoid execution of unnecessary code

                //Check necessary permissions and request them

                boolean locationGranted = ContextCompat.checkSelfPermission(TrainingActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

                boolean sensorsGranted = ContextCompat.checkSelfPermission(TrainingActivity.this,
                        Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;

                if (!locationGranted || !sensorsGranted) {
                    ActivityCompat.requestPermissions(TrainingActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_CODE);
                    Log.d(TAG, "Permissions requested");
                } else {
                    initSensors();
                    initLocation();
                }

            }
        });

        setAmbientEnabled();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "CloudFit For Wear");
        wakeLock.acquire();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        boolean permissionsGranted = true;

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:

                for (int result : grantResults) {
                    if (!(result == PackageManager.PERMISSION_GRANTED)) {
                        permissionsGranted = false;
                    }
                }

                if (permissionsGranted) {
                    initSensors();
                    initLocation();
                } else {
                    Toast.makeText(TrainingActivity.this, "Permisos insuficientes",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int resId = v.getId();
        long timeElapsed;
        switch (resId) {
            case R.id.practicePauseActionImg:
                timeWhenPaused = SystemClock.elapsedRealtime() - chronometer.getBase();
                chronometer.stop();
                sessionPaused = true;
                pauseActionImgView.setVisibility(View.GONE);
                resumeActionImgView.setVisibility(View.VISIBLE);
                pauseActionTextView.setVisibility(View.GONE);
                resumeActionTextView.setVisibility(View.VISIBLE);
                infoTextView.setText("Entrenamiento pausado");
                break;
            case R.id.practiceResumeActionImg:
                startChronometerAndUpdateInfo(true, SystemClock.elapsedRealtime() - timeWhenPaused);
                sessionPaused = false;
                pauseActionImgView.setVisibility(View.VISIBLE);
                resumeActionImgView.setVisibility(View.GONE);
                pauseActionTextView.setVisibility(View.VISIBLE);
                resumeActionTextView.setVisibility(View.GONE);
                break;
            case R.id.practiceExitActionImg:
                stopServices();

                if (sessionPaused) {
                    timeElapsed = timeWhenPaused;
                } else {
                    timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
                }

                if (currentExerciseIndex == (training.getExercises().size() - 1)) {
                    saveExerciseData(timeElapsed);
                    saveTrainingDataAndFinish();
                } else {
                    Intent intent = new Intent (TrainingActivity.this, MainActivity.class);
                    startActivity(intent);
                    Utilities.showConfirmation(TrainingActivity.this,
                            "No has completado el entrenamiento",
                            ConfirmationActivity.FAILURE_ANIMATION);
                    finish();
                }
                break;
            case R.id.finishExerciseButton:
                long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                saveExerciseData(time);
                resetDataAndMoveToNextExercise();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        stopServices();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this); //Listener for internal heart rate sensor
                                                    // and acc
        }

        try {
            if (locManager != null) {
                locManager.removeUpdates(locListener);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        wakeLock.release();

        super.onDestroy();
    }

    private void initSensors() {
        heartRateList = new ArrayList<>();
        accDataList = new ArrayList<>();
        accReferenceTimeStamp = 0;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(TrainingActivity.this, accSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        if (heartRateEnabled && zephyrEnabled) {
            // Start bluetooth and Zephyr sensor services
            infoTextView.setText("Conectando con el sensor...");

            Intent bluetoothServiceIntent = new Intent(TrainingActivity.this,
                    BluetoothService.class);
            bluetoothServiceIntent.putExtra("messenger", trainingActivityMessenger);
            bindService(bluetoothServiceIntent, bluetoothServiceConnection,
                    Context.BIND_AUTO_CREATE);
            bluetoothServiceBinded = true;

            Intent zephyrServiceIntent = new Intent(TrainingActivity.this,
                    ZephyrService.class);
            zephyrServiceIntent.putExtra("messenger", trainingActivityMessenger);
            bindService(zephyrServiceIntent, zephyrServiceConnection,
                    Context.BIND_AUTO_CREATE);
            zephyrServiceBinded = true;
        } else if (heartRateEnabled) {
            //Initialize internal heart rate sensor (if it's available)
            infoTextView.setText("Iniciando pulsómetro...");
            Sensor heartRateInternalSensor = sensorManager
                    .getDefaultSensor(Sensor.TYPE_HEART_RATE);
            if (heartRateInternalSensor == null) {
                Intent intent = new Intent (TrainingActivity.this, MainActivity.class);
                startActivity(intent);
                Utilities.showConfirmation(TrainingActivity.this,
                        "Este dispositivo no tiene pulsómetro",
                        ConfirmationActivity.FAILURE_ANIMATION);
                finish();
                return;
            }
            sensorManager.registerListener(TrainingActivity.this, heartRateInternalSensor,
                    SensorManager.SENSOR_DELAY_UI);
        } else {
            //Start without heart rate sensors
            startChronometerAndUpdateInfo(chronoAllowedToStart, SystemClock.elapsedRealtime());
            chronoAllowedToStart = false;
            pauseActionImgView.setOnClickListener(TrainingActivity.this);
            heartSensorStatusTextView.setText("Desactivado");
        }
    }

    private void initLocation() {
        GPSLocationsList = new ArrayList<>();

        if (!locationEnabled) {
            locationStatusTextView.setText("Desactivado");
            return;
        }

        firstLocationReceived = false;
        locations = new ArrayList<>();
        totalDistance = 0;

        PackageManager pm = this.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) { //Check if GPS is available
            distanceTextView.setText("0.00");
            locationStatusTextView.setText("Esperando GPS");
            locManager = (LocationManager)
                    this.getSystemService(Context.LOCATION_SERVICE);
            locListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (!firstLocationReceived) {
                        Log.d(TAG, "First location received.");
                        saveGPSData(location);
                        firstLocationReceived = true;
                        locationStatusTextView.setText("Recibiendo datos");
                    } else {
                        //Distance between last location and this new location (km)
                        // with two decimals
                        DecimalFormat precision = new DecimalFormat("0.00");
                        totalDistance += locations.get(locations.size()-1)
                                .distanceTo(location) / 1000;
                        saveGPSData(location);
                        distanceTextView.setText(precision.format(totalDistance));
                    }

                    //Stop exercise if max distance is reached
                    if (totalDistance > maxDistance && maxDistance != 0) {
                        long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                        saveExerciseData(time);
                        resetDataAndMoveToNextExercise();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.d(TAG, "Location status changed");
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Log.d(TAG, "Location provider enabled");
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.d(TAG, "Location provider disabled");
                }
            };

            try {
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
            } catch (SecurityException e) { //This exception is thrown with insufficient permissions
                e.printStackTrace();
                locationStatusTextView.setText("Error permisos");
            }
        } else {
            locationStatusTextView.setText("GPS no disponible");
        }
    }

    private void saveHeartRate(long timeMark, int heartRate) {
        HeartRate hr = new HeartRate(timeMark, heartRate);
        heartRateList.add(hr);
    }

    private void checkHeartRate (int hrValue) {

        if (hrValue < heartRateMin && heartRateMin != 0) {
            infoTextView.setText("¡Frec. cardíaca muy baja!");
            Utilities.vibrate(this, 2000);
        }

        if (hrValue > heartRateMax && heartRateMax != 0) {
            infoTextView.setText("¡Frec. cardíaca alta!");
            Utilities.vibrate(this, 500);
        }

        //Normal heart rate
        if (hrValue < heartRateMax && hrValue > heartRateMin) {
            infoTextView.setText("Entrenamiento iniciado");
        }

    }

    private void saveGPSData (Location l) {
        long timeStamp = System.currentTimeMillis();
        GPSLocation GPSLoc = new GPSLocation(timeStamp);
        GPSLoc.setLatitude(l.getLatitude());
        GPSLoc.setLongitude(l.getLongitude());
        GPSLoc.setAltitude(l.getAltitude());
        GPSLoc.setTime(l.getTime());
        GPSLoc.setSpeed(l.getSpeed());
        GPSLocationsList.add(GPSLoc);
        locations.add(l);
    }

    private void stopServices() {
        if (bluetoothServiceBinded) {
            bluetoothService.stopFindTask(); //If this task is still running, we have to stop it to
                                            //avoid problems
            unbindService(bluetoothServiceConnection);
            bluetoothServiceBinded = false;
        }

        if (zephyrServiceBinded) {
            unbindService(zephyrServiceConnection);
            zephyrServiceBinded = false;
        }
    }

    private void prepareCurrentExercise() {
        maxDistance = 0;
        heartRateMin = 0;
        heartRateMax = 0;

        if (currentExercise.getType() == Exercise.TYPE_RUNNING) {
            final Running running = currentExercise.getRunning();

            if (running.getDistanceP() != -1.0 && running.getDistanceP() != 0.0) {
                maxDistance = running.getDistanceP();
            } else if (running.getTimeP() != -1.0) {

                if (running.getTimeMaxP() != -1.0) {
                    //Stop training when max time is reached.
                    chronometer.setOnChronometerTickListener(new Chronometer
                            .OnChronometerTickListener() {
                        @Override
                        public void onChronometerTick(Chronometer chronometer) {
                            long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                            if (time >= (running.getTimeMaxP()*1000)) {
                                saveExerciseData(time);
                                Utilities.buildNotification(TrainingActivity.this,
                                        "Información",
                                        "Carrera completada correctamente.");
                                resetDataAndMoveToNextExercise();
                            }
                        }
                    });
                }

            }

            if (running.getHeartRateMin() > 0) {
                heartRateMin = running.getHeartRateMin();
            }

            if (running.getHeartRateMax() > 0) {
                heartRateMax = running.getHeartRateMax();
            }

        } else if (currentExercise.getType() == Exercise.TYPE_REST) {
            final Rest rest = currentExercise.getRest();

            chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                    if (time >= rest.getRestp()*1000) {
                        saveExerciseData(time);
                        Utilities.buildNotification(TrainingActivity.this,
                                "Información",
                                "Descanso completado correctamente.");
                        resetDataAndMoveToNextExercise();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Ha ocurrido un error al preparar el entrenamiento",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        exerciseInfoTextView.setText(Utilities.buildExerciseInfo(currentExercise));
        currentExercise.setStartTime(System.currentTimeMillis());
    }

    private void checkSharedPreferencesAndParseTraining() {
        //Check if the user wants to use Zephyr Sensor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        heartRateEnabled = prefs.getBoolean(StaticVariables.KEY_PREF_HEART_RATE_ENABLED, false);
        zephyrEnabled = prefs.getBoolean(StaticVariables.KEY_PREF_ZEPHYR_ENABLED, false);
        locationEnabled = prefs.getBoolean(StaticVariables.KEY_PREF_LOCATION_ENABLED, false);

        String tr = prefs.getString(StaticVariables.KEY_TRAINING_TO_BE_DONE, "");
        String trDone = prefs.getString(StaticVariables.KEY_TRAINING_DONE, "");

        if (!tr.equals("")) {
            //Training available
            if (!trDone.equals("")) { //A training completed exists and needs an action
                Intent intent = new Intent(TrainingActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(this, "Sincroniza y/o elimina " +
                        "el entrenamiento completado antes de continuar", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Gson gson = new Gson();
            training = gson.fromJson(tr, WearableTraining.class);
            currentExerciseIndex = 0;
            currentExercise = training.getExercises().get(currentExerciseIndex);
            Log.d(TAG, "NUMBER OF EXERCISES: "+training.getExercises().size());

            prepareCurrentExercise();

            training.setStartDate(System.currentTimeMillis());
            exercisesCompleted = new ArrayList<>();

        } else {
            // Training not available
            Intent intent = new Intent(TrainingActivity.this, MainActivity.class);
            startActivity(intent);
            Utilities.showConfirmation(this, "No hay ningún entrenamiento disponible",
                    ConfirmationActivity.FAILURE_ANIMATION);
            finish();
        }
    }

    private void startChronometerAndUpdateInfo(boolean allowed, long baseTime) {
        if (allowed) { //If the chronometer is started, is not allowed to start.

            pauseActionImgView.setOnClickListener(TrainingActivity.this);
            finishExerciseButton.setOnClickListener(TrainingActivity.this);

            if (heartRateEnabled) heartSensorStatusTextView.setText("Recibiendo datos");

            if (currentExercise.getType() == Exercise.TYPE_RUNNING) {
                infoTextView.setText("Entrenamiento iniciado");
            } else if (currentExercise.getType() == Exercise.TYPE_REST) {
                infoTextView.setText("En recuperación: "+currentExercise
                        .getRest().getRestp()/60+" min");
            }

            chronometer.setBase(baseTime);
            chronometer.start();
        }
    }

    private void resetDataAndMoveToNextExercise() {
        heartRateList = new ArrayList<>(); //Reset heart rate data

        GPSLocationsList = new ArrayList<>(); //Reset location data
        maxDistance = 0;
        totalDistance = 0;
        distanceTextView.setText("0.00");

        accDataList = new ArrayList<>(); //Reset acc data

        chronometer.stop();
        currentExerciseIndex++;

        if (currentExerciseIndex == training.getExercises().size()) { //No more exercises
            saveTrainingDataAndFinish();
            return;
        }

        currentExercise = training.getExercises().get(currentExerciseIndex);
        prepareCurrentExercise();
        startChronometerAndUpdateInfo(true, SystemClock.elapsedRealtime()); //Reset chronometer

        //If session is paused, update layout and session status
        if (sessionPaused) {
            pauseActionImgView.setVisibility(View.VISIBLE);
            resumeActionImgView.setVisibility(View.GONE);
            pauseActionTextView.setVisibility(View.VISIBLE);
            resumeActionTextView.setVisibility(View.GONE);

            sessionPaused = false; //Session is started again
        }

    }

    private void saveTrainingDataAndFinish() {
        training.setEndDate(System.currentTimeMillis());
        training.setExercises(exercisesCompleted);

        Gson gson = new Gson();
        SharedPreferences.Editor prefsEditor = PreferenceManager
                .getDefaultSharedPreferences(this).edit();
        prefsEditor.putString(StaticVariables.KEY_TRAINING_DONE, gson.toJson(training));
        prefsEditor.apply();
        Log.d(TAG, "Training saved at date: "+training.getEndDate());

        //Clear training to be done
        prefsEditor.putString(StaticVariables.KEY_TRAINING_TO_BE_DONE, "");
        prefsEditor.apply();

        //Start Main Activity again
        Intent intent = new Intent(TrainingActivity.this, MainActivity.class);
        startActivity(intent);

        Utilities.showConfirmation(this, "Entrenamiento completado correctamente",
                ConfirmationActivity.SUCCESS_ANIMATION);

        finish();
    }

    private void saveExerciseData (long timeElapsed) {
        currentExercise.setEndTime(System.currentTimeMillis());

        if (currentExercise.getType() == Exercise.TYPE_RUNNING) {
            currentExercise.getRunning().setDistanceR(totalDistance);
            currentExercise.getRunning().setTimeR(timeElapsed/1000); //Time is saved in seconds
            currentExercise.setHeartRateList(heartRateList);
            currentExercise.setGPSLocationsList(GPSLocationsList);
            currentExercise.setAccDataList(accDataList);
            exercisesCompleted.add(currentExercise);
        } else if (currentExercise.getType() == Exercise.TYPE_REST) {
            currentExercise.getRest().setRestr((int) (timeElapsed/1000));
            currentExercise.setHeartRateList(heartRateList);
            exercisesCompleted.add(currentExercise);
        }
    }

    /* Methods for internal sensors */

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //Save acc data every 1 second using a time as reference
                if (!sessionPaused && (System.currentTimeMillis() - accReferenceTimeStamp) > 1000) {
                    ACCData accData = new ACCData();
                    accData.setxValue(event.values[0]);
                    accData.setyValue(event.values[1]);
                    accData.setzValue(event.values[2]);
                    accData.setTimeStamp(System.currentTimeMillis());
                    accDataList.add(accData);
                    accReferenceTimeStamp = System.currentTimeMillis();
                }
                break;
            case Sensor.TYPE_HEART_RATE:
                startChronometerAndUpdateInfo(chronoAllowedToStart, SystemClock.elapsedRealtime());
                chronoAllowedToStart = false;
                pauseActionImgView.setOnClickListener(TrainingActivity.this);

                float heartRateFloat = event.values[0];
                int heartRateInt = Math.round(heartRateFloat);
                long timeMark = SystemClock.elapsedRealtime() - chronometer.getBase();
                heartRateTextView.setText(Integer.toString(heartRateInt));

                if (!sessionPaused) {
                    checkHeartRate(heartRateInt);
                    saveHeartRate(timeMark, heartRateInt);
                }
                break;
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Nothing to do here.
    }
}
