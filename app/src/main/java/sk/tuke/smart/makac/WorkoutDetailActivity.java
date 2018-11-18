package sk.tuke.smart.makac;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.helpers.IntentHelper;
import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.services.TrackerService;

public class WorkoutDetailActivity extends AppCompatActivity {
    @BindView(R.id.textview_workoutdetail_workouttitle) TextView workoutTitleTextView;
    @BindView(R.id.textview_workoutdetail_sportactivity) TextView sportActivityTextView;
    @BindView(R.id.textview_workoutdetail_activitydate) TextView activityDateTextView;
    @BindView(R.id.textview_workoutdetail_valueduration) TextView valueDurationTextView;
    @BindView(R.id.textview_workoutdetail_valuecalories) TextView valueCaloriesTextView;
    @BindView(R.id.textview_workoutdetail_valuedistance) TextView valueDistanceTextView;
    @BindView(R.id.textview_workoutdetail_valueavgpace) TextView valueAvgPaceTextView;

    @BindString(R.string.workoutdetail_workoutname) String workoutTitle;
    @BindString(R.string.share_message) String shareMessage;

    private Intent intent;

    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog alertDialog;

    private final String TAG = "WorkoutDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);
        ButterKnife.bind(this);

        intent = getIntent();
        extrasRenderer(intent.getBundleExtra(IntentHelper.DATA_BUNDLE));
//        createAlertDialog();
    }

    private void extrasRenderer(Bundle bundle) {
        workoutTitleRenderer();
        sportActivityRenderer(bundle.getInt(IntentHelper.DATA_SPORT, 0));
        activityDateRenderer();
        durationRenderer(bundle.getLong(IntentHelper.DATA_DURATION, 0));
        distanceRenderer(bundle.getDouble(IntentHelper.DATA_DISTANCE, 0));
        avgPaceRenderer(bundle.getDouble(IntentHelper.DATA_PACE, 0));
        caloriesRenderer(bundle.getDouble(IntentHelper.DATA_CALORIES, 0));
    }

    private void workoutTitleRenderer() {
        workoutTitleTextView.setText(workoutTitle);
        Log.i(TAG, "");
    }

    private void sportActivityRenderer(int sportActivity) {
        sportActivityTextView.setText(getSportActivityString(sportActivity));
    }

    private String getSportActivityString(int sportActivity) {
        switch (sportActivity) {
            case 0: return "Running";
            case 1: return "Walking";
            case 2: return "Cycling";
            default: return "Unknown sport";
        }
    }

    private void activityDateRenderer() {
        String activityDateString = SimpleDateFormat.getDateTimeInstance().format(new Date());
        activityDateTextView.setText(activityDateString);
    }

    private void durationRenderer(long duration) {
        String durationString = MainHelper.formatDuration(duration);
        valueDurationTextView.setText(durationString);
    }

    private void distanceRenderer(double distance) {
        String distanceString = MainHelper.formatDistance(distance) + " km";
        valueDistanceTextView.setText(distanceString);
    }

    private void avgPaceRenderer(double avgPace) {
        String avgPaceString = MainHelper.formatPace(avgPace) + " min/km";
        valueAvgPaceTextView.setText(avgPaceString);
    }

    private void caloriesRenderer(double calories) {
        String caloriesString = MainHelper.formatCalories(calories) + " kcal";
        valueCaloriesTextView.setText(caloriesString);
    }

//    private void createAlertDialog() {
////        String shareMessage = "I was out for " + getSportActivityString(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getInt(IntentHelper.DATA_SPORT, 0)).toLowerCase() + ". " +
////                "I tracked " + MainHelper.formatDistance(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getDouble(IntentHelper.DATA_DISTANCE, 0)) + " km " +
////                "in " + MainHelper.formatDuration(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getLong(IntentHelper.DATA_DURATION, 0)) + ".";
//        String shareMessage = this.shareMessage;
//        shareMessage = shareMessage
//                .replace("WORKOUT_TYPE", getSportActivityString(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getInt(IntentHelper.DATA_SPORT, 0)).toLowerCase())
//                .replace("DISTANCE", MainHelper.formatDistance(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getDouble(IntentHelper.DATA_DISTANCE, 0)))
//                .replace("UNIT", "km")
//                .replace("DURATION", MainHelper.formatDuration(intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getLong(IntentHelper.DATA_DURATION, 0)));
//
//        EditText editText = new EditText(this);
//        editText.setText(shareMessage);
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//        editText.setLayoutParams(lp);
//
//        alertDialogBuilder = new AlertDialog.Builder(this);
//        alertDialogBuilder
//                .setView(editText)
//                .setTitle("Share results")
//                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        new Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
//                        dialogInterface.dismiss();
//                    }
//                })
//                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.dismiss();
//                    }
//                });
//    }

    @OnClick(R.id.button_workoutdetail_showmap)
    public void showMapsActivity(View view) {
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(IntentHelper.DATA_POSITIONS, intent.getBundleExtra(IntentHelper.DATA_BUNDLE).getSerializable(IntentHelper.DATA_POSITIONS));
        mapsIntent.putExtra(IntentHelper.DATA_BUNDLE, bundle);
        startActivity(mapsIntent);
    }

//    @OnClick(R.id.button_workoutdetail_emailshare)
//    public void showAlertDialogEmail(View view) {
//        alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
//    }
}
