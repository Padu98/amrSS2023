package de.pbma.moa.amr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorHelper implements SensorEventListener {

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    float lastAzimuth = 0;
    float oldRoll = 0;
    private final float[] lastAccelerometerValues = new float[3];
    private final float[] lastMagnetometerValues = new float[3];
    private final MQTTLogic mqttLogic;
    private boolean mqttReady;


    public SensorHelper(SensorManager sm) {
        mqttLogic = new MQTTLogic();
        mqttLogic.registerMQTTListener(mqttListener);
        mqttLogic.connect();
        Sensor accelerometerSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometerSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private final MQTTListener mqttListener = new MQTTListener() {
        @Override
        public void onConnected() {
            mqttReady = true;
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        new Thread(()->{
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, lastAccelerometerValues, 0, event.values.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, lastMagnetometerValues, 0, event.values.length);
            }

            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometerValues, lastMagnetometerValues);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            sendVerticalDirection();
            if (phoneIsVertical()) {
                float azimuth = (float) Math.toDegrees(orientationAngles[0]);
                if (azimuth < 0) {
                    azimuth += 360;
                }
                float deltaAzimuth = Math.abs(azimuth - lastAzimuth);
                if (deltaAzimuth > 15.0f) { // Threshold for detecting a significant change
                    sendHorizontalDelta(azimuth);
                    lastAzimuth = azimuth;
                }
            }
        }).start();
    }

    private boolean phoneIsVertical() {
        float pitch = (float) Math.toDegrees(orientationAngles[1]); // Neigung in Grad
        float roll = (float) Math.toDegrees(orientationAngles[2]); // Rollen in Grad

        // Überprüfen, ob das Gerät flach liegt (nahezu parallel zum Boden)
        return Math.abs(pitch) < 15 && Math.abs(roll) > 75 && Math.abs(roll) < 105;
    }

    private void sendHorizontalDelta(float azimuth) {
        if(!mqttReady){
            Log.e("Test", "mqtt is not ready jet!");
            return;
        }
        float realDelta = azimuth - lastAzimuth;
        int valToSend = deltaToValue(Math.abs(realDelta));
        if(realDelta<0){
            valToSend *= -1;
            JSONObject json = new JSONObject();
            try {
                json.put("horizontal", valToSend);
                mqttLogic.send("amr/data", json.toString());
            }catch (JSONException ex){
                Log.e("Test", "json fail");
            }
        }
    }

    private void sendVerticalDirection(){
        if(!mqttReady){
            Log.e("Test", "mqtt is not ready jet!");
            return;
        }
        float pitch = (float) Math.toDegrees(orientationAngles[1]); // Neigung in Grad
        float roll = (float) Math.toDegrees(orientationAngles[2]); // Rollen in Grad
        if(Math.abs(pitch) < 15 && (roll < oldRoll-15 || roll > oldRoll +15)){
            oldRoll = roll;
            JSONObject json = new JSONObject();
            try {
                json.put("vertical",  roll);
                mqttLogic.send("amr/data", json.toString());
            }catch (JSONException ex){
                Log.e("Test", "json fail");
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private int deltaToValue(float del){
        if(del < 20){
            return 1;
        } else if (del < 25) {
            return 2;
        } else if (del < 30) {
            return 3;
        } else if(del < 40){
            return 4;
        }
        return 0; //abnorme werte aussortieren
    }
}



       /* if (azimuth >= 337.5 || azimuth < 22.5) {
            return "N";
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            return "NE";
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            return "E";
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            return "SE";
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            return "S";
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            return "SW";
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            return "W";
        } else if (azimuth >= 292.5 && azimuth < 337.5) {
            return "NW";
        } else {
            return "";
        }*/
