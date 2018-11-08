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

import java.util.ArrayList;

public class TrackerService extends Service implements LocationListener {
    private int state, calories, sportActivity;
    private long duration;
    private double distance, pace;
    private ArrayList<Location> positionList;
    private ArrayList<Integer> speedList;

    private LocationManager locationManager;

    private final String ACTION_TICK = "sk.tuke.smart.makac.TICK";

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 10, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("sk.tuke.smart.makac.COMMAND_START") || intent.getAction().equals("sk.tuke.smart.makac.COMMAND_CONTINUE")) {
//                ACTION_TICK 1s interval broadcast
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
