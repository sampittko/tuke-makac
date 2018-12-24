package sk.tuke.smart.makac;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.helpers.SportActivities;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class WorkoutDetailActivity extends AppCompatActivity {
    @BindView(R.id.textview_workoutdetail_workouttitle) TextView workoutTitleTextView;
    @BindView(R.id.textview_workoutdetail_sportactivity) TextView sportActivityTextView;
    @BindView(R.id.textview_workoutdetail_activitydate) TextView activityDateTextView;
    @BindView(R.id.textview_workoutdetail_valueduration) TextView valueDurationTextView;
    @BindView(R.id.textview_workoutdetail_valuecalories) TextView valueCaloriesTextView;
    @BindView(R.id.textview_workoutdetail_valuedistance) TextView valueDistanceTextView;
    @BindView(R.id.textview_workoutdetail_valueavgpace) TextView valueAvgPaceTextView;
    @BindView(R.id.textview_workoutdetail_labelshowmap) TextView showMapTextView;
    @BindView(R.id.button_workoutdetail_showmap) Button showMapButton;

    @BindString(R.string.share_message) String shareMessage;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private long currentWorkoutId;

    private Dao<Workout, Long> workoutDao;

    private final String TAG = "WorkoutDetailActivity";

    private ArrayList<List<Location>> finalPositionList;
    private int sportActivity;
    private long duration;
    private double distance, avgPace, totalCalories;
    private Date workoutDate;
    private String workoutTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLayout();
        databaseSetup();
        try {
            currentWorkoutId = getIntent().getLongExtra(IntentHelper.DATA_WORKOUT, -1);
        }
        catch(NullPointerException e) {
            Log.e(TAG, "Intent is missing workout ID.");
        }
        retrieveWorkoutValues();
        mapButtonVisibilityCheck();
        createShareAlertDialog();
        renderValues();
    }

    private void initializeLayout() {
        setContentView(R.layout.activity_workout_detail);
        ButterKnife.bind(this);
        setTitle(R.string.workout_review);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            workoutDao = databaseHelper.workoutDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveWorkoutValues() {
        try {
            Workout currentWorkout = workoutDao.queryForId(currentWorkoutId);
            sportActivity = currentWorkout.getSportActivity();
            duration = currentWorkout.getDuration();
            totalCalories = currentWorkout.getTotalCalories();
            avgPace = currentWorkout.getPaceAvg();
            distance = currentWorkout.getDistance();
            // TODO
            finalPositionList = null;
            workoutDate = currentWorkout.getCreated();
            workoutTitle = currentWorkout.getTitle();
            Log.i(TAG, "Values from local database retrieved successfully");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        menuInflater.inflate(R.menu.delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        performCorrespondingActionForMenuItem(item.getItemId());
        return true;
    }

    private void performCorrespondingActionForMenuItem(int itemId) {
        switch(itemId) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            // TODO delete workout
            case R.id.action_delete:
                break;
            // TODO on back pressed, perform back action
            case 16908332:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void renderValues() {
        workoutTitleTextView.setText(workoutTitle);
        sportActivityTextView.setText(SportActivities.getSportActivityStringFromInt(sportActivity));
        activityDateTextView.setText(MainHelper.sToDate(workoutDate.getTime()));
        valueDurationTextView.setText(MainHelper.formatDuration(MainHelper.msToS(duration)));
        String distanceString = MainHelper.formatDistance(distance) + " km";
        valueDistanceTextView.setText(distanceString);
        String avgPaceString = MainHelper.formatPace(avgPace) + " min/km";
        valueAvgPaceTextView.setText(avgPaceString);
        String caloriesString = MainHelper.formatCalories(totalCalories) + " kcal";
        valueCaloriesTextView.setText(caloriesString);
    }


    private void mapButtonVisibilityCheck() {
        if (finalPositionList == null || finalPositionList.size() == 1 && finalPositionList.get(0).size() < 2) {
            showMapButton.setVisibility(View.GONE);
            showMapTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void createShareAlertDialog() {
        String shareMessage = this.shareMessage;
        shareMessage = shareMessage
                .replace("WORKOUT_TYPE", SportActivities.getSportActivityStringFromInt(sportActivity).toLowerCase())
                .replace("DISTANCE", MainHelper.formatDistance(distance)
                .replace("UNIT", "km")
                .replace("DURATION", MainHelper.formatDuration(MainHelper.msToS(duration))));

        EditText editText = new EditText(this);
        editText.setText(shareMessage);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        editText.setLayoutParams(lp);

        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setView(editText)
                .setTitle("Share results")
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
    }

    @OnClick(R.id.button_workoutdetail_showmap)
    public void showMapsActivity(View view) {
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        mapsIntent.putExtra(IntentHelper.DATA_POSITIONS, finalPositionList);
        startActivity(mapsIntent);
    }

    @OnClick({ R.id.button_workoutdetail_emailshare, R.id.button_workoutdetail_fbsharebtn, R.id.button_workoutdetail_twittershare, R.id.button_workoutdetail_gplusshare })
    public void showAlertDialogEmail(View view) {
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
