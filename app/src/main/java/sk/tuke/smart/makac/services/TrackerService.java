package sk.tuke.smart.makac.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

import sk.tuke.smart.makac.exceptions.InsufficientDistanceException;
import sk.tuke.smart.makac.exceptions.NotEnoughLocationsException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.SportActivities;

public class TrackerService extends Service implements LocationListener {
    private final float WEIGHT = 80;
    private int state = IntentHelper.STATE_STOPPED;
    private int sportActivity;
    private double calories;
    private long duration;
    private double distance;
    private double pace;
    private Float speed;

    private boolean hasContinued;
    private Location previousPosition;

    private ArrayList<Location> positionList = new ArrayList<>();
    private ArrayList<Float> speedList = new ArrayList<>();

    private LocationManager locationManager;
    private Handler handler = new Handler();
    private final String TAG = "TrackerService";

    private final int MIN_TIME_BTW_UPDATES = 3000;
    private final int MIN_DISTANCE = 10;
    private final int PACE_UPDATE_LIMIT = MIN_TIME_BTW_UPDATES / 1000 * 2;
    private int lastLocationUpdateBefore = 0;
    private boolean locationUpdateReceived;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            duration += 1;
            lastLocationUpdateBefore += 1;

            verifyPace();

            Intent intent = createBroadcastIntent();
            sendBroadcast(intent);
            Log.i(TAG, "Broadcast intent with action TICK sent.");

            handler.postDelayed(this, 1000);
        }

        private Intent createBroadcastIntent() {
            return new Intent().setAction(IntentHelper.ACTION_TICK)
                    .putExtra(IntentHelper.DATA_DURATION, duration)
                    .putExtra(IntentHelper.DATA_DISTANCE, distance)
                    .putExtra(IntentHelper.DATA_STATE, state)
                    .putExtra(IntentHelper.DATA_POSITIONS, positionList)
                    .putExtra(IntentHelper.DATA_SPORT, sportActivity)
                    .putExtra(IntentHelper.DATA_PACE, pace)
                    .putExtra(IntentHelper.DATA_CALORIES, calories);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAG, "Service created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service is running.");

        handleIntent(intent);
        enableLocationUpdates();

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case IntentHelper.ACTION_START:
                    handler.postDelayed(runnable, 1000);
                    state = IntentHelper.STATE_RUNNING;

                    Log.i(TAG, "Service started.");
                    break;

                case IntentHelper.ACTION_CONTINUE:
                    handler.postDelayed(runnable, 1000);
//                    previousPosition = intent.getParcelableExtra(IntentHelper.DATA_POSITIONS);
                    previousPosition = positionList.isEmpty() ? null : positionList.get(positionList.size() - 1);
                    positionList = new ArrayList<>();
                    hasContinued = true;
                    state = IntentHelper.STATE_CONTINUE;

                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(IntentHelper.ACTION_GPS);
                    sendBroadcast(broadcastIntent);
                    Log.i(TAG, "Broadcast intent with action GPS sent.");

                    Log.i(TAG, "Service is active again.");
                    break;

                case IntentHelper.ACTION_PAUSE:
                    handler.removeCallbacks(runnable);
                    state = IntentHelper.STATE_PAUSED;

                    Log.i(TAG, "Service paused.");
                    break;

                case IntentHelper.ACTION_STOP:
                    handler.removeCallbacks(runnable);
                    state = IntentHelper.STATE_STOPPED;

                    Log.i(TAG, "Stopping service.");
                    stopSelf();
            }
        }
    }

    private void enableLocationUpdates() {
        if (state == IntentHelper.STATE_RUNNING || state == IntentHelper.STATE_CONTINUE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BTW_UPDATES, MIN_DISTANCE, this);
                Log.i(TAG, "Location updates requested.");
            }
            else
                Log.e(TAG, "Location updates not requested due to missing permissions.");
        }
        else
            Log.i(TAG, "Location updates not requested due to different state.");
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
        return new Binder();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");

        if (state != IntentHelper.STATE_PAUSED) {
            location.setTime(new Date().getTime());
            positionList.add(location);

            try {
                countDistance();
                countSpeed();
                countPace();
                if (speedList.size() > 1) {
                    Log.i(TAG, "Counting calories.");
                    calories = SportActivities.countCalories(sportActivity, WEIGHT, speedList, duration / 60 / 60);
                }
                else
                    Log.i(TAG, "Skipping calories calculation.");
            }
            catch (InsufficientDistanceException ide) {
                Log.w(TAG, "Location not updated because distance between last 2 locations was less than 2 meters.");
                positionList.remove(positionList.size() - 1);
                locationUpdateReceived = false;
                Log.w(TAG, "Last location removed.");
            }
            catch (NotEnoughLocationsException nele) {
                locationUpdateReceived = false;
                Log.w(TAG, "Distance not updated because there are not enough locations to count from.");
            }
        }
        else
            Log.i(TAG, "Location ignored because of the current state.");
    }

    private void countDistance() throws InsufficientDistanceException, NotEnoughLocationsException {
        if (positionList.size() > 1) {
            double newDistance = calculateNewDistance(positionList.get(positionList.size() - 1), positionList.get(positionList.size() - 2));
            validateNewDistance(newDistance);
            return;
        }
        else if (hasContinued) {
            if (previousPosition != null)
                distance += additionalDistanceAfterContinue();
            else
                Log.i(TAG, "There was no previous position. Skipping additional distance.");
            hasContinued = false;
        }

        throw new NotEnoughLocationsException();
    }

    private void validateNewDistance(double newDistance) throws InsufficientDistanceException {
        if (newDistance >= 2) {
            distance += newDistance;
            locationUpdateReceived = true;
            Log.i(TAG, "Distance counted. (" + distance + "m)");
            return;
        }

        throw new InsufficientDistanceException();
    }

    private float calculateNewDistance(Location a, Location b) {
//        a.setLatitude(a.getLatitude());
//        a.setLongitude(a.getLongitude());
//
//        b.setLatitude(b.getLatitude());
//        b.setLongitude(b.getLongitude());
        return a.distanceTo(b);
    }

    private double additionalDistanceAfterContinue() {
        double newDistance = calculateNewDistance(positionList.get(positionList.size() - 1), previousPosition);
        if (newDistance <= 100) {
            Log.i(TAG, "Distance was added after unpausing.");
            return newDistance;
        }

        Log.i(TAG, "Distance is too big to be added after unpausing.");
        return 0;
    }

    private void countSpeed() {
        speed = (float)distance / (float)duration;
        speedList.add(speed);
        Log.i(TAG, "Speed counted. (" + speed + "m/s)");
    }

    private void verifyPace() {
        if (!locationUpdateReceived && lastLocationUpdateBefore > PACE_UPDATE_LIMIT) {
            pace = 0.0;
            Log.i(TAG, "Pace set to 0.0km/h due to overcome time limit.");
        }
        else if (locationUpdateReceived) {
            locationUpdateReceived = false;
            lastLocationUpdateBefore = 0;
            Log.i(TAG, "Location received. Variables reset.");
        }
    }

    private void countPace() {
        pace = speed * 60 / 1000;
        Log.i(TAG, "Pace counted. (" + pace + "km/min)");
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

        // TODO onStatusChanged()

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

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
