package sk.tuke.smart.makac;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.model.Workout;

public class HistoryListAdapter extends ArrayAdapter<String> {
    private List<Workout> workouts;
    private List<String> stringifiedWorkouts;
    private boolean isBin;

    public HistoryListAdapter(Context context, int resource, List<String> stringifiedWorkouts, List<Workout> workouts) {
        super(context, resource, stringifiedWorkouts);
        this.workouts = workouts;
        this.stringifiedWorkouts = stringifiedWorkouts;
        isBin = resource == R.layout.adapter_history_bin;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(!isBin ? R.layout.adapter_history : R.layout.adapter_history_bin, parent, false);
        Workout currentWorkout = workouts.get(workouts.size() - position - 1);
        setIcon(convertView, currentWorkout.getSportActivity());
        setTitle(convertView, currentWorkout);
        setDate(convertView, currentWorkout);
        setSportActivity(convertView, position);
        return convertView;
    }

    private void setIcon(View convertView, int sportActivity) {
        ImageView iconImageView;
        if (!isBin)
            iconImageView = convertView.findViewById(R.id.imageview_history_icon);
        else
            iconImageView = convertView.findViewById(R.id.imageview_history_icon_bin);
        switch (sportActivity) {
            case 0:
                iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
                break;
            case 1:
                iconImageView.setImageResource(R.drawable.ic_directions_walk_blue_24dp);
                break;
            case 2:
                iconImageView.setImageResource(R.drawable.ic_motorcycle_blue_24dp);
                break;
            default:
                iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
                break;
        }
    }

    private void setTitle(View convertView, Workout currentWorkout) {
        TextView titleTextView;
        if (!isBin)
            titleTextView = convertView.findViewById(R.id.textview_history_title);
        else
            titleTextView = convertView.findViewById(R.id.textview_history_title_bin);
        titleTextView.setText(currentWorkout.getTitle());
        titleTextView.setTag(currentWorkout.getId());
    }

    private void setDate(View convertView, Workout currentWorkout) {
        TextView dateTimeTextView;
        if (!isBin)
            dateTimeTextView = convertView.findViewById(R.id.textview_history_datetime);
        else
            dateTimeTextView = convertView.findViewById(R.id.textview_history_datetime_bin);
        // fix for test where NullPointerException occured in case of calling getTime() on workoutDate
        if (currentWorkout.getCreated() == null) {
            if (currentWorkout.getLastUpdate() != null)
                dateTimeTextView.setText(MainHelper.sToDate(currentWorkout.getLastUpdate().getTime()));
            else
                dateTimeTextView.setText(MainHelper.sToDate(new Date().getTime()));
        }
        else
            dateTimeTextView.setText(MainHelper.sToDate(currentWorkout.getCreated().getTime()));
    }

    private void setSportActivity(View convertView, int position) {
        String workout = getItem(position);
        TextView sportActivityTextView;
        if (!isBin)
            sportActivityTextView = convertView.findViewById(R.id.textview_history_sportactivity);
        else
            sportActivityTextView = convertView.findViewById(R.id.textview_history_sportactivity_bin);
        sportActivityTextView.setText(workout);
    }

    @Override
    public String getItem(int position) {
        return stringifiedWorkouts.get(getCount() - position - 1);
    }
}