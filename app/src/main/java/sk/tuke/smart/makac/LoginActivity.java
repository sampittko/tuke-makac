package sk.tuke.smart.makac;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import sk.tuke.smart.makac.settings.SettingsActivity;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.textview_username) public TextView usernameTextView;
    @BindView(R.id.sign_in_button) public SignInButton signInButton;
    @BindView(R.id.sign_out_button) public SignInButton signOutButton;
    @BindView(R.id.button_save_changes) public Button saveChangesButton;

    private static final String TAG = "LoginActivity";

    private static final int SIGN_IN_CODE = 777;

    private GoogleSignInAccount account;

    private GoogleSignInClient mGoogleSignInClient;

    private LoginActivity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLayout();
        thisActivity = this;
        configureGoogleSignIn();
    }

    private void initializeLayout() {
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        setTitle(R.string.user_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        configureOnClickListeners();
        configureSignOutButton();
        fillInputsWithValues();
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
                        updateUI();
                    }
                });
            }
        });

        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfileChanges();
            }
        });
    }

    // TODO
    private void fillInputsWithValues() {

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

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI();
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
            case 16908332:
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
         account = GoogleSignIn.getLastSignedInAccount(this);
         updateUI();
    }

    private void updateUI() {
        if (account != null) {
            usernameTextView.setText(account.getDisplayName());
            usernameTextView.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "You are signed in", Toast.LENGTH_SHORT).show();
        }
        else {
            usernameTextView.setVisibility(View.INVISIBLE);
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            Toast.makeText(this, "You are signed out", Toast.LENGTH_SHORT).show();
        }
    }

    // TODO
    private void saveProfileChanges() {

    }
}