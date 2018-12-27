package sk.tuke.smart.makac.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import sk.tuke.smart.makac.DatabaseConnection;
import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.exceptions.InsufficientDistanceException;
import sk.tuke.smart.makac.exceptions.NotEnoughLocationsException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.helpers.SportActivities;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.UserProfile;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class TrackerService extends Service implements LocationListener, DatabaseConnection {
    private final double SESSION_DIFF_LIMIT = 100;
    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "TrackerService";
    private final int MIN_TIME_BTW_UPDATES = 3000;
    private final int MIN_DISTANCE = 10;
    private final int PACE_UPDATE_LIMIT = MIN_TIME_BTW_UPDATES / 1000 * 2;

    private int state;
    private int sportActivity;
    private double calories, previousCalories, totalCalories, distance, pace;
    private long duration;
    private float speed;

    private float weight;

    private boolean hasContinued;

    private Location previousLocation;
    private Location currentLocation;

    private Date firstSpeedTime;

    private ArrayList<Float> speedList;
    private ArrayList<Location> positionList;

    private Dao<GpsPoint, Long> gpsPointDao;
    private Dao<Workout, Long> workoutDao;
    private Dao<UserProfile, Long> userProfileDao;

    private LocationManager locationManager;

    private Handler handler;

    private Workout pendingWorkout;

    private long sessionNumber;

    private int lastLocationUpdateBeforeSeconds, workoutDataSavedAtSeconds;

    private SharedPreferences userShPr;

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
            sendBroadcast(createNewIntent());
            currentLocation = null;
            Log.i(TAG, IntentHelper.ACTION_TICK);
            handler.postDelayed(this, 1000);
        }

        private Intent createNewIntent() {
            return new Intent().setAction(IntentHelper.ACTION_TICK)
                    .putExtra(IntentHelper.DATA_WORKOUT_DURATION, MainHelper.msToS(duration))
                    .putExtra(IntentHelper.DATA_WORKOUT_DISTANCE, distance)
                    .putExtra(IntentHelper.DATA_SERVICE_STATE, state)
                    .putExtra(IntentHelper.DATA_WORKOUT_LOCATION, currentLocation)
                    .putExtra(IntentHelper.DATA_WORKOUT_SPORT_ACTIVITY, sportActivity)
                    .putExtra(IntentHelper.DATA_WORKOUT_PACE, pace)
                    .putExtra(IntentHelper.DATA_WORKOUT_CALORIES, calories);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initializeVariables();
        checkLocationPermissions();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        databaseSetup();
        userShPr = getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        Log.i(TAG, "Service created");
    }

    private void initializeVariables() {
        speedList = new ArrayList<>();
        positionList = new ArrayList<>();
        handler = new Handler();
        sportActivity = SportActivities.RUNNING;
        state = IntentHelper.STATE_STOPPED;
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "Creating service with missing permissions");
            Toast.makeText(this, "Permissions for GPS are missing", Toast.LENGTH_LONG).show();
        }
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            gpsPointDao = databaseHelper.gpsPointDao();
            workoutDao = databaseHelper.workoutDao();
            userProfileDao = databaseHelper.userProfileDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service is running");
        if (intent != null)
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
                    Log.w(TAG, "Intent action was not specified or does not correspond to any defined action");
                    break;
            }
        }
    }

    private void performStartAction() {
        createNewWorkout();
        handler.postDelayed(timerRunnable, 1000);
        setWeightAccordingToCurrentUser();
        updateState(IntentHelper.STATE_RUNNING);
        sessionNumber = 1;
        Log.i(TAG, "Service started");
    }

    private void setWeightAccordingToCurrentUser() {
        try {
            List<UserProfile> userProfiles = userProfileDao.queryForEq("user_id", userShPr.getLong(getString(R.string.usershpr_userid), Long.valueOf(getString(R.string.usershpr_userid_default))));
            UserProfile currentUserProfile = userProfiles.get(0);
            if (currentUserProfile.getWeight() <= 0)
                throw new IndexOutOfBoundsException();
            weight = currentUserProfile.getWeight();
            Log.i(TAG, "User weight " + weight + "kg set");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (IndexOutOfBoundsException e) {
            weight = UserProfile.DEFAULT_WEIGHT;
            Log.i(TAG, "Default weight set");
        }
    }

    private void createNewWorkout() {
        List<Workout> workouts = null;

        try {
            workouts = workoutDao.queryForAll();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }

        if (workouts != null) {
            long workoutId = Workout.ID_OFFSET;
            String workoutTitle;

            if (workouts.size() == 0)
                workoutId += 1;
            else
                workoutId += workouts.size() + 1;

            workoutTitle = "Workout " + workoutId;
            pendingWorkout = new Workout(workoutTitle, sportActivity);
            pendingWorkout.setCreated(new Date());

            try {
                workoutDao.create(pendingWorkout);
                Log.i(TAG, "Workout with ID " + workoutId + " created");
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void performContinueAction() {
        handler.postDelayed(timerRunnable, 1000);
        previousLocation = positionList.isEmpty() ? null : positionList.get(positionList.size() - 1);
        positionList = new ArrayList<>();
        speedList = new ArrayList<>();
        hasContinued = true;
        updateState(IntentHelper.STATE_CONTINUE);
        sessionNumber++;
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(IntentHelper.ACTION_GPS);
        sendBroadcast(broadcastIntent);
        Log.i(TAG, IntentHelper.ACTION_GPS);
        Log.i(TAG, "Service resumed");
    }

    private void performPauseAction() {
        if (sessionNumber == 0)
            performWorkoutRecovery();
        else
            saveWorkoutData();
        handler.removeCallbacks(timerRunnable);
        updateState(IntentHelper.STATE_PAUSED);
        previousCalories += calories;
        Log.i(TAG, "Service paused");
    }

    private void performWorkoutRecovery() {
        try {
            pendingWorkout = workoutDao.queryForId(Workout.ID_OFFSET + workoutDao.countOf());
            previousCalories = pendingWorkout.getTotalCalories();
            distance = pendingWorkout.getDistance();
            duration = pendingWorkout.getDuration();
            GpsPoint previousGpsPoint = getPreviousGpsPoint();
            if (previousGpsPoint != null) {
                retrievePreviousLocation(previousGpsPoint);
                sessionNumber = previousGpsPoint.getSessionNumber();
            }
            Log.i(TAG, "Workout recovery successful");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private GpsPoint getPreviousGpsPoint() throws SQLException {
        List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", pendingWorkout.getId());
        if (gpsPoints.size() > 0)
            return gpsPoints.get(gpsPoints.size() - 1);
        else
            return null;
    }

    private void retrievePreviousLocation(GpsPoint previousGpsPoint) throws SQLException {
        List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", pendingWorkout.getId());
        if (gpsPoints.size() > 0) {
            Location retrievedLocation = new Location("");
            retrievedLocation.setLatitude(previousGpsPoint.getLatitude());
            retrievedLocation.setLongitude(previousGpsPoint.getLongitude());
            previousLocation = retrievedLocation;
            Log.i(TAG, "Location was retrieved successfully from last gps point");
        }
        else
            Log.i(TAG, "There is no previous gps point to retrieve last location from");
    }

    private void performStopAction() {
        handler.removeCallbacks(timerRunnable);
        updateState(IntentHelper.STATE_STOPPED);
        saveWorkoutData();
        Log.i(TAG, "Stopping service");
        stopSelf();
    }

    private void updateState(int newState) {
        state = newState;
        if (state != IntentHelper.STATE_STOPPED) {
            try {
                // TODO NullPointerException
                pendingWorkout.setStatus(getWorkoutStatus());
                workoutDao.update(pendingWorkout);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableLocationUpdates() {
        if (state == IntentHelper.STATE_RUNNING || state == IntentHelper.STATE_CONTINUE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BTW_UPDATES, MIN_DISTANCE, this);
                Log.i(TAG, "Location updates for service were requested");
            }
            else
                Log.e(TAG, "Location updates cannot be requested because of missing permissions");
        }
        else
            Log.i(TAG, "Location updates not requested because workout is not running");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        Log.i(TAG, "Location updates were stopped");
        OpenHelperManager.releaseHelper();
        Log.i(TAG, "Service was destroyed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "New location received");

        if (state != IntentHelper.STATE_PAUSED) {
            try {
                saveCurrentLocation(location);
                countDistance();
                countSpeed();
                countPace();
                calories = SportActivities.countCalories(sportActivity, weight, speedList, getTimeFillingSpeedListInHours());
                persistGpsPoint();
            }
            catch (InsufficientDistanceException ide) {
                Log.w(TAG, "Distance difference was insufficient to update all counters");
                removeCurrentLocation();
            }
            catch (NotEnoughLocationsException nele) {
                Log.w(TAG, "Counters not updated because of missing locations (minimum of 2 required)");
            }
        }
        else
            Log.i(TAG, "Workout is paused and new location ignored");
    }

    private void saveCurrentLocation(Location location) {
        location.setTime(new Date().getTime());
        positionList.add(location);
        currentLocation = location;
        Log.i(TAG, "New location saved");
    }

    private void removeCurrentLocation() {
        positionList.remove(positionList.size() - 1);
        currentLocation = null;
        Log.i(TAG, "New location removed");
    }

    private void persistGpsPoint() {
        countTotalCalories();
        GpsPoint currentGpsPoint = new GpsPoint(pendingWorkout, sessionNumber, currentLocation, duration, speed, pace, totalCalories);
        try {
            gpsPointDao.create(currentGpsPoint);
            Log.i(TAG, "Gps point with updated workout counters was persisted to the local database");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void countTotalCalories() {
        totalCalories = previousCalories + calories;
        Log.i(TAG, "New total calories for gps point: " + totalCalories + "kcal");
    }

    private void performCheck() {
        verifyPace();
        verifyWorkoutData();
    }

    private void verifyPace() {
        if (lastLocationUpdateBeforeSeconds > PACE_UPDATE_LIMIT && pace != 0.0) {
            pace = 0.0;
            Log.i(TAG, "Pace reset due to overcome pace limit");
        }
    }

    private void verifyWorkoutData() {
        if (lastLocationUpdateBeforeSeconds > 10 && lastLocationUpdateBeforeSeconds - workoutDataSavedAtSeconds == 11) {
            Log.i(TAG, "No new location received for " + lastLocationUpdateBeforeSeconds + " seconds");
            saveWorkoutData();
            workoutDataSavedAtSeconds = lastLocationUpdateBeforeSeconds - 1;
        }
    }

    private void saveWorkoutData() {
        try {
            pendingWorkout.setTotalCalories(totalCalories);
            pendingWorkout.setDistance(distance);
            pendingWorkout.setDuration(duration);
            pendingWorkout.setLastUpdate(new Date());
            pendingWorkout.setPaceAvg(getAveragePace());
            pendingWorkout.setStatus(getWorkoutStatus());
            workoutDao.update(pendingWorkout);
            Log.i(TAG, "Workout data saved to database");
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        catch (NullPointerException npe) {
            Log.e(TAG, "Workout is null");
        }
    }

    private double getAveragePace() {
        ArrayList<Double> paceList = new ArrayList<>();
        try {
            List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", pendingWorkout.getId());
            double currentPace;
            for (GpsPoint gpsPoint : gpsPoints) {
                currentPace = gpsPoint.getPace();
                if (currentPace != 0.0)
                    paceList.add(currentPace);
            }
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
            if (previousLocation != null) {
                double additionalDistance = getAdditionalDistanceAfterContinue();
                distance += additionalDistance;
                Log.i(TAG, additionalDistance + " meter(s) added after resuming pending workout");
            }
            else
                Log.i(TAG, "Skipping addition of additional distance after resuming pending workout because of missing previous location");
            hasContinued = false;
        }
        else
            throw new NotEnoughLocationsException();
    }

    private double getAdditionalDistanceAfterContinue() {
        double newDistance = calculateNewDistance(positionList.get(positionList.size() - 1), previousLocation);
        if (newDistance <= SESSION_DIFF_LIMIT)
            return newDistance;
        return 0;
    }

    private float calculateNewDistance(Location a, Location b) {
        return a.distanceTo(b);
    }

    private void validateNewDistance(double newDistance) throws InsufficientDistanceException {
        if (newDistance >= 2) {
            distance += newDistance;
            lastLocationUpdateBeforeSeconds = 0;
            Log.i(TAG, "New distance: " + distance + "m");
            return;
        }

        throw new InsufficientDistanceException();
    }

    private void countSpeed() {
        speed = (float)distance / (float) MainHelper.msToS(duration);
        speedList.add(speed);
        if (speedList.size() == 1)
            firstSpeedTime = new Date();
        Log.i(TAG, "New speed: " + speed + "m/s");
    }

    private double getTimeFillingSpeedListInHours() {
        Date lastSpeedTime = new Date();
        return ((lastSpeedTime.getTime() - firstSpeedTime.getTime()) / 3.6) / 1000000;
    }

    private void countPace() {
        pace = 1000 / speed;
        Log.i(TAG, "New pace: " + pace + "min/km");
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
