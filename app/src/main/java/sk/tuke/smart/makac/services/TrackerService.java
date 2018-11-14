package sk.tuke.smart.makac.services;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

import sk.tuke.smart.makac.helpers.IntentHelper;

public class TrackerService extends Service implements LocationListener {
    private int state;
    private int calories;
    private int sportActivity = IntentHelper.ACTIVITY_RUNNING;
    private long duration;
    private double distance;
    private double pace;
    private ArrayList<Location> positionList;
    private ArrayList<Integer> speedList;

    private LocationManager locationManager;

    private Handler handler;
    private Runnable runnable;

    private final String TAG = "TrackerService";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        runnable = new Runnable() {
            Intent intent;

            @Override
            public void run() {
                intent = new Intent().setAction(IntentHelper.ACTION_TICK);
                duration += 1;
                intent.putExtra(IntentHelper.DATA_DURATION, duration);
                sendBroadcast(intent);
                handler.postDelayed(this, 1000);
            }
        };

        // REQUEST LOCATION UPDATES
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 10, this);
            Log.i(TAG, "Location updates requested.");
        }
        else {
            Log.e(TAG, "Location updates not requested.");
            Log.e(TAG, "Service cannot be ran.");
            throw new IllegalStateException();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service running.");
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case IntentHelper.ACTION_START:
                    Log.i(TAG, "Starting service.");
                    handler.postDelayed(runnable, 1000);
                    state = IntentHelper.STATE_RUNNING;
                    break;
                case IntentHelper.ACTION_CONTINUE:
                    Log.i(TAG, "Continuing service.");
                    handler.postDelayed(runnable, 1000);
                    state = IntentHelper.STATE_CONTINUE;
                    break;
                case IntentHelper.ACTION_PAUSE:
                    Log.i(TAG, "Pausing service.");
                    handler.removeCallbacks(runnable);
                    state = IntentHelper.STATE_PAUSED;
                    break;
                case IntentHelper.ACTION_STOP:
                    Log.i(TAG, "Stopping service.");
                    handler.removeCallbacks(runnable);
                    state = IntentHelper.STATE_STOPPED;
                    stopSelf();
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        Log.i(TAG, "Location updates were stopped.");
        Log.i(TAG, "Service was destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");
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
