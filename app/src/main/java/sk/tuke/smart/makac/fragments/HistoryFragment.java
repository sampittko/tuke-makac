package sk.tuke.smart.makac.fragments;

import android.content.Context;
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
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.config.DatabaseHelper;

public class HistoryFragment extends Fragment implements DatabaseConnection {
    @BindView(R.id.textview_history_noHistoryData) public TextView noHistoryDataTextView;
    @BindView(R.id.listview_history_workouts) public ListView workoutsListView;

    private static final String TAG = "HistoryFragment";

    private OnFragmentInteractionListener mListener;

    private Dao<Workout, Long> workoutDao;

    private FragmentActivity thisFragmentActivity;

    private SharedPreferences userShPr;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisFragmentActivity = getActivity();
        thisFragmentActivity.setTitle(R.string.menu_history);
        setHasOptionsMenu(true);
        databaseSetup();
        userShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(thisFragmentActivity, DatabaseHelper.class);
            workoutDao = databaseHelper.workoutDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        thisFragmentActivity.invalidateOptionsMenu();
    }

    private void displayHistoryItems(List<Workout> endedWorkouts) {
        HistoryListAdapter historyListAdapter = new HistoryListAdapter(thisFragmentActivity, R.layout.adapter_history, getStringifiedWorkouts(endedWorkouts), endedWorkouts);
        workoutsListView.setAdapter(historyListAdapter);
        workoutsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                long workoutId = getWorkoutId(view);
                Intent intent = new Intent(thisFragmentActivity, WorkoutDetailActivity.class);
                intent.putExtra(IntentHelper.DATA_WORKOUT_ID, workoutId);
                startActivityForResult(intent, Workout.DELETE_REQUEST);
            }

            private long getWorkoutId(View view) {
                ViewGroup viewGroup1 = (ViewGroup) view;
                ViewGroup viewGroup2 = (ViewGroup) viewGroup1.getChildAt(1);
                TextView workoutTitleTextView = (TextView) viewGroup2.getChildAt(0);
                return Long.valueOf(workoutTitleTextView.getTag().toString());
            }
        });
        Log.i(TAG, "Workouts displayed successfully");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Workout.DELETE_REQUEST && resultCode == Workout.DELETE_RESULT)
            removeWorkoutFromList(data);
    }

    private void removeWorkoutFromList(Intent data) {
        long deletedWorkoutId = data.getLongExtra(IntentHelper.DATA_WORKOUT_ID, 0);
        String deletedWorkoutTitle = data.getStringExtra(IntentHelper.DATA_WORKOUT_TITLE);
        String toastMessage = deletedWorkoutTitle + " was deleted (ID: " + deletedWorkoutId + ")";
        Toast.makeText(thisFragmentActivity, toastMessage, Toast.LENGTH_SHORT).show();
        displayContent();
    }

    private List<String> getStringifiedWorkouts(List<Workout> workouts) {
        List<String> stringifiedWorkouts = new ArrayList<>();
        for (Workout workout : workouts)
            stringifiedWorkouts.add(workout.toString());
        return stringifiedWorkouts;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.clear_history, menu);
        if (userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))))
            inflater.inflate(R.menu.sync_with_server, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);
        displayContent();
        return view;
    }

    private void displayContent() {
        try {
            List<Workout> endedWorkouts = workoutDao.queryForEq("status", Workout.statusEnded);
            if (endedWorkouts.size() > 0)
                displayHistoryItems(endedWorkouts);
            else {
                noHistoryDataTextView.setVisibility(View.VISIBLE);
                Log.i(TAG, "There is no data to display");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
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

    public interface OnFragmentInteractionListener {}
}
