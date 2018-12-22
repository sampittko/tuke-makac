package sk.tuke.smart.makac.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.MenuItem;
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
import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.exceptions.SensorNotPresentException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.model.User;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchFragment extends Fragment {
    @BindView(R.id.button_stopwatch_start) public Button startWorkoutButton;
    @BindView(R.id.button_stopwatch_endworkout) public Button endWorkoutButton;
    @BindView(R.id.textview_stopwatch_duration) public TextView durationTextView;
    @BindView(R.id.textview_stopwatch_distance) public TextView distanceTextView;
    @BindView(R.id.textview_stopwatch_pace) public TextView paceTextView;
    @BindView(R.id.textview_stopwatch_calories) public TextView caloriesTextView;

    @BindDrawable(R.drawable.ic_pause_circle_filled_green) public Drawable pauseDrawable;
    @BindDrawable(R.drawable.ic_play_circle_filled_green) public Drawable playDrawable;

    @BindString(R.string.stopwatch_stop) public String stopString;
    @BindString(R.string.stopwatch_continue) public String continueString;

    private boolean workoutStarted, workoutPaused, persistedOnce;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private int sportActivity = IntentHelper.ACTIVITY_RUNNING;

    private long duration;

    private double distance, pace, calories, totalCalories, latestBiggestNonZeroCalories;

    private final String TAG = "StopwatchFragment";

    private ArrayList<Double> paceList = new ArrayList<>();
    private ArrayList<Location> latestPositionList = new ArrayList<>();
    private ArrayList<List<Location>> finalPositionList = new ArrayList<>();

    private IntentFilter intentFilter;

    private FragmentActivity thisFragmentActivity;

    private DatabaseHelper databaseHelper;

    private List<Workout> workouts;

    private Workout currentWorkout;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent broadcastIntent) {
            try {
                if (broadcastIntent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    renderValues(broadcastIntent);
                    verifyNewestLocation((Location)broadcastIntent.getParcelableExtra(IntentHelper.DATA_LOCATION));
                }
                else if (broadcastIntent.getAction().equals(IntentHelper.ACTION_GPS))
                    saveLatestPositionList();
                else
                    Log.e(TAG, "Broadcast intent does not contain accepted action.");
            }
            catch(NullPointerException e) {
                Log.e(TAG, "Broadcast intent does not have any action inside.");
            }
        }
    };

    private OnFragmentInteractionListener mListener;

    public StopwatchFragment() {
        // Required empty public constructor
    }

    public static StopwatchFragment newInstance() {
        return new StopwatchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        thisFragmentActivity = getActivity();
        thisFragmentActivity.setTitle(R.string.app_name);

        databaseHelper = OpenHelperManager.getHelper(thisFragmentActivity, DatabaseHelper.class);

//        createNewWorkout();
//        createUser();

        try {
            checkGPS();
        }
        catch(SensorNotPresentException e) {
            Log.e(TAG, "GPS sensor is missing so application cannot be started.");
            thisFragmentActivity.finish();
        }

        createStopWorkoutAlertDialog();

        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
        intentFilter.addAction(IntentHelper.ACTION_GPS);

        thisFragmentActivity.registerReceiver(broadcastReceiver, intentFilter);

    }

    private void createUser() {
        try {
            Dao<User, Long> userDao = databaseHelper.userDao();
            userDao.create(new User());
            List<User> users = userDao.queryForAll();
            Log.i("usersprofile", users.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createNewWorkout() {
        currentWorkout = new Workout();

        try {
            workouts = databaseHelper.workoutDao().queryForAll();
        }
        catch(SQLException e) {
            Log.e(TAG, "SQL error.");
        }

        if (workouts.size() == 0) {
            currentWorkout.setTitle("Workout 1");
            Log.i(TAG, "Workout ID is 1.");
        }
        else {
            currentWorkout.setTitle("Workout " + (workouts.size() + 1));
            Log.i(TAG, "Workout ID is " + (workouts.size() + 1) + ".");
        }
        currentWorkout.setSportActivity(sportActivity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.syncwithserver, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_syncwithserver:
                // TODO sync with server
                Log.e(TAG, "Sync not implemetned.");
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return true;
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
                        thisFragmentActivity.finish();
                    }

                    private void stopTrackerService() {
                        Intent intent1 = new Intent(thisFragmentActivity, TrackerService.class);
                        intent1.setAction(IntentHelper.ACTION_STOP);
                        thisFragmentActivity.startService(intent1);
                    }

                    private void startWorkoutDetailActivity() {
                        Intent intent = new Intent(thisFragmentActivity, WorkoutDetailActivity.class);
                        intent.putExtra(IntentHelper.DATA_SPORT, sportActivity);
                        intent.putExtra(IntentHelper.DATA_DURATION, duration);
                        intent.putExtra(IntentHelper.DATA_DISTANCE, distance);
                        intent.putExtra(IntentHelper.DATA_PACE, countAvgPace());
                        intent.putExtra(IntentHelper.DATA_CALORIES, calories);
                        intent.putExtra(IntentHelper.DATA_POSITIONS, finalPositionList);
                        // TODO workoutId Intent
                        intent.putExtra(IntentHelper.DATA_WORKOUT, 0);
                        startActivity(intent);
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
    public void onPause() {
        super.onPause();
        if (!workoutStarted || workoutPaused) {
            thisFragmentActivity.unregisterReceiver(broadcastReceiver);
            thisFragmentActivity.stopService(new Intent(thisFragmentActivity, TrackerService.class));
        }

        Log.i(TAG, "Receiver unregistered.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stopwatch, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        thisFragmentActivity.registerReceiver(broadcastReceiver, intentFilter);

        if (!workoutStarted)
            thisFragmentActivity.startService(new Intent(thisFragmentActivity, TrackerService.class));

        Log.i(TAG, "Receiver registered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(thisFragmentActivity, TrackerService.class);
        thisFragmentActivity.stopService(intent);
        Log.i(TAG, "Service stopped.");
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
        Log.i(TAG, "Broadcast intent received with action TICK.");

        durationRenderer(broadcastIntent.getLongExtra(
                IntentHelper.DATA_DURATION, 0));
        distanceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_DISTANCE, 0));
        paceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_PACE, 0));
        caloriesRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_CALORIES, 0));

        Log.i(TAG, "UI updated.");
    }

    private void durationRenderer(long broadcastIntentDuration) {
        duration = broadcastIntentDuration;

        String newDuration = String.valueOf(MainHelper.formatDuration(duration));
        durationTextView.setText(newDuration);
        Log.i(TAG, "Duration value updated. (" + newDuration + ")");
    }

    private void distanceRenderer(double broadcastIntentDistance) {
        if (distance != broadcastIntentDistance) {
            distance = broadcastIntentDistance;

            String newDistance = MainHelper.formatDistance(distance);
            distanceTextView.setText(newDistance);
            Log.i(TAG, "Distance value updated. (" + newDistance + "km)");
        }
        else
            Log.i(TAG, "Distance did not need an update.");
    }

    private void paceRenderer(double broadcastIntentPace) {
        if (pace != broadcastIntentPace) {
            pace = broadcastIntentPace;
            paceList.add(pace);

            String newPace = MainHelper.formatPace(pace);
            paceTextView.setText(newPace);
            Log.i(TAG, "Pace value updated. (" + newPace + "km/min)");
        }
        else
            Log.i(TAG, "Pace did not need an update.");
    }

    private void caloriesRenderer(double broadcastIntentCalories) {
        if (calories != broadcastIntentCalories) {
            calories = broadcastIntentCalories + totalCalories;

            if (broadcastIntentCalories != 0 && broadcastIntentCalories > latestBiggestNonZeroCalories)
                latestBiggestNonZeroCalories = broadcastIntentCalories;

            String newCalories = MainHelper.formatCalories(calories);
            caloriesTextView.setText(newCalories);
            Log.i(TAG, "Calories value updated. (" + newCalories + "kcal)");
        }
        else
            Log.i(TAG, "Calories did not need an update.");
    }

    @OnClick(R.id.button_stopwatch_endworkout)
    public void stopRecordingHandler(View view) {
        alertDialog = alertDialogBuilder.create();
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
        Log.i(TAG, "Broadcast intent received with action GPS.");

        if (!latestPositionList.isEmpty()) {
            finalPositionList.add(latestPositionList);
            latestPositionList = new ArrayList<>();
            Log.i(TAG, "Location list saved after unpausing.");
        }
        else
            Log.i(TAG, "Location list is empty and does not need to be saved after unpausing.");

        totalCalories += latestBiggestNonZeroCalories;
        latestBiggestNonZeroCalories = 0;
    }

    private double countAvgPace() {
        double paceCount = 0;

        for (double pace : paceList)
            paceCount += pace;

        return paceCount / paceList.size();
    }

    @OnClick(R.id.button_stopwatch_activeworkout)
    public void showActiveWorkoutMap(View view) {
        Intent intent = new Intent(thisFragmentActivity, ActiveWorkoutMapActivity.class);
        intent.putExtra(IntentHelper.DATA_POSITIONS, finalPositionList);
        startActivity(intent);
        Log.i(TAG, "Showing active workout map.");
    }

    public interface OnFragmentInteractionListener {}
}
