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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

import sk.tuke.smart.makac.exceptions.InsufficientDistanceException;
import sk.tuke.smart.makac.exceptions.NotEnoughLocationsException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.SportActivities;

public class TrackerService extends Service implements LocationListener {
    private int state = IntentHelper.STATE_STOPPED;
    private int sportActivity;
    private double calories;
    private long duration;
    private double distance;
    private double pace;
    private Float speed;

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

    /**
     *
     */
    private Runnable runnable = new Runnable() {
        /**
         *
         */
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

        /**
         * @return
         */
        private Intent createBroadcastIntent() {
            return new Intent().setAction(IntentHelper.ACTION_TICK)
                    .putExtra(IntentHelper.DATA_DURATION, duration)
                    .putExtra(IntentHelper.DATA_DISTANCE, distance)
                    .putExtra(IntentHelper.DATA_STATE, state)
                    .putExtra(IntentHelper.DATA_POSITIONS, positionList) // TODO NullPointerException
                    .putExtra(IntentHelper.DATA_SPORT, sportActivity)
                    .putExtra(IntentHelper.DATA_PACE, pace)
                    .putExtra(IntentHelper.DATA_CALORIES, calories);
        }
    };

    /**
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAG, "Service created.");
    }

    /**
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service is running.");

        handleIntent(intent);
        enableLocationUpdates();

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * @param intent
     */
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
                    positionList = new ArrayList<>();
                    state = IntentHelper.STATE_CONTINUE;

                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(IntentHelper.ACTION_GPS);
                    sendBroadcast(broadcastIntent);
                    Log.i(TAG, "Broadcast intent with action GPS sent.");
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

    /**
     *
     */
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

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        Log.i(TAG, "Location updates were stopped.");
        Log.i(TAG, "Service was destroyed.");
    }

    /**
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");

        if (state != IntentHelper.STATE_PAUSED) {
            positionList.add(location);

            try {
                countDistance();
                countSpeed();
                countPace();
//                calories = SportActivities.countCalories(sportActivity, IntentHelper.DEFAULT_WEIGHT, speedList, null);
            }
            catch (InsufficientDistanceException ide) {
                Log.w(TAG, "Location not updated because distance between last 2 locations was less than 2 meters.");
                positionList.remove(positionList.size() - 1);
                locationUpdateReceived = false;
                Log.w(TAG, "Last location removed.");
            }
            catch (NotEnoughLocationsException nele) {
                locationUpdateReceived = false;
                Log.w(TAG, "Location not updated because there are not enough locations to count from.");
            }
        }
        else
            Log.i(TAG, "Location ignored because of the current state.");
    }

    /**
     * @throws InsufficientDistanceException
     * @throws NotEnoughLocationsException
     */
    private void countDistance() throws InsufficientDistanceException, NotEnoughLocationsException {
        if (positionList.size() > 1) {
            double newDistance = calculateNewDistance();
            validateNewDistance(newDistance);
            return;
        }

        throw new NotEnoughLocationsException();
    }

    /**
     * @return
     */
    private float calculateNewDistance() {
        Location currentLocation = positionList.get(positionList.size() - 1);
        Location lastLocation = positionList.get(positionList.size() - 2);

        lastLocation.setLatitude(lastLocation.getLatitude());
        lastLocation.setLongitude(lastLocation.getLongitude());

        currentLocation.setLatitude(currentLocation.getLatitude());
        currentLocation.setLongitude(currentLocation.getLongitude());

        return lastLocation.distanceTo(currentLocation);
    }

    /**
     * @param newDistance
     * @throws InsufficientDistanceException
     */
    private void validateNewDistance(double newDistance) throws InsufficientDistanceException {
        if (newDistance >= 2) {
            distance += newDistance;
            locationUpdateReceived = true;
            Log.i(TAG, "Distance counted. (" + distance + "m)");
            return;
        }

        throw new InsufficientDistanceException();
    }

    /**
     *
     */
    private void countSpeed() {
        speed = (float)distance / (float)duration;
        speedList.add(speed);
        Log.i(TAG, "Speed counted. (" + speed + "m/s)");
    }

    /**
     *
     */
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

    /**
     *
     */
    private void countPace() {
        pace = speed * 60 / 1000;
        Log.i(TAG, "Pace counted. (" + pace + "km/min)");
    }

    /**
     * @param s
     * @param i
     * @param bundle
     */
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    /**
     * @param s
     */
    @Override
    public void onProviderEnabled(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void onProviderDisabled(String s) {

    }

    /**
     * @return
     */
    public int getState() {
        return state;
    }

    /**
     * @return
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return
     */
    public double getPace() {
        return pace;
    }
}
