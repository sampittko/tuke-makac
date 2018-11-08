package sk.tuke.smart.makac;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import sk.tuke.smart.makac.services.TrackerService;

public class StopwatchActivity extends AppCompatActivity {
    private boolean workoutStarted;
    private boolean workoutPaused;

    private Button startWorkoutButton;
    private Button endWorkoutButton;

    private int sportActivity;
    private long duration;
    private double distance;
    private double pace;
    private double calories;
    private ArrayList<List<Location>> finalPositionList;

    private final String ACTION_START = "sk.tuke.smart.makac.COMMAND_START";
    private final String ACTION_PAUSE = "sk.tuke.smart.makac.COMMAND_PAUSE";
    private final String ACTION_CONTINUE = "sk.tuke.smart.makac.COMMAND_CONTINUE";
    private final String ACTION_STOP = "sk.tuke.smart.makac.COMMAND_STOP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);
        startWorkoutButton = findViewById(R.id.button_stopwatch_start);
        endWorkoutButton = findViewById(R.id.button_stopwatch_endworkout);
    }

    public void toggleRecording(View view) {
        if (!workoutStarted) {
            startWorkoutButton.setText(R.string.stopwatch_stop);

            Intent i = new Intent(this, TrackerService.class);
            i.setAction(ACTION_START);
            startService(i);

            workoutStarted = !workoutStarted;
        }
        else {
            if (!workoutPaused) {
                startWorkoutButton.setText(R.string.stopwatch_continue);

                Intent i = new Intent(this, TrackerService.class);
                i.setAction(ACTION_PAUSE);
                startService(i);

                endWorkoutButton.setVisibility(Button.VISIBLE);
            }
            else {
                startWorkoutButton.setText(R.string.stopwatch_stop);

                Intent i = new Intent(this, TrackerService.class);
                i.setAction(ACTION_CONTINUE);
                startService(i);

                endWorkoutButton.setVisibility(Button.GONE);
            }

            workoutPaused = !workoutPaused;
        }
    }

    public void stopRecording(View view) {
        endWorkoutButton.setText(R.string.stopwatch_stop);

        Intent i = new Intent(this, TrackerService.class);
        i.setAction(ACTION_STOP);
        startService(i);

        i = new Intent(this, WorkoutDetailActivity.class);
        i.putExtra("SPORT_ACTIVITY", sportActivity);
        i.putExtra("ACTIVITY_DURATION", duration);
        i.putExtra("ACTIVITY_DISTANCE", distance);
        i.putExtra("ACTIVITY_PACE", pace);
        i.putExtra("ACTIVITY_CALORIES", calories);
        i.putExtra("ACTIVITY_POSITIONS", finalPositionList);
        startActivity(i);
    }

    private void updateCounters() {

    }
}
