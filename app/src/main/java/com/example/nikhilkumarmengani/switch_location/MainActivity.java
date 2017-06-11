package com.example.nikhilkumarmengani.switch_location;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.icu.text.DisplayContext;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.cos;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private TextView latitude_, longitude_,provider_type;
    private LocationManager location_manager;
    private Float accuracy_GPS=0.0f, accuracy_NETWORK=0.0f;
    private Location loc_GPS, loc_NETWORK;
    private int flag=0;
    private MyService myService_;
    private Handler myHandler = new Handler();
    private Handler myHandler1 = new Handler();
    private Button myButton;
    private TextView test;
    private Timer timer_ = null;
    private  boolean bound= false;
    private float position[] = new float[]{0,0} ;
    private int setInitLoc =0;
    private TextView  method2Lat,method2Long, error_, velocity_, distance_moved, bearing_cal;
    private Location bestLoc=null ;
    private int bestLocattionSet=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        location_manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        latitude_ = (TextView) findViewById(R.id.textView6);
        longitude_ = (TextView) findViewById(R.id.textView7);
        provider_type = (TextView)findViewById(R.id.textView10);
        method2Lat = (TextView) findViewById(R.id.textView14);
        method2Long=(TextView)findViewById(R.id.textView15);

        error_ = (TextView)findViewById(R.id.textView19);




    }


     /* This method startMe() is to start the timer . Timer gets the distance travelled  and  direction  for interval of time and  */

    public void startMe() {


        if(timer_==null)
        {
            timer_ = new Timer();
            Log.d("100","error" );
            timer_.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    myHandler1.post(new Runnable() {
                        @Override
                        public void run() {
                            float distance = myService_.getDistance(7);
                            float bearing  = myService_.getBearing();
                            float init_vel=myService_.getInitial_vel();
                            float rms = myService_.getRMS();

                            position = getCoordinates(distance,bearing);
                            method2Lat.setText(position[0]+"");
                            method2Long.setText(position[1]+"");
                            float errordistance = errorInPosition(position[0],position[1]);
                            error_.setText(errordistance+"");
                        }
                    });
                }
            }, 5000, 3000);

        }


    }

    /***
     * This method is to calculate the error in position using haversign formula
     * @param lat2
     * @param long2
     * @return
     */
    public float errorInPosition(float lat2,float long2)
    {
        float lat1,long1,dlat,dlong,errdistance;
        float R = 6378100f;
        if(bestLocattionSet==1) {
            synchronized (bestLoc) {
                lat1 = (float) bestLoc.getLatitude();
                long1 = (float) bestLoc.getLongitude();
            }
            lat1 = (float)Math.toRadians(lat1);
            long1 = (float)Math.toRadians(long1);
            lat2 = (float)Math.toRadians(lat2);
            long2 = (float)Math.toRadians(long2);

            dlat = lat2 - lat1;
            dlong = long2 - long1;
            float a = (float) (Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlong / 2)* Math.sin(dlong/2));
            float c = ((float) Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
            c = c * 2.0f;
            Log.d("100",a+","+ c);
            errdistance = R * c;
            return errdistance;
        }
        return 1.0f;
    }


    /***
     * this method is used to calculate the new latitude and new longitude position from distance and bearing
     * @param distance
     * @param bearing
     * @return
     */
    public float[] getCoordinates(float distance, float bearing)
    {
        float d = distance/1000;
        float R = 6378.1f;
        float new_lat, new_long, old_lat = position[0], old_long = position[1];
        if(setInitLoc!=1)
        {
            Toast.makeText(getApplicationContext(),"Initial location for method 2 is not set",Toast.LENGTH_LONG).show();
        }
        else {

            old_lat = (float) Math.toRadians(old_lat);
            old_long = (float) Math.toRadians(old_long);
            new_lat = (float) Math.asin((Math.sin(old_lat) * cos(d / R)) +
                    (cos(old_lat) * Math.sin(d / R) * cos(bearing)));
            new_long = old_long + (float) Math.atan2((Math.sin(bearing) * Math.sin(d / R) * cos(old_lat)),
                    (cos(d / R) - (Math.sin(old_lat) * Math.sin(new_lat))));
            position[0] = (float) Math.toDegrees(new_lat);
            position[1] = (float) Math.toDegrees(new_long);
        }
        return position;





    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,this);
        location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);
    }

    /***
     * handler for avoiding the race conditions of accelerometer values
     */


    class Handler_class implements Runnable
    {
        Float lat_,long_;
        String type_;
        public  Handler_class(Float lat_,Float long_,String type_)
            {
                this.lat_ = lat_;
                this.long_ = long_;
                this.type_ = type_;


            }

        @Override
        public void run() {
            if(setInitLoc==0 )
            {
                position[0] = lat_;
                position[1] = long_;
                setInitLoc =1;

            }


            latitude_.setText(lat_.toString());
            longitude_.setText(long_.toString());
            provider_type.setText(type_);


        }
    }


    @Override
    public void onLocationChanged(Location location) {

        if(bestLoc==null){
            bestLoc = location;
            Intent myIntent = new Intent(this,MyService.class);
            bindService(myIntent,serviceConnection_, Context.BIND_AUTO_CREATE);

        }
        if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            accuracy_GPS = location.getAccuracy();
            loc_GPS = location;
        }
        else {
            accuracy_NETWORK = location.getAccuracy();
            loc_NETWORK = location;
        }
        if(accuracy_GPS < accuracy_NETWORK && accuracy_GPS!=0.0)
        {
            if(flag==0)
            {
                //Toast.makeText(this, "Switched to GPS", Toast.LENGTH_LONG).show();
                flag=1;
            }

            if(bestLocattionSet==0) {
                synchronized (bestLoc) {
                    bestLoc = loc_GPS;
                    bestLocattionSet =1;
                }

            }
                myHandler.post(new Handler_class(new Float(loc_GPS.getLatitude()), new Float(loc_GPS.getLongitude()), "GPS"));

        }
        else
        {
            if(flag==0)
            {
                //Toast.makeText(this, "switched to NETWORK", Toast.LENGTH_LONG).show();
                flag=1;
            }

            if(bestLocattionSet==0) {
                synchronized (bestLoc) {
                    bestLoc = loc_NETWORK;
                    bestLocattionSet =1;
                }

            }
                myHandler.post(new Handler_class(new Float(loc_NETWORK.getLatitude()), new Float(loc_NETWORK.getLongitude()), "NETWORK"));
        }



    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /***
     * service connection
     */
    ServiceConnection serviceConnection_ = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
                MyService.MyBinder  binder_ = (MyService.MyBinder) service;
                myService_ = binder_.getService();
                bound = true;
                startMe();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
              bound =false;
        }
    };

}
