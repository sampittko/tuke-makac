package sk.tuke.smart.makac;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
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
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class WorkoutDetailActivity extends AppCompatActivity implements OnMapReadyCallback, DatabaseConnection {
    @BindView(R.id.textview_workoutdetail_workouttitle) public TextView workoutTitleTextView;
    @BindView(R.id.textview_workoutdetail_sportactivity) public TextView sportActivityTextView;
    @BindView(R.id.textview_workoutdetail_activitydate) public TextView activityDateTextView;
    @BindView(R.id.textview_workoutdetail_valueduration) public TextView valueDurationTextView;
    @BindView(R.id.textview_workoutdetail_valuecalories) public TextView valueCaloriesTextView;
    @BindView(R.id.textview_workoutdetail_valuedistance) public TextView valueDistanceTextView;
    @BindView(R.id.textview_workoutdetail_valueavgpace) public TextView valueAvgPaceTextView;
    @BindView(R.id.textview_workoutdetail_labelshowmap) public TextView showMapTextView;
    @BindView(R.id.button_workoutdetail_showmap) public Button showMapButton;

    @BindString(R.string.share_message) public String shareMessage;

    private AlertDialog.Builder alertDialogBuilderShare;
    private AlertDialog.Builder alertDialogBuilderTitle;
    private AlertDialog alertDialog;

    private Workout currentWorkout;

    private List<GpsPoint> currentGpsPoints;

    private Dao<Workout, Long> workoutDao;
    private Dao<GpsPoint, Long> gpsPointDao;

    private final String TAG = "WorkoutDetailActivity";

    private ArrayList<List<Location>> finalPositionList;
    private int sportActivity;
    private long duration;
    private double distance, avgPace, totalCalories;
    private Date workoutDate, workoutLastUpdate;
    private String workoutTitle;

    private SupportMapFragment mapFragment;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseSetup();
        initializeLayout();
        retrieveWorkoutValues();
        renderValues();
        mapEntitiesVisibilityCheck();
        createShareAlertDialog();
        createEditTitleAlertDialog();
    }

    private void initializeLayout() {
        setContentView(R.layout.activity_workout_detail);
        ButterKnife.bind(this);
        setTitle(R.string.workout_review);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_workoutdetail_map);
        mapFragment.getMapAsync(this);
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            workoutDao = databaseHelper.workoutDao();
            gpsPointDao = databaseHelper.gpsPointDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveWorkoutValues() {
        try {
            Long currentWorkoutId = getIntent().getLongExtra(IntentHelper.DATA_WORKOUT_ID, -1);
            currentWorkout = workoutDao.queryForId(currentWorkoutId);
            sportActivity = currentWorkout.getSportActivity();
            duration = currentWorkout.getDuration();
            totalCalories = currentWorkout.getTotalCalories();
            avgPace = currentWorkout.getPaceAvg();
            distance = currentWorkout.getDistance();
            currentGpsPoints = gpsPointDao.queryForEq("workout_id", currentWorkoutId);
            finalPositionList = MainHelper.getFinalPositionList(currentGpsPoints);
            workoutDate = currentWorkout.getCreated();
            workoutLastUpdate = currentWorkout.getLastUpdate();
            workoutTitle = currentWorkout.getTitle();
            Log.i(TAG, "Values from local database retrieved successfully");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO unit switch
    private void renderValues() {
        try {
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
        catch (NullPointerException e) {
            e.printStackTrace();
            // fix for test where NullPointerException occured in case of calling getTime() on workoutDate
            if (workoutDate == null) {
                if (workoutLastUpdate != null)
                    activityDateTextView.setText(MainHelper.sToDate(workoutLastUpdate.getTime()));
                else
                    activityDateTextView.setText(MainHelper.sToDate(new Date().getTime()));
            }
        }
    }
    private void mapEntitiesVisibilityCheck() {
        if (finalPositionList == null || finalPositionList.size() == 1 && finalPositionList.get(0).size() < 2) {
            getSupportFragmentManager().beginTransaction().hide(mapFragment).commit();
            showMapButton.setVisibility(View.GONE);
            showMapTextView.setVisibility(View.GONE);
        }
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

        alertDialogBuilderShare = new AlertDialog.Builder(this);
        alertDialogBuilderShare
                .setView(editText)
                .setTitle("Share results")
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        alertDialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        alertDialog.dismiss();
                    }
                });
    }

    private void createEditTitleAlertDialog() {
        final EditText editText = new EditText(this);
        editText.setText(currentWorkout.getTitle());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        editText.setLayoutParams(lp);
        editText.setFocusableInTouchMode(true);

        alertDialogBuilderTitle = new AlertDialog.Builder(this);
        alertDialogBuilderTitle
                .setView(editText)
                .setTitle("Edit workout title")
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateWorkoutTitle(editText.getText().toString());
                        dialogInterface.dismiss();
                        alertDialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        editText.setText(currentWorkout.getTitle());
                        alertDialog.dismiss();
                    }
                });
    }

    private void updateWorkoutTitle(String newTitle) {
        try {
            currentWorkout.setTitle(newTitle);
            workoutDao.update(currentWorkout);
            workoutTitleTextView.setText(newTitle);
            Toast.makeText(this, "Workout title updated", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Workout title updated");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    @OnClick(R.id.button_workoutdetail_showmap)
    public void showMapsActivity(View view) {
        showMapsActivity();
    }

    public void showMapsActivity() {
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        mapsIntent.putExtra(IntentHelper.DATA_WORKOUT_ID, currentWorkout.getId());
        startActivity(mapsIntent);
    }

    @OnClick({ R.id.button_workoutdetail_emailshare, R.id.button_workoutdetail_fbsharebtn, R.id.button_workoutdetail_twittershare, R.id.button_workoutdetail_gplusshare })
    public void showAlertDialogEmail(View view) {
        alertDialog = alertDialogBuilderShare.create();
        alertDialog.show();
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
            case R.id.action_delete:
                deleteWorkout();
                break;
            case android.R.id.home:
                setResult(Workout.CLOSE_RESULT);
                finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void deleteWorkout() {
        try {
            currentWorkout.setStatus(Workout.statusDeleted);
            workoutDao.update(currentWorkout);
            gpsPointDao.delete(currentGpsPoints);
            Log.i(TAG, "Workout data deleted");
            Intent data = new Intent();
            data.putExtra(IntentHelper.DATA_WORKOUT_TITLE, currentWorkout.getTitle());
            setResult(Workout.DELETE_RESULT, data);
            finish();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Workout.CLOSE_RESULT);
        super.onBackPressed();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                showMapsActivity();
            }
        });
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        renderRoute();
        try {
            setCamera();
        }
        catch (IndexOutOfBoundsException e) {
            Log.i(TAG, "No map to render");
        }
    }

    private void renderRoute() {
        for (List<Location> locationList : finalPositionList) {
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
            for (Location location : locationList)
                polylineOptions.add(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addPolyline(polylineOptions);
        }
        Log.i(TAG, "Route was rendered");
    }

    private void setCamera() throws IndexOutOfBoundsException {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        LatLng startPoint = getStartPoint();
        LatLng endPoint = getEndPoint();

        builder.include(startPoint);
        builder.include(endPoint);

        LatLngBounds bounds = builder.build();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int widthPixels = displayMetrics.widthPixels;
        int heightPixels = displayMetrics.heightPixels / 3;

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, widthPixels, heightPixels, 100);
        mMap.animateCamera(cu);

        Log.i(TAG, "Camera setup successful");
    }

    private LatLng getStartPoint() throws IndexOutOfBoundsException {
        return new LatLng(finalPositionList.get(0).get(0).getLatitude(), finalPositionList.get(0).get(0).getLongitude());
    }

    private LatLng getEndPoint() {
        List<Location> lastLocationList = finalPositionList.get(finalPositionList.size() - 1);
        return new LatLng(lastLocationList.get(lastLocationList.size() - 1).getLatitude(), lastLocationList.get(lastLocationList.size() - 1).getLongitude());
    }

    @OnClick(R.id.textview_workoutdetail_workouttitle)
    public void showTitleEditText(View view) {
        alertDialog = alertDialogBuilderTitle.create();
        alertDialog.show();
    }
}
