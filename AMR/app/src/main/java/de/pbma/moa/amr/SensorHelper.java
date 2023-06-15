package de.pbma.moa.amr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

public class SensorHelper implements SensorEventListener {
    private SensorManager sensorManager;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final Context ctx;

    private String text = "0";

    float lastAzimuth = 0;
    private float[] lastAccelerometerValues = new float[3];
    private float[] lastMagnetometerValues = new float[3];


    public SensorHelper(SensorManager sm, Context context){
        ctx = context;
        sensorManager = sm;

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometerValues, 0, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometerValues, 0, event.values.length);
        }

        if (lastAccelerometerValues != null && lastMagnetometerValues != null) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometerValues, lastMagnetometerValues);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float azimuth = (float) Math.toDegrees(orientationAngles[0]);
            if (azimuth < 0) {
                azimuth += 360;
            }

            float deltaAzimuth = Math.abs(azimuth - lastAzimuth);
            if (deltaAzimuth > 1.0f) { // Threshold for detecting a significant change
                lastAzimuth = azimuth;
                String heading = getHeadingText(azimuth);
                Log.d("Compass", "Himmelsrichtung: " + heading);
            }
        }
    }

    private String getHeadingText(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) {
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
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
