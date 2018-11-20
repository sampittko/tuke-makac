package sk.tuke.smart.makac;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.exceptions.SensorNotPresentException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchActivity extends AppCompatActivity {
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

    private boolean workoutStarted, workoutPaused;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private int sportActivity = IntentHelper.ACTIVITY_RUNNING;

    private long duration;

    private double distance, pace, calories, totalCalories, latestBiggestNonZeroCalories;

    private final String TAG = "StopwatchActivity";

    private ArrayList<Double> paceList = new ArrayList<>();
    private ArrayList<Location> latestPositionList = new ArrayList<>();
    private ArrayList<List<Location>> finalPositionList = new ArrayList<>();

    private IntentFilter intentFilter;

    private StopwatchActivity stopwatchActivity;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent broadcastIntent) {
            try {
                if (broadcastIntent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    renderValues(broadcastIntent);
                    verifyLatestPositionList(broadcastIntent.<Location>getParcelableArrayListExtra(IntentHelper.DATA_POSITIONS));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            checkGPS();
        }
        catch(SensorNotPresentException e) {
            Log.e(TAG, "GPS sensor is missing so application cannot be started.");
            finish();
        }

        setContentView(R.layout.activity_stopwatch);
        ButterKnife.bind(this);
        stopwatchActivity = this;

        createAlertDialog();

        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
        intentFilter.addAction(IntentHelper.ACTION_GPS);
    }

    private void checkGPS() throws SensorNotPresentException {
        checkSensorPresence();
        checkLocationPermissions();
    }

    private void checkSensorPresence() throws SensorNotPresentException {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(this, "Missing GPS sensor in device. Application closing.", Toast.LENGTH_LONG).show();
            throw new SensorNotPresentException();
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},0);
            Log.i(TAG, "Location permissions requested.");
        }
        else
            Log.i(TAG, "Location permissions OK.");
    }

    private void createAlertDialog() {
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Stop workout")
                .setMessage("Do you really want to stop recording?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveLatestPositionList();
                        endWorkoutButton.setText(stopString);

                        Intent intent1 = new Intent(stopwatchActivity, TrackerService.class);
                        intent1.setAction(IntentHelper.ACTION_STOP);
                        startService(intent1);

                        Intent intent2 = new Intent(stopwatchActivity, WorkoutDetailActivity.class);
                        Bundle bundle2 = new Bundle();
                        bundle2.putInt(IntentHelper.DATA_SPORT, sportActivity);
                        bundle2.putLong(IntentHelper.DATA_DURATION, duration);
                        bundle2.putDouble(IntentHelper.DATA_DISTANCE, distance);
                        bundle2.putDouble(IntentHelper.DATA_PACE, countAvgPace());
                        bundle2.putDouble(IntentHelper.DATA_CALORIES, calories);
                        bundle2.putSerializable(IntentHelper.DATA_POSITIONS, finalPositionList);
                        intent2.putExtra(IntentHelper.DATA_BUNDLE, bundle2);
                        startActivity(intent2);
                        dialogInterface.dismiss();
                        finish();
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
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);

        Log.i(TAG, "Receiver unregistered.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);

        Log.i(TAG, "Receiver registered.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, TrackerService.class);
        stopService(intent);
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
        Intent intent = new Intent(this, TrackerService.class);
        intent.setAction(intentAction);
        startService(intent);

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

    private void verifyLatestPositionList(List<Location> newPositionList) {
        if (newPositionList.size() == 0) {
            Log.i(TAG, "There are no locations inside the list.");
            return;
        }

        Location newPositionListNewestLocation = newPositionList.get(newPositionList.size() - 1);
        Location latestPositionListNewestLocation = latestPositionList.size() == 0 ? new Location("") : latestPositionList.get(latestPositionList.size() - 1);

        if (newPositionListNewestLocation.getTime() != latestPositionListNewestLocation.getTime()) {
            latestPositionList.add(newPositionListNewestLocation);
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
}