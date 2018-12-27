package sk.tuke.smart.makac;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.stetho.Stetho;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import sk.tuke.smart.makac.fragments.AboutFragment;
import sk.tuke.smart.makac.fragments.HistoryFragment;
import sk.tuke.smart.makac.fragments.StopwatchFragment;
import sk.tuke.smart.makac.model.User;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        AboutFragment.OnFragmentInteractionListener,
        HistoryFragment.OnFragmentInteractionListener,
        StopwatchFragment.OnFragmentInteractionListener,
        DatabaseConnection {
    private static final String TAG = "MainActivity";

    private NavigationView navigationView;

    private MainActivity thisActivity;

    private GoogleSignInAccount account;

    private Dao<User, Long> userDao;

    private SharedPreferences userShPr;
    private SharedPreferences appShPr;

    private TextView userNameTextView;
    private ImageView userImageImageView;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;
        Stetho.initializeWithDefaults(this);
        databaseSetup();
        initializeLayout();
        userShPr = getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        appShPr = getSharedPreferences(getString(R.string.appshpr), Context.MODE_PRIVATE);
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            userDao = databaseHelper.userDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeLayout() {
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        setNavigationHeaderView();

        displayStopwatchFragment();
    }

    private void setNavigationHeaderView() {
        View.OnClickListener openLoginOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(thisActivity, LoginActivity.class));
            }
        };

        View headerview = navigationView.getHeaderView(0);

        userImageImageView = headerview.findViewById(R.id.menu_loggedInUserImage);
        userImageImageView.setOnClickListener(openLoginOnClick);

        userNameTextView = headerview.findViewById(R.id.menu_loggedInUserFullName);
        userNameTextView.setOnClickListener(openLoginOnClick);
    }

    private void displayStopwatchFragment() {
        MenuItem workoutMenuItem = navigationView.getMenu().findItem(R.id.nav_workout);
        onNavigationItemSelected(workoutMenuItem);
        workoutMenuItem.setChecked(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            setUnknownUserData();
            updateSharedPreferencesForOfflineUser();
        }
    }

    private void setUnknownUserData() {
        try {
            setUnknownUser();
            Log.i(TAG, "Unknown user set");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setUnknownUser() throws SQLException {
        try {
            List<User> offlineUsers = userDao.queryForEq("accType", User.ACC_TYPE_OFFLINE);
            currentUser = offlineUsers.get(0);
            Log.i(TAG, "Unknown user exists");
        }
        catch (IndexOutOfBoundsException e) {
            currentUser = new User(0, "0");
            userDao.create(currentUser);
            Log.i(TAG, "Unknown user created");
        }
    }

    private void updateSharedPreferencesForOfflineUser() {
        SharedPreferences.Editor userShPrEditor = userShPr.edit();
        userShPrEditor.putBoolean(getString(R.string.usershpr_usersignedin), false);
        userShPrEditor.putLong(getString(R.string.usershpr_userid), currentUser.getId());
        userShPrEditor.apply();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNavigationViewUserValues();
        logSharedPreferences();
    }

    private void renderNavigationViewUserValues() {
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Glide.with(this).load(account.getPhotoUrl()).into(userImageImageView);
            userNameTextView.setText(account.getDisplayName());
        }
        else {
            userImageImageView.setImageResource(R.mipmap.ic_launcher_round);
            userNameTextView.setText(R.string.all_unknownuser);
        }
    }

    private void logSharedPreferences() {
        Log.i(TAG, getString(R.string.usershpr_usersignedin) + " " + userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))));
        Log.i(TAG, getString(R.string.usershpr_userid) + " " + userShPr.getLong(getString(R.string.usershpr_userid), Integer.valueOf(getString(R.string.usershpr_userid_default))));
        Log.i(TAG, getString(R.string.appshpr_gps) + " " + appShPr.getBoolean(getString(R.string.appshpr_gps), Boolean.valueOf(getString(R.string.appshpr_gps_default))));
        Log.i(TAG, getString(R.string.appshpr_unit) + " " + appShPr.getInt(getString(R.string.appshpr_unit), Integer.valueOf(getString(R.string.appshpr_unit_default))));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            navigationView.getCheckedItem().setChecked(false);
            navigationView.getMenu().findItem(R.id.nav_workout).setChecked(true);
            if (!getTitle().equals(R.string.app_name))
                setTitle(R.string.app_name);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        performCorrespondingActionForMenuItem(item.getItemId());
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        performCorrespondingActionForMenuItem(item.getItemId());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void performCorrespondingActionForMenuItem(int itemId) {
        switch(itemId) {
            case R.id.nav_workout:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_replaceable, StopwatchFragment.newInstance())
                        .commit();
                break;
            case R.id.nav_history:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_replaceable, HistoryFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.nav_about:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_replaceable, AboutFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_sync_with_server:
                // TODO sync with server
                Toast.makeText(this, "Sync is not available", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Sync not implemetned");
                break;
            case R.id.action_clear_history:
                // TODO clear history
                Toast.makeText(this, "Clearing history is not available", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "History cleaning not implemetned");
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof StopwatchFragment) {
            StopwatchFragment stopwatchFragment = (StopwatchFragment) fragment;
            stopwatchFragment.setmListener(this);
        }
    }

    @Override
    public void onWorkoutStopped() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_replaceable, StopwatchFragment.newInstance())
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }
}
