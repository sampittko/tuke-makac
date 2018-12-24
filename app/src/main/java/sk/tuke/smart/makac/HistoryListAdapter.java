package sk.tuke.smart.makac;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_history, parent, false);
        setIcon(convertView);
        setTitle(convertView, position);
        setDate(convertView, position);
        setSportActivity(convertView, position);
        return convertView;
    }

    // TODO workout icon set by user and default icon depending on sport activity
    private void setIcon(View convertView) {
        ImageView iconImageView = convertView.findViewById(R.id.imageview_history_icon);
        iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
    }

    // TODO workout title set by user
    private void setTitle(View convertView, int position) {
        TextView titleTextView = convertView.findViewById(R.id.textview_history_title);
        titleTextView.setText(workouts.get(position).getTitle());
    }

    private void setDate(View convertView, int position) {
        TextView dateTimeTextView = convertView.findViewById(R.id.textview_history_datetime);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd.MM. yyyy", Locale.ENGLISH);
        dateTimeTextView.setText(sdf.format(workouts.get(position).getCreated().getTime()));
    }

    private void setSportActivity(View convertView, int position) {
        String workout = getItem(position);
        TextView sportActivityTextView = convertView.findViewById(R.id.textview_history_sportactivity);
        sportActivityTextView.setText(workout);
    }
}