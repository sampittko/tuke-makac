package sk.tuke.smart.makac;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import butterknife.BindString;
import butterknife.BindView;
import sk.tuke.smart.makac.helpers.IntentHelper;

public class WorkoutDetailActivity extends AppCompatActivity {
    @BindView(R.id.textview_workoutdetail_workouttitle) TextView workoutTitleTextView;
    @BindView(R.id.textview_workoutdetail_sportactivity) TextView sportActivityTextView;
    @BindView(R.id.textview_workoutdetail_activitydate) TextView activityDateTextView;
    @BindView(R.id.textview_workoutdetail_valueduration) TextView valueDurationTextView;
    @BindView(R.id.textview_workoutdetail_valuecalories) TextView valueCaloriesTextView;
    @BindView(R.id.textview_workoutdetail_valuedistance) TextView valueDistanceTextView;
    @BindView(R.id.textview_workoutdetail_valueavgpace) TextView valueAvgPaceTextView;

    @BindString(R.string.workoutdetail_workoutname) String workoutName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);
        // TODO intent null
        renderExtras(getIntent());
    }

    private void renderExtras(Intent intent) {
        String sportActivity = intent.getIntExtra(IntentHelper.DATA_SPORT, 0) == 0 ? "Running" : "Other sport";
        sportActivityTextView.setText(sportActivity);

        String duration = String.valueOf(intent.getLongExtra(IntentHelper.DATA_DURATION, 0));
        valueDurationTextView.setText(duration);

        String distance = String.valueOf(intent.getDoubleExtra(IntentHelper.DATA_DISTANCE, 0)) + "km";
        valueDistanceTextView.setText(distance);

        String pace = String.valueOf(intent.getDoubleExtra(IntentHelper.DATA_PACE, 0)) + "min/km";
        valueAvgPaceTextView.setText(pace);

        String calories = String.valueOf(intent.getDoubleExtra(IntentHelper.DATA_CALORIES, 0) + "kcal");
        valueCaloriesTextView.setText(calories);

        activityDateTextView.setText(SimpleDateFormat.getDateTimeInstance().toString());

        workoutTitleTextView.setText(workoutName);

        // TODO parceableArrayList
    }
}
