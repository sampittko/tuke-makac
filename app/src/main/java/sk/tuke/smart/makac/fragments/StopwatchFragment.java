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
import sk.tuke.smart.makac.UnitChange;
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.WorkoutRecovery;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.helpers.SportActivities;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchFragment extends Fragment implements DatabaseConnection, UnitChange, WorkoutRecovery {
    @BindView(R.id.button_stopwatch_start) public Button startWorkoutButton;
    @BindView(R.id.button_stopwatch_endworkout) public Button endWorkoutButton;
    @BindView(R.id.textview_stopwatch_duration) public TextView durationTextView;
    @BindView(R.id.textview_stopwatch_distance) public TextView distanceTextView;
    @BindView(R.id.textview_stopwatch_pace) public TextView paceTextView;
    @BindView(R.id.textview_stopwatch_calories) public TextView caloriesTextView;
    @BindView(R.id.textview_stopwatch_distanceunit) public TextView distanceUnitTextView;
    @BindView(R.id.textview_stopwatch_unitpace) public TextView paceUnitTextView;
    @BindView(R.id.button_stopwatch_selectsport) public Button selectSportButton;

    @BindDrawable(R.drawable.ic_pause_circle_filled_green) public Drawable pauseDrawable;
    @BindDrawable(R.drawable.ic_play_circle_filled_green) public Drawable playDrawable;

    @BindString(R.string.stopwatch_stop) public String stopString;
    @BindString(R.string.stopwatch_continue) public String continueString;

    private final String TAG = "StopwatchFragment";

    private boolean workoutStarted, workoutPaused, menuInvalidated;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private double distance, pace, calories, totalCalories, latestBiggestNonZeroCalories;

    private long duration;

    private List<Location> latestPositionList;

    private IntentFilter intentFilter;

    private FragmentActivity thisFragmentActivity;

    private int userSelectedSportActivity, currentDistanceUnit;

    private Dao<Workout, Long> workoutDao;
    private Dao<GpsPoint, Long> gpsPointDao;

    private SharedPreferences userShPr;
    private SharedPreferences appShPr;

    private Workout lastWorkout;

    private boolean unitChanged, locationChecked;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent broadcastIntent) {
            try {
                if (broadcastIntent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    Log.i(TAG, IntentHelper.ACTION_TICK);
                    renderValues(broadcastIntent);
                    verifyNewestLocation((Location)broadcastIntent.getParcelableExtra(IntentHelper.DATA_WORKOUT_LOCATION));
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
        createStopWorkoutAlertDialog();
        registerBroadcastReceiver();
        databaseSetup();
    }

    private void initializeVariables() {
        latestPositionList = new ArrayList<>();
        thisFragmentActivity = getActivity();
        thisFragmentActivity.setTitle(R.string.app_name);
        appShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.appshpr), Context.MODE_PRIVATE);
        userShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        currentDistanceUnit = appShPr.getInt(getString(R.string.appshpr_unit), Integer.valueOf(getString(R.string.appshpr_unit_default)));
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

                    private void startWorkoutDetailActivity() {
                        try {
                            Intent intent = new Intent(thisFragmentActivity, WorkoutDetailActivity.class);
                            intent.putExtra(IntentHelper.DATA_WORKOUT_ID, getCurrentWorkoutId());
                            startActivityForResult(intent, Workout.STOPWATCH_REQUEST);
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
        if (requestCode == Workout.STOPWATCH_REQUEST && resultCode == Workout.CLOSE_RESULT)
            mListener.onWorkoutStopped();
        else if (requestCode == Workout.STOPWATCH_REQUEST && resultCode == Workout.DELETE_RESULT) {
            String deletedWorkoutTitle = data.getStringExtra(IntentHelper.DATA_WORKOUT_TITLE);
            String toastMessage = deletedWorkoutTitle + " was deleted";
            Toast.makeText(thisFragmentActivity, toastMessage, Toast.LENGTH_SHORT).show();
            mListener.onWorkoutStopped();
        }
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
            // line that needs to be run after database scheme upgrade
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
        if (menuInvalidated)
            inflater.inflate(R.menu.delete_pending, menu);
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
        setUnits();
        performWorkoutRecoveryCheck();
        return view;
    }

    private void performWorkoutRecoveryCheck() {
        try {
            List<Workout> userWorkouts = workoutDao.queryForEq(Workout.COLUMN_USERID, getCurrentUserId());
            if (userWorkouts.size() != 0) {
                lastWorkout = userWorkouts.get(userWorkouts.size() - 1);
                int lastWorkoutStatus = lastWorkout.getStatus();
                if (lastWorkoutStatus == Workout.statusPaused || lastWorkoutStatus == Workout.statusUnknown)
                    performWorkoutRecovery();
            }
            else
                Log.i(TAG, "Skipping recovery check because of empty local workout database table");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void performWorkoutRecovery() {
        workoutStarted = true;
        workoutPaused = false;
        toggleRecordingHandler(getView());
        renderValues(MainHelper.msToS(lastWorkout.getDuration()), lastWorkout.getDistance(), lastWorkout.getPaceAvg(), lastWorkout.getTotalCalories());
        retrievePositions();
        Log.i(TAG, "StopwatchFragment recovered");
    }

    private void retrievePositions() {
        try {
            List<GpsPoint> gpsPoints = gpsPointDao.queryForEq(GpsPoint.COLUMN_WORKOUTID, getCurrentWorkoutId());
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
        if (!locationChecked) {
            checkForGpsChecking();
            locationChecked = true;
        }
        checkForUnitChange();
        thisFragmentActivity.invalidateOptionsMenu();
        Log.i(TAG, "Receiver registered");
    }

    public void checkForUnitChange() {
        int newUnit = appShPr.getInt(getString(R.string.appshpr_unit), Integer.valueOf(getString(R.string.appshpr_unit_default)));
        if (currentDistanceUnit != newUnit) {
            unitChanged = true;
            currentDistanceUnit = newUnit;
            renderUnitDependentValues();
            Log.i(TAG, "Unit dependent values re-rendered after unit change");
            Toast.makeText(thisFragmentActivity, "Units changed", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForGpsChecking() {
        if (appShPr.getBoolean(getString(R.string.appshpr_gps), Boolean.valueOf(getString(R.string.appshpr_gps_default))))
            checkLocationPermissions();
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @OnClick(R.id.button_stopwatch_start)
    public void toggleRecordingHandler(View view) {
        if (workoutStarted) {
            if (workoutPaused) {
                checkForGpsChecking();
                toggleRecording(true, true, IntentHelper.ACTION_CONTINUE, false, false);
            }
            else
                toggleRecording(false, false, IntentHelper.ACTION_PAUSE, true, false);
        }
        else {
            if (!menuInvalidated) {
                menuInvalidated = true;
                thisFragmentActivity.invalidateOptionsMenu();
            }
            checkForGpsChecking();
            toggleRecording(true, true, IntentHelper.ACTION_START, false, true);
        }
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
                IntentHelper.DATA_WORKOUT_DURATION, 0));
        distanceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_WORKOUT_DISTANCE, 0));
        paceRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_WORKOUT_PACE, 0));
        caloriesRenderer(broadcastIntent.getDoubleExtra(
                IntentHelper.DATA_WORKOUT_CALORIES, 0));
    }

    private void renderValues(long duration, double distance, double pace, double totalCalories) {
        durationRenderer(duration);
        distanceRenderer(distance);
        paceRenderer(pace);
        caloriesRenderer(totalCalories);
    }

    private void renderUnitDependentValues() {
        if (duration > 0) {
            distanceRenderer(distance);
            paceRenderer(pace);
        }
        setUnits();
        unitChanged = false;
    }

    private void setUnits() {
        if (currentDistanceUnit == SportActivities.UNIT_KILOMETERS) {
            distanceUnitTextView.setText(R.string.all_labeldistanceunitkilometers);
            paceUnitTextView.setText(R.string.textview_stopwatch_unitpace);
        }
        else {
            distanceUnitTextView.setText(R.string.all_labeldistanceunitmiles);
            paceUnitTextView.setText(R.string.textview_stopwatch_unitpace_min_mi);
        }
    }

    private void durationRenderer(long broadcastIntentDuration) {
        duration = broadcastIntentDuration;

        String newDuration = String.valueOf(MainHelper.formatDuration(duration));
        durationTextView.setText(newDuration);
        Log.i(TAG, "New duration: " + newDuration);
    }

    private void distanceRenderer(double broadcastIntentDistance) {
        if (distance != broadcastIntentDistance || unitChanged) {
            distance = broadcastIntentDistance;

            String newDistance;
            if (currentDistanceUnit == SportActivities.UNIT_KILOMETERS)
                newDistance = MainHelper.formatDistance(distance);
            else
                newDistance = MainHelper.formatDistanceMiles(distance);
            distanceTextView.setText(newDistance);
            Log.i(TAG, "New distance: " + newDistance);
        }
    }

    private void paceRenderer(double broadcastIntentPace) {
        if (broadcastIntentPace == Double.POSITIVE_INFINITY)
            broadcastIntentPace = 0;

        if (pace != broadcastIntentPace || unitChanged) {
            pace = broadcastIntentPace;

            String newPace;
            if (currentDistanceUnit == SportActivities.UNIT_KILOMETERS)
                newPace = MainHelper.formatPace(pace);
            else
                newPace = MainHelper.formatPaceMiles(pace);
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
            intent.putExtra(IntentHelper.DATA_WORKOUT_ID, getCurrentWorkoutId());
            startActivity(intent);
            Log.i(TAG, "Showing active workout map");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private long getCurrentWorkoutId() throws SQLException {
        List<Workout> userWorkouts = workoutDao.queryForEq(Workout.COLUMN_USERID, getCurrentUserId());
        return userWorkouts.get(userWorkouts.size() - 1).getId();
    }

    private long getCurrentUserId() {
        return userShPr.getLong(getString(R.string.usershpr_userid), Long.valueOf(getString(R.string.usershpr_userid_default)));
    }

    @OnClick(R.id.button_stopwatch_selectsport)
    public void displaySelectSportDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(thisFragmentActivity);
        builder.setTitle("Select sport")
                .setItems(R.array.stringarray_stopwatch_sportactivities, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int position) {
                userSelectedSportActivity = position;
                switch (position) {
                    case 0:
                        Log.i(TAG, "Sport activity set to running");
                        selectSportButton.setBackground(thisFragmentActivity.getDrawable(R.drawable.ic_launcher_foreground));
                        break;
                    case 1:
                        Log.i(TAG, "Sport activity set to walking");
                        selectSportButton.setBackground(thisFragmentActivity.getDrawable(R.drawable.ic_directions_walk_blue_24dp));
                        break;
                    case 2:
                        Log.i(TAG, "Sport activity set to cycling");
                        selectSportButton.setBackground(thisFragmentActivity.getDrawable(R.drawable.ic_motorcycle_blue_24dp));
                        break;
                    default:
                        Log.e(TAG, "Sport activity set to running because of unexpected behavior");
                        selectSportButton.setBackground(thisFragmentActivity.getDrawable(R.drawable.ic_launcher_foreground));
                        break;
                }
            }
        });
        builder.show();
    }

    public void displayDeletePendingAlertDialog() {
        AlertDialog.Builder alertDialogBuilderPendingDelete = new AlertDialog.Builder(thisFragmentActivity);
        alertDialogBuilderPendingDelete
                .setTitle("Delete pending workout")
                .setMessage("Do you really want to delete this workout?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopTrackerService();
                        deletePendingWorkout();
                        Log.i(TAG, "Pending workout deleted");
                        Toast.makeText(thisFragmentActivity, "Pending workout deleted", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                        mListener.onWorkoutStopped();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.dismiss();
                    }
                });
        alertDialog = alertDialogBuilderPendingDelete.create();
        alertDialog.show();
    }

    private void stopTrackerService() {
        Intent intent = new Intent(thisFragmentActivity, TrackerService.class);
        intent.putExtra(IntentHelper.DATA_WORKOUT_SPORT_ACTIVITY, userSelectedSportActivity);
        intent.setAction(IntentHelper.ACTION_STOP);
        thisFragmentActivity.startService(intent);
    }

    private void deletePendingWorkout() {
        try {
            Workout pendingWorkout = workoutDao.queryForId(getCurrentWorkoutId());
            pendingWorkout.setStatus(Workout.statusDeleted);
            workoutDao.update(pendingWorkout);
            Log.i(TAG, "Pending workout data deleted");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public interface OnFragmentInteractionListener {
        void onWorkoutStopped();
    }
}
