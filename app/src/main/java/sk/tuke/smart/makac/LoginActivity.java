package sk.tuke.smart.makac;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.tuke.smart.makac.model.User;
import sk.tuke.smart.makac.model.UserProfile;
import sk.tuke.smart.makac.model.config.DatabaseHelper;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class LoginActivity extends AppCompatActivity implements DatabaseConnection {
    @BindView(R.id.textview_username) public TextView usernameTextView;
    @BindView(R.id.textview_username_unknown) public TextView usernameUnknownTextView;
    @BindView(R.id.sign_in_button) public SignInButton signInButton;
    @BindView(R.id.sign_out_button) public SignInButton signOutButton;
    @BindView(R.id.button_save_changes) public Button saveChangesButton;
    @BindView(R.id.height_input) public EditText heightInput;
    @BindView(R.id.age_input) public EditText ageInput;
    @BindView(R.id.weight_input) public EditText weightInput;

    private static final String TAG = "LoginActivity";

    private static final int SIGN_IN_CODE = 777;

    private SharedPreferences userShPr;

    private GoogleSignInAccount account;

    private GoogleSignInClient mGoogleSignInClient;

    private LoginActivity thisActivity;

    private UserProfile currentUserProfile;

    private User currentUser;

    private Dao<User, Long> userDao;
    private Dao<UserProfile, Long> userProfileDao;

    private boolean userSignedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userShPr = getSharedPreferences(getString(R.string.usershpr), Context.MODE_PRIVATE);
        thisActivity = this;
        initializeLayout();
        configureGoogleSignIn();
        databaseSetup();
    }

    private void initializeLayout() {
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        setTitle(R.string.user_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        configureOnClickListeners();
        configureSignOutButton();
        configureOnFocusChangeListeners();
    }

    private void configureOnClickListeners() {
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGN_IN_CODE);
            }
        });

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        account = GoogleSignIn.getLastSignedInAccount(thisActivity);
                        performUserDependingActions();
                    }
                });
            }
        });
    }

    private void configureSignOutButton() {
        for (int i = 0; i < signOutButton.getChildCount(); i++) {
            View v = signOutButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(R.string.action_sign_out);
                return;
            }
        }
    }

    private void configureOnFocusChangeListeners() {
        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    if (saveChangesButton.getVisibility() != View.VISIBLE && contentsHasChanged(view))
                        saveChangesButton.setVisibility(View.VISIBLE);
                }
            }
        };
        weightInput.setOnFocusChangeListener(onFocusChangeListener);
        ageInput.setOnFocusChangeListener(onFocusChangeListener);
        heightInput.setOnFocusChangeListener(onFocusChangeListener);
    }

    private boolean contentsHasChanged(View view) {
        if (!(view instanceof EditText))
            throw new UnsupportedOperationException();
        EditText input = (EditText) view;
        switch ((String) input.getTag()) {
            case "weight_input":
                if (inputValueDiffersFromUserValue(weightInput.getText(), currentUserProfile.getWeight())) {
                    Log.i(TAG, "Weight is different");
                    return true;
                }
                Log.i(TAG, "Weight is not different");
                break;
            case "age_input":
                if (inputValueDiffersFromUserValue(ageInput.getText(), currentUserProfile.getAge())) {
                    Log.i(TAG, "Age is different");
                    return true;
                }
                Log.i(TAG, "Age is not different");
                break;
            case "height_input":
                if (inputValueDiffersFromUserValue(heightInput.getText(), currentUserProfile.getHeight())) {
                    Log.i(TAG, "Height is different");
                    return true;
                }
                Log.i(TAG, "Height is not different");
                break;
        }
        return false;
    }

    private boolean inputValueDiffersFromUserValue(Editable inputValue, Number userValue) {
        return !inputValue.toString().equals(String.valueOf(userValue));
    }

    private void configureGoogleSignIn() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    public void databaseSetup() {
        try {
            DatabaseHelper databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            userDao = databaseHelper.userDao();
            userProfileDao = databaseHelper.userProfileDao();
            Log.i(TAG, "Local database is ready");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == SIGN_IN_CODE) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            performUserDependingActions();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            performUserDependingActions();
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

    private void performCorrespondingActionForMenuItem(int itemId) {
        switch(itemId) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
         performUserDependingActions();
    }

    private void performUserDependingActions() {
        checkUser();
        updateSharedPreferences();
        updateUI();
    }

    private void checkUser() {
        account = GoogleSignIn.getLastSignedInAccount(this);
        try {
            if (account == null)
                setUnknownUserData();
            else
                setGoogleUserData();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setUnknownUserData() throws SQLException {
        setUnknownUser();
        setUnknownUserProfile();
        userSignedIn = false;
        Log.i(TAG, "Unknown user set");
    }

    private void setUnknownUser() throws SQLException {
        List<User> offlineUsers = userDao.queryForEq(User.COLUMN_ACCTYPE, User.ACCTYPE_OFFLINE);
        currentUser = offlineUsers.get(0);
        Log.i(TAG, "Unknown user exists");
    }

    private void setUnknownUserProfile() throws SQLException {
        try {
            List<UserProfile> offlineUserProfiles = userProfileDao.queryForEq(UserProfile.COLUMN_USERID, currentUser.getId());
            currentUserProfile = offlineUserProfiles.get(0);
            Log.i(TAG, "Unknown user profile exists");
        }
        catch (IndexOutOfBoundsException e) {
            currentUserProfile = new UserProfile();
            currentUserProfile.setUser(currentUser);
            userProfileDao.create(currentUserProfile);
            Log.i(TAG, "Unknown user profile created");
        }
    }

    private void setGoogleUserData() throws SQLException {
        setGoogleUser();
        setGoogleUserProfile();
        userSignedIn = true;
        Log.i(TAG, "Google user set");
    }

    private void setGoogleUser() throws SQLException {
        try {
            List<User> users = userDao.queryForEq(User.COLUMN_ACCID, String.valueOf(account.getId()));
            currentUser = users.get(0);
            Log.i(TAG, "Google user exists");
        }
        catch (IndexOutOfBoundsException e) {
            currentUser = new User(User.ACCTYPE_GOOGLE, String.valueOf(account.getId()));
            userDao.create(currentUser);
            Log.i(TAG, "Google user created");
        }
    }

    private void setGoogleUserProfile() throws SQLException {
        try {
            List<UserProfile> userProfiles = userProfileDao.queryForEq(UserProfile.COLUMN_USERID, currentUser.getId());
            currentUserProfile = userProfiles.get(0);
            Log.i(TAG, "Google user profile exists");
        }
        catch (IndexOutOfBoundsException e) {
            currentUserProfile = new UserProfile();
            currentUserProfile.setUser(currentUser);
            userProfileDao.create(currentUserProfile);
            Log.i(TAG, "Google user profile created");
        }
    }

    private void updateSharedPreferences() {
        SharedPreferences.Editor shprEditor = userShPr.edit();
        shprEditor.putLong(getString(R.string.usershpr_userid), currentUser.getId());
        shprEditor.putBoolean(getString(R.string.usershpr_usersignedin), userSignedIn);
        shprEditor.apply();
        Log.i(TAG, "Shared preferences updated to values: " + userShPr.getLong(getString(R.string.usershpr_userid), Long.valueOf(getString(R.string.usershpr_userid_default))) + ", " + userShPr.getBoolean(getString(R.string.usershpr_usersignedin), Boolean.valueOf(getString(R.string.usershpr_usersignedin_default))));
    }

    private void updateUI() {
        if (account != null) {
            fillInputsWithValues(currentUserProfile.getHeight(), currentUserProfile.getAge(), currentUserProfile.getWeight());
            changeVisibility(View.GONE, View.VISIBLE, View.VISIBLE, View.GONE);
            usernameTextView.setText(account.getDisplayName());
            Toast.makeText(this, "You are signed in", Toast.LENGTH_SHORT).show();
        }
        else {
            fillInputsWithValues(currentUserProfile.getHeight(), currentUserProfile.getAge(), currentUserProfile.getWeight());
            changeVisibility(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE);
            Toast.makeText(this, "You are signed out", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeVisibility(int signInVisibility, int signOutVisibility, int usernameVisibility, int usernameUnknownVisiblity) {
        signInButton.setVisibility(signInVisibility);
        signOutButton.setVisibility(signOutVisibility);
        usernameTextView.setVisibility(usernameVisibility);
        usernameUnknownTextView.setVisibility(usernameUnknownVisiblity);
        Log.i(TAG, "Visibility of layout elements changed");
    }

    private void fillInputsWithValues(float height, int age, float weight) {
        heightInput.setText(String.valueOf(height));
        ageInput.setText(String.valueOf(age));
        weightInput.setText(String.valueOf(weight));
        Log.i(TAG, "Input values were filled");
    }

    @OnClick(R.id.button_save_changes)
    public void saveProfileChanges(View view) {
        try {
            int newAge = Integer.valueOf(ageInput.getText().toString());
            float newHeight = Float.valueOf(heightInput.getText().toString());
            float newWeight = Float.valueOf(weightInput.getText().toString());
            currentUserProfile.setAge(newAge);
            currentUserProfile.setHeight(newHeight);
            currentUserProfile.setWeight(newWeight);
            userProfileDao.update(currentUserProfile);
            Log.i(TAG, "User profile values updated to: " + newWeight + ", " + newAge + ", " + newHeight);
            saveChangesButton.setVisibility(View.GONE);
            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Age value is invalid", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }
}