package sk.tuke.smart.makac;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import sk.tuke.smart.makac.model.Workout;

public class HistoryListAdapter extends ArrayAdapter<String> {
    private List<Workout> workouts;
    private Context context;
    private int resource;

    public HistoryListAdapter(Context context, int resource, List<Workout> workouts) {
        super(context, resource);
        this.context = context;
        this.resource = resource;
        this.workouts = workouts;
    }

    // TODO getView
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
}
