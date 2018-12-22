package sk.tuke.smart.makac;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.stetho.Stetho;

import sk.tuke.smart.makac.fragments.AboutFragment;
import sk.tuke.smart.makac.fragments.HistoryFragment;
import sk.tuke.smart.makac.fragments.StopwatchFragment;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        AboutFragment.OnFragmentInteractionListener,
        HistoryFragment.OnListFragmentInteractionListener,
        StopwatchFragment.OnFragmentInteractionListener {
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // live DB
        Stetho.initializeWithDefaults(this);

        initializeMainActivityLayout();
    }

    private void initializeMainActivityLayout() {
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

        displayStopwatchFragment();
    }

    private void displayStopwatchFragment() {
        MenuItem workoutMenuItem = navigationView.getMenu().findItem(R.id.nav_workout);
        onNavigationItemSelected(workoutMenuItem);
        workoutMenuItem.setChecked(true);
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
            default:
                throw new UnsupportedOperationException();
        }
    }
}
