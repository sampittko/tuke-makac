package sk.tuke.smart.makac.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import sk.tuke.smart.makac.R;

public class AboutFragment extends Fragment {
    private FragmentActivity thisFragmentActivity;

    private SharedPreferences userShPr;

    public AboutFragment() {
    }

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.menu_about);
        setHasOptionsMenu(true);
        thisFragmentActivity = getActivity();
        userShPr = thisFragmentActivity.getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))))
            inflater.inflate(R.menu.sync_with_server, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        thisFragmentActivity.invalidateOptionsMenu();
    }

    public interface OnFragmentInteractionListener {}
}
