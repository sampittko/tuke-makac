package sk.tuke.smart.makac;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import sk.tuke.smart.makac.helpers.MainHelper;
import sk.tuke.smart.makac.model.Workout;

public class HistoryListAdapter extends ArrayAdapter<String> {
    private List<Workout> workouts;

    public HistoryListAdapter(Context context, int resource, List<String> stringifiedWorkouts, List<Workout> workouts) {
        super(context, resource, stringifiedWorkouts);
        this.workouts = workouts;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_history, parent, false);
        }
        Workout currentWorkout = workouts.get(position);
        setIcon(convertView);
        setTitle(convertView, currentWorkout);
        setDate(convertView, currentWorkout);
        setSportActivity(convertView, position);
        return convertView;
    }

    // TODO workout icon
    private void setIcon(View convertView) {
        ImageView iconImageView = convertView.findViewById(R.id.imageview_history_icon);
        iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
    }

    private void setTitle(View convertView, Workout currentWorkout) {
        TextView titleTextView = convertView.findViewById(R.id.textview_history_title);
        titleTextView.setText(currentWorkout.getTitle());
        titleTextView.setTag(currentWorkout.getId());
    }

    private void setDate(View convertView, Workout currentWorkout) {
        TextView dateTimeTextView = convertView.findViewById(R.id.textview_history_datetime);
        dateTimeTextView.setText(MainHelper.sToDate(currentWorkout.getCreated().getTime()));
    }

    private void setSportActivity(View convertView, int position) {
        String workout = getItem(position);
        TextView sportActivityTextView = convertView.findViewById(R.id.textview_history_sportactivity);
        sportActivityTextView.setText(workout);
    }
}