package sk.tuke.smart.makac;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class HistoryListAdapter extends ArrayAdapter<String> {
    public HistoryListAdapter(Context context, int resource, List<String> workouts) {
        super(context, resource, workouts);
    }

    // TODO getView
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        String workout = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_history, parent, false);
        }
        TextView sportActivityTextView = (TextView) convertView.findViewById(R.id.textview_history_sportactivity);
        sportActivityTextView.setText(workout);
        return convertView;
    }
}