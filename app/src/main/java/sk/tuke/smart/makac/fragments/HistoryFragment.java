package sk.tuke.smart.makac.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import sk.tuke.smart.makac.DatabaseConnection;
import sk.tuke.smart.makac.HistoryListAdapter;
import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.UnitChange;
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class HistoryFragment extends Fragment implements DatabaseConnection, UnitChange {
    @BindView(R.id.textview_history_noHistoryData) public TextView noHistoryDataTextView;
    @BindView(R.id.listview_history_workouts) public ListView workoutsListView;

    private static final String TAG = "HistoryFragment";

    private OnFragmentInteractionListener mListener;

    private Dao<Workout, Long> workoutDao;
    private Dao<GpsPoint, Long> gpsPointDao;

    private FragmentActivity thisFragmentActivity;

    private SharedPreferences userShPr;
    private SharedPreferences appShPr;

    private int currentDistanceUnit;

    private AlertDialog.Builder alertDialogBuilderHistory;
    private AlertDialog.Builder alertDialogBuilderDeleted;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeVariables();
        setHasOptionsMenu(true);
        thisFragmentActivity.setTitle(R.string.menu_history);
        databaseSetup();
        createClearHistoryAlertDialog();
        createClearDeletedAlertDialog();
    }

    private void initializeVariables() {
        thisFragmentActivity = getActivity();
        userShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        appShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.appshpr), Context.MODE_PRIVATE);
        currentDistanceUnit = appShPr.getInt(getString(R.string.appshpr_unit), Integer.valueOf(getString(R.string.appshpr_unit_default)));
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(thisFragmentActivity, DatabaseHelper.class);
            workoutDao = databaseHelper.workoutDao();
            gpsPointDao = databaseHelper.gpsPointDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createClearHistoryAlertDialog() {
        alertDialogBuilderHistory = new AlertDialog.Builder(thisFragmentActivity);
        alertDialogBuilderHistory.setTitle("Clear history")
                .setMessage("Do you really want to clear workout history?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        clearUserWorkoutHistory();
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

    private void createClearDeletedAlertDialog() {
        alertDialogBuilderDeleted = new AlertDialog.Builder(thisFragmentActivity);
        alertDialogBuilderDeleted.setTitle("Clear deleted workouts")
                .setMessage("Do you really want to delete these workouts forever?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        performPermanentDelete();
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
    public void onResume() {
        super.onResume();
        thisFragmentActivity.invalidateOptionsMenu();
        checkForUnitChange();
    }

    public void checkForUnitChange() {
        int newUnit = appShPr.getInt(getString(R.string.appshpr_unit), Integer.valueOf(getString(R.string.appshpr_unit_default)));
        if (currentDistanceUnit != newUnit) {
            currentDistanceUnit = newUnit;
            renderList();
            Log.i(TAG, "History list was re-rendered after unit change");
            Toast.makeText(thisFragmentActivity, "Units changed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Workout.HISTORY_REQUEST && resultCode == Workout.DELETE_RESULT)
            removeWorkoutFromList(data);
        else if (requestCode == Workout.HISTORY_REQUEST && resultCode == Workout.UPDATE_RESULT)
            renderList();
    }

    private void removeWorkoutFromList(Intent data) {
        String deletedWorkoutTitle = data.getStringExtra(IntentHelper.DATA_WORKOUT_TITLE);
        String toastMessage = deletedWorkoutTitle + " was deleted";
        Toast.makeText(thisFragmentActivity, toastMessage, Toast.LENGTH_SHORT).show();
        renderList();
    }

    private List<String> getStringifiedWorkouts(List<Workout> workouts) {
        List<String> stringifiedWorkouts = new ArrayList<>();
        for (Workout workout : workouts)
            stringifiedWorkouts.add(workout.toString(currentDistanceUnit));
        return stringifiedWorkouts;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (thisFragmentActivity.getTitle().equals(getString(R.string.menu_history))) {
            inflater.inflate(R.menu.show_deleted, menu);
            inflater.inflate(R.menu.clear_history, menu);
        }
        else if (thisFragmentActivity.getTitle().equals(getString(R.string.history_workoutsbin))) {
            inflater.inflate(R.menu.show_ended, menu);
            inflater.inflate(R.menu.clear_deleted, menu);
        }
        if (userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))))
            inflater.inflate(R.menu.sync_with_server, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);
        renderList();
        return view;
    }

    private void renderList() {
        try {
            renderList(getFilteredUserWorkouts(Workout.statusEnded), R.layout.adapter_history);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderList(List<Workout> workouts, int resource) {
        if (workouts.size() > 0) {
            renderHistoryItems(workouts, resource);
            setVisibility(View.GONE, View.VISIBLE);
        }
        else {
            setVisibility(View.VISIBLE, View.GONE);
            Toast.makeText(thisFragmentActivity, "No workouts present", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "There is no data to display");
        }
    }

    private void setVisibility(int noHistoryDataVisibility, int workoutsVisibility) {
        noHistoryDataTextView.setVisibility(noHistoryDataVisibility);
        workoutsListView.setVisibility(workoutsVisibility);
    }

    private void renderHistoryItems(List<Workout> workouts, int resource) {
        HistoryListAdapter historyListAdapter = new HistoryListAdapter(thisFragmentActivity, resource, getStringifiedWorkouts(workouts), workouts);
        workoutsListView.setAdapter(historyListAdapter);
        if (resource == R.layout.adapter_history) {
            workoutsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    long workoutId = getWorkoutId(view);
                    Intent intent = new Intent(thisFragmentActivity, WorkoutDetailActivity.class);
                    intent.putExtra(IntentHelper.DATA_WORKOUT_ID, workoutId);
                    intent.putExtra(IntentHelper.DATA_HISTORY_REQUEST, Workout.HISTORY_REQUEST);
                    startActivityForResult(intent, Workout.HISTORY_REQUEST);
                }

                private long getWorkoutId(View view) {
                    ViewGroup viewGroup1 = (ViewGroup) view;
                    ViewGroup viewGroup2 = (ViewGroup) viewGroup1.getChildAt(1);
                    TextView workoutTitleTextView = (TextView) viewGroup2.getChildAt(0);
                    return Long.valueOf(workoutTitleTextView.getTag().toString());
                }
            });
        }
        else {
            workoutsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    renewWorkout(getWorkoutId(view));
                }

                private long getWorkoutId(View view) {
                    ViewGroup viewGroup1 = (ViewGroup) view;
                    ViewGroup viewGroup2 = (ViewGroup) viewGroup1.getChildAt(0);
                    TextView workoutTitleTextView = (TextView) viewGroup2.getChildAt(1);
                    return Long.valueOf(workoutTitleTextView.getTag().toString());
                }

                private void renewWorkout(long workoutId) {
                    try {
                        Workout clickedWorkout = workoutDao.queryForId(workoutId);
                        clickedWorkout.setStatus(Workout.statusEnded);
                        workoutDao.update(clickedWorkout);
                        Toast.makeText(thisFragmentActivity, clickedWorkout.getTitle() + " renewed", Toast.LENGTH_SHORT).show();
                        showDeletedWorkouts();
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        Log.i(TAG, "Workouts displayed successfully");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    public void displayClearHistoryAlertDialog() {
        AlertDialog alertDialog = alertDialogBuilderHistory.create();
        alertDialog.show();

        Log.i(TAG, "Alert dialog is now visible.");
    }

    public void clearUserWorkoutHistory() {
        try {
            List<Workout> userEndedWorkouts = getFilteredUserWorkouts(Workout.statusEnded);
            if (userEndedWorkouts.size() == 0) {
                Toast.makeText(thisFragmentActivity, "No workout to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Workout currentWorkout : userEndedWorkouts) {
                currentWorkout.setStatus(Workout.statusDeleted);
                workoutDao.update(currentWorkout);
            }

            renderList();

            Toast.makeText(thisFragmentActivity, "All workouts were deleted", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "All workouts deleted");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void showDeletedWorkouts() {
        try {
            thisFragmentActivity.setTitle(R.string.history_workoutsbin);
            thisFragmentActivity.invalidateOptionsMenu();
            renderList(getFilteredUserWorkouts(Workout.statusDeleted), R.layout.adapter_history_bin);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void showEndedWorkouts() {
        thisFragmentActivity.setTitle(R.string.menu_history);
        thisFragmentActivity.invalidateOptionsMenu();
        renderList();
    }

    private List<Workout> getFilteredUserWorkouts(int workoutStatus) throws SQLException {
        long currentUserId = userShPr.getLong(getString(R.string.usershpr_userid), Long.valueOf(getString(R.string.usershpr_userid_default)));
        List<Workout> userWorkouts = workoutDao.queryForEq(Workout.COLUMN_USERID, currentUserId);
        List<Workout> filteredUserWorkouts = new ArrayList<>();

        switch (workoutStatus) {
            case Workout.statusEnded:
                for (Workout workout : userWorkouts) {
                    if (workout.getStatus() == Workout.statusEnded)
                        filteredUserWorkouts.add(workout);
                }
                break;
            case Workout.statusDeleted:
                for (Workout workout : userWorkouts) {
                    if (workout.getStatus() == Workout.statusDeleted)
                        filteredUserWorkouts.add(workout);
                }
                break;
            default:
                break;
        }

        return filteredUserWorkouts;
    }

    public void displayClearDeletedAlertDialog() {
        AlertDialog alertDialog = alertDialogBuilderDeleted.create();
        alertDialog.show();

        Log.i(TAG, "Alert dialog is now visible.");
    }

    private void performPermanentDelete() {
        try {
            List<Workout> userDeletedWorkouts = getFilteredUserWorkouts(Workout.statusDeleted);
            if (userDeletedWorkouts.size() == 0) {
                Toast.makeText(thisFragmentActivity, "No workout to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            // DELETE ALL GPS POINTS ASSOCIATED WITH WORKOUTS
            for (Workout workout : userDeletedWorkouts)
                gpsPointDao.delete(gpsPointDao.queryForEq(GpsPoint.COLUMN_WORKOUTID, workout.getId()));

            workoutDao.delete(userDeletedWorkouts);

            renderList(new ArrayList<Workout>(), R.layout.adapter_history_bin);

            Toast.makeText(thisFragmentActivity, "All workouts were permanentally deleted", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "All workouts were permanentally deleted");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public interface OnFragmentInteractionListener {}
}
