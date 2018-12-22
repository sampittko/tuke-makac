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
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.tuke.smart.makac.exceptions.InsufficientDistanceException;
import sk.tuke.smart.makac.exceptions.NotEnoughLocationsException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.SportActivities;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class TrackerService extends Service implements LocationListener {
    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "TrackerService";
    private final float WEIGHT = 80;
    private final int MIN_TIME_BTW_UPDATES = 3000;
    private final int MIN_DISTANCE = 10;
    private final int PACE_UPDATE_LIMIT = MIN_TIME_BTW_UPDATES / 1000 * 2;

    private int state = IntentHelper.STATE_STOPPED;
    private int sportActivity = IntentHelper.ACTIVITY_RUNNING;
    private double calories, previousCalories, totalCalories, distance, pace;
    private long duration;
    private float speed;

    private boolean hasContinued, locationUpdateReceived;

    private Location previousPosition;
    private Location currentLocation;

    private Date firstSpeedTime;

    private ArrayList<Float> speedList = new ArrayList<>();
    private ArrayList<Location> positionList = new ArrayList<>();

    private Dao<GpsPoint, Long> gpsPointDao;
    private Dao<Workout, Long> workoutDao;

    private LocationManager locationManager;

    private Handler handler = new Handler();

    private DatabaseHelper databaseHelper;

    private Workout pendingWorkout;

    private long sessionNumber;

    private int lastLocationUpdateBeforeSeconds, workoutDataSavedAtSeconds;

    public class LocalBinder extends Binder {
        public TrackerService getService() {
            return TrackerService.this;
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            duration += 1000;
            lastLocationUpdateBeforeSeconds += 1;

            performCheck();

            sendBroadcast(createBroadcastIntent());
            Log.i(TAG, "Broadcast intent with action TICK sent.");

            currentLocation = null;

            handler.postDelayed(this, 1000);
        }

        private Intent createBroadcastIntent() {
            return new Intent().setAction(IntentHelper.ACTION_TICK)
                    .putExtra(IntentHelper.DATA_DURATION, getSecondsDuration())
                    .putExtra(IntentHelper.DATA_DISTANCE, distance)
                    .putExtra(IntentHelper.DATA_STATE, state)
                    .putExtra(IntentHelper.DATA_LOCATION, currentLocation)
                    .putExtra(IntentHelper.DATA_SPORT, sportActivity)
                    .putExtra(IntentHelper.DATA_PACE, pace)
                    .putExtra(IntentHelper.DATA_CALORIES, calories);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        checkLocationPermissions();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        databaseSetup();
        createNewWorkout();

        Log.i(TAG, "Service created.");
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "Creating service with missing permissions.");
            Toast.makeText(this, "Permissions for GPS are missing.", Toast.LENGTH_LONG).show();
        }
    }

    private void databaseSetup() {
        databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        try {
            gpsPointDao = databaseHelper.gpsPointDao();
            gpsPointDao.delete(gpsPointDao.queryForAll());

            workoutDao = databaseHelper.workoutDao();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createNewWorkout() {
        List<Workout> workouts = null;

        try {
            workouts = databaseHelper.workoutDao().queryForAll();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }

        if (workouts != null) {
            int workoutId;
            String workoutTitle;

            if (workouts.size() == 0)
                workoutId = 1;
            else
                workoutId = workouts.size() + 1;

            workoutTitle = "Workout " + workoutId;
            pendingWorkout = new Workout(workoutTitle, sportActivity);
            pendingWorkout.setCreated(new Date());

            try {
                workoutDao.create(pendingWorkout);
                Log.i(TAG, "Workout with ID " + workoutId + " created.");
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
                    performStartAction();
                    break;
                case IntentHelper.ACTION_CONTINUE:
                    performContinueAction();
                    break;
                case IntentHelper.ACTION_PAUSE:
                    performPauseAction();
                    break;
                case IntentHelper.ACTION_STOP:
                    performStopAction();
                    break;
                default:
                    Log.i(TAG, "There is no specific task to process in service.");
                    break;
            }
        }
    }

    private void performStartAction() {
        handler.postDelayed(timerRunnable, 1000);
        state = IntentHelper.STATE_RUNNING;
        sessionNumber = 1;
        Log.i(TAG, "Service started.");
    }

    private void performContinueAction() {
        handler.postDelayed(timerRunnable, 1000);
        previousPosition = positionList.isEmpty() ? null : positionList.get(positionList.size() - 1);
        positionList = new ArrayList<>();
        speedList = new ArrayList<>();
        hasContinued = true;
        state = IntentHelper.STATE_CONTINUE;
        sessionNumber++;
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(IntentHelper.ACTION_GPS);
        sendBroadcast(broadcastIntent);
        Log.i(TAG, "Broadcast intent with action GPS sent.");
        Log.i(TAG, "Service is active again.");
    }

    private void performPauseAction() {
        handler.removeCallbacks(timerRunnable);
        state = IntentHelper.STATE_PAUSED;
        previousCalories += calories;
        Log.i(TAG, "Service paused.");
    }

    private void performStopAction() {
        handler.removeCallbacks(timerRunnable);
        state = IntentHelper.STATE_STOPPED;
        Log.i(TAG, "Stopping service.");
        stopSelf();
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
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");

        if (state != IntentHelper.STATE_PAUSED) {
            location.setTime(new Date().getTime());
            positionList.add(location);
            currentLocation = location;

            try {
                countDistance();
                countSpeed();
                countPace();
                calories = SportActivities.countCalories(sportActivity, WEIGHT, speedList, getTimeFillingSpeedListInHours());
                countTotalCalories();
                persistGpsPoint();
            }
            catch (InsufficientDistanceException ide) {
                Log.w(TAG, "Location not updated because distance between last 2 locations was less than 2 meters.");
                positionList.remove(positionList.size() - 1);
                currentLocation = null;
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

    private void countTotalCalories() {
        totalCalories = previousCalories + calories;
        Log.i(TAG, "Total calories calculated. (" + totalCalories + ")");
    }

    private void persistGpsPoint() {
        GpsPoint currentGpsPoint = new GpsPoint(pendingWorkout, sessionNumber, currentLocation, duration, speed, pace, totalCalories);
        try {
            gpsPointDao.create(currentGpsPoint);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performCheck() {
        verifyVariables();
        verifyPace();
        verifyWorkoutData();
    }

    private void verifyVariables() {
        if (locationUpdateReceived) {
            locationUpdateReceived = false;
            lastLocationUpdateBeforeSeconds = 0;
            Log.i(TAG, "Location received. Variables reset.");
        }
    }

    private void verifyPace() {
        if (!locationUpdateReceived && lastLocationUpdateBeforeSeconds > PACE_UPDATE_LIMIT) {
            pace = 0.0;
            Log.i(TAG, "Pace set to 0.0km/h due to overcome time limit.");
        }
    }

    private void verifyWorkoutData() {
        if (lastLocationUpdateBeforeSeconds > 10 && lastLocationUpdateBeforeSeconds - workoutDataSavedAtSeconds == 10) {
            Log.i(TAG, "No new location received for " + lastLocationUpdateBeforeSeconds + " seconds.");
            saveWorkoutData();
            workoutDataSavedAtSeconds = lastLocationUpdateBeforeSeconds;
        }
        else
            Log.i(TAG, "Saving workout data is not required.");
    }

    private void saveWorkoutData() {
        try {
            pendingWorkout.setTotalCalories(totalCalories);
            pendingWorkout.setDistance(distance);
            pendingWorkout.setDuration(duration);
            pendingWorkout.setLastUpdate(new Date());
            pendingWorkout.setPaceAvg(getAveragePace());
            pendingWorkout.setStatus(getWorkoutStatus());
            databaseHelper.workoutDao().update(pendingWorkout);
            Log.i(TAG, "Workout data saved to database.");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double getAveragePace() {
        ArrayList<Double> paceList = new ArrayList<>();
        try {
            List<GpsPoint> gpsPoints = gpsPointDao.queryForAll();
            for (GpsPoint gpsPoint : gpsPoints)
                paceList.add(gpsPoint.getPace());
            return SportActivities.getAveragePace(paceList);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int getWorkoutStatus() {
        switch (state) {
            case IntentHelper.STATE_CONTINUE:
                return Workout.statusUnknown;
            case IntentHelper.STATE_PAUSED:
                return Workout.statusPaused;
            case IntentHelper.STATE_RUNNING:
                return Workout.statusUnknown;
            case IntentHelper.STATE_STOPPED:
                return Workout.statusEnded;
            default:
                return Workout.statusUnknown;
        }
    }

    private void countDistance() throws InsufficientDistanceException, NotEnoughLocationsException {
        if (positionList.size() > 1) {
            double newDistance = calculateNewDistance(positionList.get(positionList.size() - 1), positionList.get(positionList.size() - 2));
            validateNewDistance(newDistance);
        }
        else if (hasContinued) {
            if (previousPosition != null)
                distance += additionalDistanceAfterContinue();
            else
                Log.i(TAG, "There was no previous position. Skipping additional distance.");
            hasContinued = false;
        }
        else
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
        return a.distanceTo(b);
    }

    private double additionalDistanceAfterContinue() {
        double newDistance = calculateNewDistance(positionList.get(positionList.size() - 1), previousPosition);
        if (newDistance <= 100) {
            Log.i(TAG, "Distance was updated after unpausing.");
            return newDistance;
        }

        Log.i(TAG, "Distance is too big to be added after unpausing.");
        return 0;
    }

    private void countSpeed() {
        speed = (float)distance / (float)getSecondsDuration();
        speedList.add(speed);
        if (speedList.size() == 1)
            firstSpeedTime = new Date();
        Log.i(TAG, "Speed counted. (" + speed + "m/s)");
    }

    private double getTimeFillingSpeedListInHours() {
        Date lastSpeedTime = new Date();
        return ((lastSpeedTime.getTime() - firstSpeedTime.getTime()) / 3.6) / 1000000;
    }

    private void countPace() {
        pace = 1000 / speed;
        Log.i(TAG, "Pace counted. (" + pace + "min/km)");
    }

    private long getSecondsDuration() {
        return duration / 1000;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        //
    }

    @Override
    public void onProviderEnabled(String s) {
        //
    }

    @Override
    public void onProviderDisabled(String s) {
        //
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
