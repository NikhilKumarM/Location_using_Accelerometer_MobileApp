package com.example.nikhilkumarmengani.switch_location;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.lang.Math.*;
public class MyService extends Service implements SensorEventListener {

    MyBinder mybinder_ = new MyBinder();
    DecimalFormat df = new DecimalFormat("##.#");
    private static final float EPSILON = 0.01f ;
    Sensor  accelerometer_ , gyroscope_;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector = new float[4];
    private float timestamp;
    private float [] rotationCurrent = {1,0,0,0,1,0,0,0,1};
    Float bearing=0.0f ;
    float RMS=0.0f;
    float prev_RMS= 0.0f;
    Queue<SensorEvent>    myqueue_ = new LinkedList<SensorEvent>();
    public float initial_vel=0.0f;
    float orientation[] = new float[3];
    float gx_=0.0f,gy_=0.0f,gz_=0.0f;
    int deconsider=0;


    public class MyBinder extends Binder {

        MyService getService()
        {

            return  MyService.this;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

        SensorManager sensorManager_acc = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer_ = sensorManager_acc.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager_acc.registerListener(this, accelerometer_,SensorManager.SENSOR_DELAY_NORMAL);
        SensorManager sensorManager_gyro = (SensorManager)getSystemService(SENSOR_SERVICE);
        gyroscope_ =  sensorManager_gyro.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager_gyro.registerListener(this, gyroscope_,SensorManager.SENSOR_DELAY_FASTEST);


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

           return mybinder_;
    }


    public float getInitial_vel() {
        return initial_vel;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType()== Sensor.TYPE_ACCELEROMETER)
        {
          synchronized (myqueue_) {
              myqueue_.add(event);
          }


        }
        if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) (thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            rotationCurrent = matrixMultiply(rotationCurrent, deltaRotationMatrix);

                SensorManager.getOrientation(rotationCurrent, orientation);

                 //  bearing from 0-360 degrees
            synchronized (bearing) {
                bearing = (float) Math.toDegrees(orientation[0]);
                if (bearing < 0) {
                    bearing += 360;
                }
            }
        }



    }

    /***
     * This method calculates the distance by using accelerometer values. Filtering is also performed in this method.
     * @param time
     * @return
     */
    public float getDistance(float time)
    {

        float distance =-1.0f;
        float acc = 1;
        int size_;
        float alpha =0.95f;
        RMS=0.0f;
        int count=0;
        ArrayList<Float> filter_ = new ArrayList<Float>();

        synchronized (myqueue_) {
            size_ = myqueue_.size();
            float mean =0.0f;
            if(prev_RMS==0) {
                for (int i = 0; i < size_ / 2; i++) {
                    myqueue_.remove();

                }
            }
            while (!myqueue_.isEmpty()) {

                SensorEvent e = myqueue_.poll();
                float ax_ = e.values[0];
                float ay_ = e.values[1];
                float az_ = e.values[2];
                gx_ = gx_* alpha +(1-alpha)* ax_;
                gy_ = gy_ * alpha +(1-alpha)* ay_;
                gz_= gz_ * alpha + (1-alpha)* az_;
                ax_ = ax_ - gx_;
                ay_ = ay_ - gy_;
                az_ = az_- gz_;
                ax_ = Float.parseFloat(df.format(ax_));
                ay_ = Float.parseFloat(df.format(ay_));
                az_ = Float.parseFloat(df.format(az_));
                float a_mag = (float) Math.sqrt(ax_ * ax_ + ay_ * ay_ + az_ * az_);
                a_mag = Float.parseFloat(df.format(a_mag));
                filter_.add(a_mag);
                RMS =RMS+ a_mag*a_mag;

            }





            if(deconsider<1)
            {
                RMS =0.0f;
                deconsider++;
            }


            RMS = (float) Math.sqrt(RMS);
            RMS = RMS/size_;
            RMS = Float.parseFloat(df.format(RMS));
            if (RMS < prev_RMS) {
                acc = -1;
            }
            Log.d("101","RMS:" +RMS);
            prev_RMS = RMS;

            initial_vel = Float.parseFloat(df.format(initial_vel));
             distance = (initial_vel * time) + (0.5f * (acc) * RMS * time * time);
            initial_vel = Float.parseFloat(df.format(initial_vel));
            if(prev_RMS==0)
            {
                initial_vel =0;
            }
            else {
                initial_vel += acc * RMS * time;
            }
        }
        if (distance<0.0f)
            return 0.0f;
        else
        return distance;
    }

    public float getRMS() {
        return RMS;
    }

    public float getBearing()
    {
             synchronized (bearing) {
                 return bearing;
             }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /***
     * This method is used for the updating the values of RotationMatrix
     * @param rotationCurrent
     * @param deltaRotationMatrix
     * @return
     */
    public float[] matrixMultiply(float[] rotationCurrent, float[] deltaRotationMatrix) {

        float rC []= new float[9];
        rC[0] = rotationCurrent[0]* deltaRotationMatrix[0]+rotationCurrent[1]* deltaRotationMatrix[3]+rotationCurrent[2]* deltaRotationMatrix[6];
        rC[1] = rotationCurrent[0]* deltaRotationMatrix[1]+rotationCurrent[1]* deltaRotationMatrix[4]+rotationCurrent[2]* deltaRotationMatrix[7];
        rC[2] = rotationCurrent[0]* deltaRotationMatrix[2]+rotationCurrent[1]* deltaRotationMatrix[5]+rotationCurrent[2]* deltaRotationMatrix[8];

        rC[3] = rotationCurrent[3]* deltaRotationMatrix[0]+rotationCurrent[4]* deltaRotationMatrix[3]+rotationCurrent[5]* deltaRotationMatrix[6];
        rC[4] = rotationCurrent[3]* deltaRotationMatrix[1]+rotationCurrent[4]* deltaRotationMatrix[4]+rotationCurrent[5]* deltaRotationMatrix[7];
        rC[5] = rotationCurrent[3]* deltaRotationMatrix[2]+rotationCurrent[4]* deltaRotationMatrix[5]+rotationCurrent[5]* deltaRotationMatrix[8];

        rC[6] = rotationCurrent[6]* deltaRotationMatrix[0]+rotationCurrent[7]* deltaRotationMatrix[3]+rotationCurrent[8]* deltaRotationMatrix[6];
        rC[7] = rotationCurrent[6]* deltaRotationMatrix[1]+rotationCurrent[7]* deltaRotationMatrix[4]+rotationCurrent[8]* deltaRotationMatrix[7];
        rC[8] = rotationCurrent[6]* deltaRotationMatrix[2]+rotationCurrent[7]* deltaRotationMatrix[5]+rotationCurrent[8]* deltaRotationMatrix[8];
        //Log.d("100","matrix:"+rotationCurrent);
        return rC;



    }
}
