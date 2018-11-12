package sk.tuke.smart.makac.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

import sk.tuke.smart.makac.StopwatchActivity;
import sk.tuke.smart.makac.helpers.IntentCommands;

public class TrackerService extends Service implements LocationListener {
    private int state;
    private int calories;
    private int sportActivity;
    private long duration;
    private double distance;
    private double pace;
    private ArrayList<Location> positionList;
    private ArrayList<Integer> speedList;

    private Thread intervalThread;
    private LocationManager locationManager;

    private TrackerService trackerService;
    private final String TAG = "TrackerService";

    @Override
    public void onCreate() {
        super.onCreate();
        trackerService = this;

        // REQUEST LOCATION UPDATES
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 10, this);

        // CREATE THREAD FOR 1S INTERVAL
        intervalThread = new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    Thread.sleep(1000);
                    Intent intent = new Intent(getApplicationContext(), StopwatchActivity.class);
                    intent.setAction(IntentCommands.ACTION_TICK);
                    sendBroadcast(intent);
                }
                catch(InterruptedException e) {
                    //
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service running.");
        if (intent.getAction() != null) {
            if (intent.getAction().equals(IntentCommands.ACTION_START) || intent.getAction().equals(IntentCommands.ACTION_CONTINUE)) {
                intervalThread.run();
                Log.i(TAG, "Interval thread is running.");
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void countCalories() {
        if (speedList.size() >= 2) {

        }
    }

    public int getState() {
        return state;
    }

    public long getDuration() {
        return duration;
    }

    public double getDistance() {
        return distance;
    }

    public double getPace() {
        return pace;
    }
}
