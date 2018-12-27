package sk.tuke.smart.makac.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.ActiveWorkoutMapActivity;
import sk.tuke.smart.makac.DatabaseConnection;
import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.exceptions.SensorNotPresentException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.helpers.SportActivities;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchFragment extends Fragment implements DatabaseConnection {
    @BindView(R.id.button_stopwatch_start) public Button startWorkoutButton;
    @BindView(R.id.button_stopwatch_endworkout) public Button endWorkoutButton;
    @BindView(R.id.textview_stopwatch_duration) public TextView durationTextView;
    @BindView(R.id.textview_stopwatch_distance) public TextView distanceTextView;
    @BindView(R.id.textview_stopwatch_pace) public TextView paceTextView;
    @BindView(R.id.textview_stopwatch_calories) public TextView caloriesTextView;
    @BindView(R.id.textview_stopwatch_distanceunit) public TextView distanceUnitTextView;
    @BindView(R.id.textview_stopwatch_unitpace) public TextView paceUnitTextview;

    @BindDrawable(R.drawable.ic_pause_circle_filled_green) public Drawable pauseDrawable;
    @BindDrawable(R.drawable.ic_play_circle_filled_green) public Drawable playDrawable;

    @BindString(R.string.stopwatch_stop) public String stopString;
    @BindString(R.string.stopwatch_continue) public String continueString;

    private final String TAG = "StopwatchFragment";

    private boolean workoutStarted, workoutPaused;

    private AlertDialog.Builder alertDialogBuilder;

    private double distance, pace, calories, totalCalories, latestBiggestNonZeroCalories;

    private List<Location> latestPositionList;

    private IntentFilter intentFilter;

    private FragmentActivity thisFragmentActivity;

    private StopwatchFragment stopwatchFragment;

    private Dao<Workout, Long> workoutDao;
    private Dao<GpsPoint, Long> gpsPointDao;

    private SharedPreferences userShPr;
    private SharedPreferences appShPr;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent broadcastIntent) {
            try {
                if (broadcastIntent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    Log.i(TAG, IntentHelper.ACTION_TICK);
                    renderValues(broadcastIntent);
                    verifyNewestLocation((Location)broadcastIntent.getParcelableExtra(IntentHelper.DATA_LOCATION));
                }
                else if (broadcastIntent.getAction().equals(IntentHelper.ACTION_GPS)) {
                    Log.i(TAG, IntentHelper.ACTION_GPS);
                    saveLatestPositionList();
                }
                else
                    Log.e(TAG, "Broadcast intent does not contain accepted action");
            }
            catch(NullPointerException e) {
                Log.e(TAG, "Broadcast intent does not have any action inside");
            }
        }
    };

    private OnFragmentInteractionListener mListener;

    public void setmListener(OnFragmentInteractionListener mListener) {
        this.mListener = mListener;
    }

    public StopwatchFragment() {}

    public static StopwatchFragment newInstance() {
        return new StopwatchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeVariables();
        setHasOptionsMenu(true);
        try {
            checkGPS();
        }
        catch(SensorNotPresentException e) {
            Log.e(TAG, "GPS sensor is missing so application cannot be started");
            thisFragmentActivity.finish();
        }
        createStopWorkoutAlertDialog();
        registerBroadcastReceiver();
        databaseSetup();
        userShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        appShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.appshpr), Context.MODE_PRIVATE);
    }

    private void initializeVariables() {
        latestPositionList = new ArrayList<>();
        thisFragmentActivity = getActivity();
        thisFragmentActivity.setTitle(R.string.app_name);
    }

    private void checkGPS() throws SensorNotPresentException {
        checkSensorPresence();
        checkLocationPermissions();
    }

    private void checkSensorPresence() throws SensorNotPresentException {
        if (!thisFragmentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(thisFragmentActivity, "Missing GPS sensor in device. Application closing.", Toast.LENGTH_LONG).show();
            throw new SensorNotPresentException();
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(thisFragmentActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ActivityCompat.checkSelfPermission(thisFragmentActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(thisFragmentActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},0);
            Log.i(TAG, "Location permissions requested.");
        }
        else
            Log.i(TAG, "Location permissions OK.");
    }

    private void createStopWorkoutAlertDialog() {
        alertDialogBuilder = new AlertDialog.Builder(thisFragmentActivity);
        alertDialogBuilder.setTitle("Stop workout")
                .setMessage("Do you really want to stop recording?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveLatestPositionList();
                        endWorkoutButton.setText(stopString);
                        stopTrackerService();
                        startWorkoutDetailActivity();
                        dialogInterface.dismiss();
                    }

                    private void stopTrackerService() {
                        Intent intent1 = new Intent(thisFragmentActivity, TrackerService.class);
                        intent1.setAction(IntentHelper.ACTION_STOP);
                        thisFragmentActivity.startService(intent1);
                    }

                    private void startWorkoutDetailActivity() {
                        try {
                            Intent intent = new Intent(thisFragmentActivity, WorkoutDetailActivity.class);
                            intent.putExtra(IntentHelper.DATA_WORKOUT, getCurrentWorkoutId());
                            startActivityForResult(intent, 1);
                        }
                        catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 1)
            mListener.onWorkoutStopped();
    }

    private void registerBroadcastReceiver() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
        intentFilter.addAction(IntentHelper.ACTION_GPS);
        thisFragmentActivity.registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "Broadcast receiver registered");
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(thisFragmentActivity, DatabaseHelper.class);
            // databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), databaseHelper.getConnectionSource(), 4, 5);
            workoutDao = databaseHelper.workoutDao();
            gpsPointDao = databaseHelper.gpsPointDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))))
            inflater.inflate(R.menu.sync_with_server, menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!workoutStarted || workoutPaused) {
            thisFragmentActivity.unregisterReceiver(broadcastReceiver);
            thisFragmentActivity.stopService(new Intent(thisFragmentActivity, TrackerService.class));
            Log.i(TAG, "Receiver unregistered.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stopwatch, container, false);
        ButterKnife.bind(this, view);
        performWorkoutRecoveryCheck();
        return view;
    }

    private void performWorkoutRecoveryCheck() {
        try {
            long workoutsCount = workoutDao.countOf();
            if (workoutsCount != 0) {
                Workout lastWorkout = workoutDao.queryForId(workoutsCount + Workout.ID_OFFSET);
                int lastWorkoutStatus = lastWorkout.getStatus();
                if (lastWorkoutStatus == Workout.statusPaused || lastWorkoutStatus == Workout.statusUnknown)
                    performWorkoutRecovery(lastWorkout);
            }
            else
                Log.i(TAG, "Skipping recovery check because of empty local workout database table");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performWorkoutRecovery(Workout lastWorkout) {
        workoutStarted = true;
        workoutPaused = false;
        toggleRecordingHandler(getView());
        renderValues(MainHelper.msToS(lastWorkout.getDuration()), lastWorkout.getDistance(), lastWorkout.getPaceAvg(), lastWorkout.getTotalCalories());
        retrievePositions();
        Log.i(TAG, "StopwatchFragment recovered");
    }

    private void retrievePositions() {
        try {
            List<GpsPoint> gpsPoints = gpsPointDao.queryForEq("workout_id", getCurrentWorkoutId());
            ArrayList<List<Location>> finalPositionList = MainHelper.getFinalPositionList(gpsPoints);
            if (finalPositionList.size() == 0)
                Log.i(TAG, "There are no positions to retrieve");
            else {
                latestPositionList = finalPositionList.get(finalPositionList.size() - 1);
                Log.i(TAG, "Positions retrieved from local database");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        thisFragmentActivity.registerReceiver(broadcastReceiver, intentFilter);
        if (!workoutStarted)
            thisFragmentActivity.startService(new Intent(thisFragmentActivity, TrackerService.class));
        thisFragmentActivity.invalidateOptionsMenu();
        Log.i(TAG, "Receiver registered");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(thisFragmentActivity, TrackerService.class);
        try {
            thisFragmentActivity.unregisterReceiver(broadcastReceiver);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        thisFragmentActivity.stopService(intent);
        OpenHelperManager.releaseHelper();
        Log.i(TAG, "Service stopped");
    }

    // TODO onAttach
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    // TODO onDetach
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @OnClick(R.id.button_stopwatch_start)
    public void toggleRecordingHandler(View view) {
        if (workoutStarted) {
            if (workoutPaused)
                toggleRecording(true, true, IntentHelper.ACTION_CONTINUE, false, false);
            else
                toggleRecording(false, false, IntentHelper.ACTION_PAUSE, true, false);
        }
        else
            toggleRecording(true, true, IntentHelper.ACTION_START, false, true);
    }

    private void toggleRecording(boolean useStopString, boolean usePauseDrawable, String intentAction, boolean endWorkoutButtonVisible, boolean changeWorkoutStarted) {
        // START SERVICE WITH CORRESPONDING ACTION
        Intent intent = new Intent(thisFragmentActivity, TrackerService.class);
        intent.setAction(intentAction);
        thisFragmentActivity.startService(intent);

        // CHANGE BUTTON STRING
        if (useStopString)
            startWorkoutButton.setText(stopString);
        else
            startWorkoutButton.setText(continueString);

        // CHANGE BUTON BACKGROUND
        if (usePauseDrawable)
            startWorkoutButton.setBackground(pauseDrawable);
        else
            startWorkoutButton.setBackground(playDrawable);

        // CHANGE END WORKOUT BUTTON VISIBILITY
        if (endWorkoutButtonVisible)
            endWorkoutButton.setVisibility(Button.VISIBLE);
        else
            endWorkoutButton.setVisibility(Button.GONE);

        // CHANGE BOOLEAN VARIABLE FOR TOGGLE DECISIONING
        if (changeWorkoutStarted)
            workoutStarted = !workoutStarted;
        else
            workoutPaused = !workoutPaused;
    }

    private void renderValues(Intent broadcastIntent) {
        durationRenderer(broadcastIntent.getLongExtra(
                IntentHelper.DATA_DURATION, 0));
        distanceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_DISTANCE, 0));
        paceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_PACE, 0));
        caloriesRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_CALORIES, 0));
    }

    private void renderValues(long duration, double distance, double pace, double totalCalories) {
        durationRenderer(duration);
        distanceRenderer(distance);
        paceRenderer(pace);
        caloriesRenderer(totalCalories);
    }

    private void durationRenderer(long broadcastIntentDuration) {
        long duration = broadcastIntentDuration;

        String newDuration = String.valueOf(MainHelper.formatDuration(duration));
        durationTextView.setText(newDuration);
        Log.i(TAG, "New duration: " + newDuration);
    }

    // TODO unit switch
    private void distanceRenderer(double broadcastIntentDistance) {
        if (distance != broadcastIntentDistance) {
            distance = broadcastIntentDistance;

            String newDistance = MainHelper.formatDistance(distance);
            distanceTextView.setText(newDistance);
            Log.i(TAG, "New distance: " + newDistance);
        }
    }

    // TODO unit switch
    private void paceRenderer(double broadcastIntentPace) {
        if (pace != broadcastIntentPace) {
            pace = broadcastIntentPace;

            String newPace = MainHelper.formatPace(pace);
            paceTextView.setText(newPace);
            Log.i(TAG, "New pace: " + newPace);
        }
    }

    private void caloriesRenderer(double broadcastIntentCalories) {
        if (calories - totalCalories != broadcastIntentCalories) {
            calories = broadcastIntentCalories + totalCalories;

            if (broadcastIntentCalories != 0 && broadcastIntentCalories > latestBiggestNonZeroCalories)
                latestBiggestNonZeroCalories = broadcastIntentCalories;

            String newCalories = MainHelper.formatCalories(calories);
            caloriesTextView.setText(newCalories);
            Log.i(TAG, "New calories: " + newCalories);
        }
    }

    @OnClick(R.id.button_stopwatch_endworkout)
    public void stopRecordingHandler(View view) {
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        Log.i(TAG, "Alert dialog is now visible.");
    }

    private void verifyNewestLocation(Location newestLocation) {
        if (newestLocation == null) {
            Log.i(TAG, "There is no new location.");
            return;
        }

        Location latestPositionListNewestLocation = latestPositionList.size() == 0 ? new Location("") : latestPositionList.get(latestPositionList.size() - 1);

        if (newestLocation.getTime() != latestPositionListNewestLocation.getTime()) {
            latestPositionList.add(newestLocation);
            Log.i(TAG, "Position list updated.");
            return;
        }

        Log.i(TAG, "Position list did not need an update.");
    }

    private void saveLatestPositionList() {
        if (!latestPositionList.isEmpty()) {
            latestPositionList = new ArrayList<>();
            Log.i(TAG, "Location list saved after unpausing.");
        }
        else
            Log.i(TAG, "Location list is empty and does not need to be saved after unpausing.");

        totalCalories += latestBiggestNonZeroCalories;
        latestBiggestNonZeroCalories = 0;
    }

    @OnClick(R.id.button_stopwatch_activeworkout)
    public void showActiveWorkoutMap(View view) {
        try {
            Intent intent = new Intent(thisFragmentActivity, ActiveWorkoutMapActivity.class);
            intent.putExtra(IntentHelper.DATA_WORKOUT, getCurrentWorkoutId());
            startActivity(intent);
            Log.i(TAG, "Showing active workout map");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private long getCurrentWorkoutId() throws SQLException {
        return Workout.ID_OFFSET + workoutDao.countOf();
    }

    public interface OnFragmentInteractionListener {
        void onWorkoutStopped();
    }
}
