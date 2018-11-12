package sk.tuke.smart.makac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import sk.tuke.smart.makac.helpers.IntentCommands;
import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchActivity extends AppCompatActivity {
    private boolean workoutStarted;
    private boolean workoutPaused;

    private Button startWorkoutButton;
    private Button endWorkoutButton;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private int sportActivity;
    private long duration;
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
            Log.i(TAG, "Broadcast intent received.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stopwatchActivity = this;
        setContentView(R.layout.activity_stopwatch);
        createAlertDialog();

        startWorkoutButton = findViewById(R.id.button_stopwatch_start);
        endWorkoutButton = findViewById(R.id.button_stopwatch_endworkout);

        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentCommands.ACTION_TICK);
    }

    // CREATING ALERT DIALOG
    private void createAlertDialog() {
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Stop workout")
                .setMessage("Do you really want to stop recording?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        endWorkoutButton.setText(R.string.stopwatch_stop);

                        Intent intent = new Intent(stopwatchActivity, TrackerService.class);
                        intent.setAction(IntentCommands.ACTION_STOP);
                        startService(intent);

                        intent = new Intent(stopwatchActivity, WorkoutDetailActivity.class);
                        intent.putExtra("SPORT", sportActivity);
                        intent.putExtra("DURATION", duration);
                        intent.putExtra("DISTANCE", distance);
                        intent.putExtra("PACE", pace);
                        intent.putExtra("CALORIES", calories);
                        intent.putExtra("POSITIONS", finalPositionList);
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
        Log.i("StopwatchActivity", "Receiver unregistered.");
    }

    // START/PAUSE/CONTINUE BUTTON HANDLER
    public void toggleRecording(View view) {
        if (!workoutStarted) {
            startWorkoutButton.setText(R.string.stopwatch_stop);

            Intent intent = new Intent(this, TrackerService.class);
            intent.setAction(IntentCommands.ACTION_START);
            startService(intent);

            workoutStarted = !workoutStarted;
        }
        else {
            if (!workoutPaused) {
                startWorkoutButton.setText(R.string.stopwatch_continue);

                Intent intent = new Intent(this, TrackerService.class);
                intent.setAction(IntentCommands.ACTION_PAUSE);
                startService(intent);

                endWorkoutButton.setVisibility(Button.VISIBLE);
            }
            else {
                startWorkoutButton.setText(R.string.stopwatch_stop);

                Intent intent = new Intent(this, TrackerService.class);
                intent.setAction(IntentCommands.ACTION_CONTINUE);
                startService(intent);

                endWorkoutButton.setVisibility(Button.GONE);
            }

            workoutPaused = !workoutPaused;
        }

        Log.i(TAG, "Workout is paused: " + workoutPaused + " Workout started: " + workoutStarted);
    }

    // STOP BUTTON HANDLER
    public void stopRecording(View view) {
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
