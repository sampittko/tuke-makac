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

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchActivity extends AppCompatActivity {
    @BindView(R.id.button_stopwatch_start) public Button startWorkoutButton;
    @BindView(R.id.button_stopwatch_endworkout) public Button endWorkoutButton;
    @BindView(R.id.textview_stopwatch_duration) public TextView durationTextView;

    @BindDrawable(R.drawable.ic_pause_circle_filled_green_24dp) public Drawable pauseDrawable;
    @BindDrawable(R.drawable.ic_play_circle_filled_green_24dp) public Drawable playDrawable;

    @BindString(R.string.stopwatch_stop) public String stopString;
    @BindString(R.string.stopwatch_continue) public String continueString;

    private boolean workoutStarted;
    private boolean workoutPaused;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private int sportActivity;
    private String duration;
    private double distance;
    private double pace;
    private double calories;
    private ArrayList<List<Location>> finalPositionList;

    private IntentFilter intentFilter;
    private StopwatchActivity stopwatchActivity;
    private final String TAG = "StopwatchActivity";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (!intent.getAction().equals(IntentHelper.ACTION_TICK)) {
                    Log.e(TAG, "Broadcast intent does not contain accepted action.");
                    return;
                }

                Log.i(TAG, "Broadcast intent received.");
                handleBroadcastIntent(intent);
            }
            catch(NullPointerException e) {
                Log.e(TAG, "Intent does not have any action inside.");
            }
        }

        private void handleBroadcastIntent(Intent intent) {
            Log.i(TAG, "Handling intent.");
            duration = String.valueOf(MainHelper.formatDuration(
                    intent.getLongExtra(
                            IntentHelper.DATA_DURATION, 0)
            ));

            durationTextView.setText(duration);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stopwatchActivity = this;
        setContentView(R.layout.activity_stopwatch);
        createAlertDialog();
        ButterKnife.bind(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},0);
            Log.i(TAG, "Location permissions requested.");
        } else {
            Log.i(TAG, "Location permissions OK.");
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
    }

    private void createAlertDialog() {
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Stop workout")
                .setMessage("Do you really want to stop recording?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        endWorkoutButton.setText(stopString);

                        Intent intent = new Intent(stopwatchActivity, TrackerService.class);
                        intent.setAction(IntentHelper.ACTION_STOP);
                        startService(intent);

                        intent = new Intent(stopwatchActivity, WorkoutDetailActivity.class);
                        intent.putExtra(IntentHelper.DATA_SPORT, sportActivity);
                        intent.putExtra(IntentHelper.DATA_DURATION, duration);
                        intent.putExtra(IntentHelper.DATA_DISTANCE, distance);
                        intent.putExtra(IntentHelper.DATA_PACE, pace);
                        intent.putExtra(IntentHelper.DATA_CALORIES, calories);
                        intent.putExtra(IntentHelper.DATA_POSITIONS, finalPositionList);
                        startActivity(intent);
                        dialogInterface.dismiss();
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
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);

        Log.i(TAG, "Receiver registered.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);

        Log.i(TAG, "Receiver unregistered.");
    }

    @OnClick(R.id.button_stopwatch_start)
    public void toggleRecordingHandler(View view) {
        if (!workoutStarted)
            toggleRecording(true, true, IntentHelper.ACTION_START, false, true);
        else {
            if (workoutPaused)
                toggleRecording(true, true, IntentHelper.ACTION_CONTINUE, false, false);
            else
                toggleRecording(false, false, IntentHelper.ACTION_PAUSE, true, false);
        }
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
        else {
            workoutPaused = !workoutPaused;
            if (!workoutPaused)
                createNewLocationList();
        }
    }

    private void createNewLocationList() {
        // finalPositionList.add(new List<Location>());
    }

    @OnClick(R.id.button_stopwatch_endworkout)
    public void stopRecordingHandler(View view) {
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        Log.i(TAG, "Alert dialog is now visible.");
    }
}
