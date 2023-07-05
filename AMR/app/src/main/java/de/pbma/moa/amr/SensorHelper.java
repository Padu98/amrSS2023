package de.pbma.moa.amr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class SensorHelper implements SensorEventListener {

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    float lastAzimuth = 0;
    float oldRoll = 0;
    private final float[] lastAccelerometerValues = new float[3];
    private final float[] lastMagnetometerValues = new float[3];
    private final MQTTLogic mqttLogic;
    private boolean mqttReady;
    private final SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor derCoolereMagnetsensor;
    private float oldAzimuthDegrees = 0;
    private float oldRollDegrees = 0;

    public SensorHelper(SensorManager sm, Context ctx) {
        sensorManager = sm;
        mqttLogic = new MQTTLogic(ctx);
        mqttLogic.registerMQTTListener(mqttListener);
        mqttLogic.connect();
    }

    public void registerSensorListener() {
       // accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        derCoolereMagnetsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR); //TYPE_MAGNETIC_FIELD
        //sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, derCoolereMagnetsensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterSensorListener() {
        sensorManager.unregisterListener(this, accelerometerSensor);
        sensorManager.unregisterListener(this, derCoolereMagnetsensor);
    }

    private final MQTTListener mqttListener = new MQTTListener() {
        @Override
        public void onConnected() {
            mqttReady = true;
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Arrays.equals(event.values, lastAccelerometerValues) || Arrays.equals(event.values, lastMagnetometerValues)) {
            //Log.e("Test", "same values");
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) { //TYPE_MAGNETIC_FIELD
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float azimuthDegrees = (float) Math.toDegrees(orientationAngles[0]);
        if (azimuthDegrees < 0) {
            azimuthDegrees += 360;
        }
        //float pitchDegrees = (float) Math.toDegrees(orientationAngles[1]);
        float rollDegrees = (float) Math.toDegrees(orientationAngles[2]);

        if(oldAzimuthDegrees == 0 && oldRollDegrees == 0){
            oldAzimuthDegrees = azimuthDegrees;
            oldRollDegrees = rollDegrees;
        }
        if (azThresholdIsValid(oldAzimuthDegrees, azimuthDegrees)) {
            oldAzimuthDegrees = azimuthDegrees;
            sendValue(azimuthDegrees, "horizontal");
            Log.e("test", "send hor");
        }

        if (rollThresholdIsValid(oldRollDegrees, rollDegrees)) {
            oldRollDegrees = rollDegrees;
            sendValue(rollDegrees, "vertical");
            Log.e("test", "send hor");
        }
    }}

    private boolean azThresholdIsValid(float old, float neu) {
        float delta = old - neu;
        int threshold = 2;
        if (Math.abs(delta) > 250) {
            if (old > neu) {
                delta = 360 - old + neu;
            } else {
                delta = old + 360 - neu;
            }
        }
        if (delta > threshold) {
            return true;
        }
        return true;
    }

    private boolean rollThresholdIsValid(float old, float neu) {
        float delta = old - neu;
        int threshold = 2;
        if (Math.abs(delta) > 250) {
            if (old > neu) {
                delta = 360 - old  - Math.abs(neu);
            }else{
                delta = 360 - Math.abs(old) + neu;
            }
        }
        if (delta > threshold) {
            return true;
        }
        return true;
    }

    private void sendValue(float val, String top) {
        if (!mqttReady) {
            Log.e("Test", "mqtt is not ready jet!");
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put(top, val);
            mqttLogic.send("amr/data", json.toString());
        } catch (JSONException ex) {
            Log.e("Test", "json exceptioin azimuth");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
