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
    private static final String TAG = "AboutFragment";

    private OnFragmentInteractionListener mListener;

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
        userShPr = thisFragmentActivity.getSharedPreferences("user", Context.MODE_PRIVATE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (userShPr.getBoolean("userSignedIn", false))
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

    // TODO onAttach
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

    // TODO onDetach
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {}
}
