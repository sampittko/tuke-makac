package sk.tuke.smart.makac.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.WorkoutDetailActivity;
import sk.tuke.smart.makac.exceptions.SensorNotPresentException;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.services.TrackerService;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StopwatchFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StopwatchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopwatchFragment extends Fragment {
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

    private FragmentActivity activity;

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

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public StopwatchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
     * @return A new instance of fragment StopwatchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StopwatchFragment newInstance() {
//        StopwatchFragment fragment = new StopwatchFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return new StopwatchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();
        activity.setTitle(R.string.app_name);

        try {
            checkGPS();
        }
        catch(SensorNotPresentException e) {
            Log.e(TAG, "GPS sensor is missing so application cannot be started.");
            activity.finish();
        }

        createAlertDialog();

        intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_TICK);
        intentFilter.addAction(IntentHelper.ACTION_GPS);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    private void checkGPS() throws SensorNotPresentException {
        checkSensorPresence();
        checkLocationPermissions();
    }

    private void checkSensorPresence() throws SensorNotPresentException {
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(activity, "Missing GPS sensor in device. Application closing.", Toast.LENGTH_LONG).show();
            throw new SensorNotPresentException();
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},0);
            Log.i(TAG, "Location permissions requested.");
        }
        else
            Log.i(TAG, "Location permissions OK.");
    }

    private void createAlertDialog() {
        alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setTitle("Stop workout")
                .setMessage("Do you really want to stop recording?")
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveLatestPositionList();
                        endWorkoutButton.setText(stopString);

                        Intent intent1 = new Intent(activity, TrackerService.class);
                        intent1.setAction(IntentHelper.ACTION_STOP);
                        activity.startService(intent1);

                        Intent intent2 = new Intent(activity, WorkoutDetailActivity.class);
                        intent2.putExtra(IntentHelper.DATA_SPORT, sportActivity);
                        intent2.putExtra(IntentHelper.DATA_DURATION, duration);
                        intent2.putExtra(IntentHelper.DATA_DISTANCE, distance);
                        intent2.putExtra(IntentHelper.DATA_PACE, countAvgPace());
                        intent2.putExtra(IntentHelper.DATA_CALORIES, calories);
                        intent2.putExtra(IntentHelper.DATA_POSITIONS, finalPositionList);
                        startActivity(intent2);
                        dialogInterface.dismiss();
                        activity.finish();
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
    public void onPause() {
        super.onPause();
        activity.unregisterReceiver(broadcastReceiver);
        if (!workoutStarted || workoutPaused)
            activity.stopService(new Intent(activity, TrackerService.class));

        Log.i(TAG, "Receiver unregistered.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stopwatch, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.registerReceiver(broadcastReceiver, intentFilter);
        if (!workoutStarted)
            activity.startService(new Intent(activity, TrackerService.class));

        Log.i(TAG, "Receiver registered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(activity, TrackerService.class);
        activity.stopService(intent);
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
        Intent intent = new Intent(activity, TrackerService.class);
        intent.setAction(intentAction);
        activity.startService(intent);

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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
//         TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
    }
}
