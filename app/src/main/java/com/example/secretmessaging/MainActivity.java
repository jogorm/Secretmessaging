package com.example.secretmessaging;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import com.google.api.services.gmail.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import static com.example.secretmessaging.R.id.googleStatus;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "SpHeceJw3jmFVskY50nnUfBC4";
    private static final String TWITTER_SECRET = "prOOd00ugc6t1Uegw77zAZKZY1CHGuzwuPPYt0wRH9srL8YfGj";


    private static Twitter twitter;
    private static SharedPreferences mSharedPreferences;
    private static RequestToken requestToken;

    private static SharedPreferences settings;

    private Button btnLogin;
    private Button logOut;
    private Button actButton;


    //Google Stuff
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private TextView gmailStatus;
    private TextView twitterStatus;
    private SignInButton mCallApiButton;
    ProgressDialog mProgress;
    private Button logoutGmail;


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Connect to Gmail";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};
    private com.google.api.services.gmail.Gmail mService = null;

    //Twitter stuff
    private String twitter_consumer_key;
    private String twitter_consumer_secret;
    private String twitter_callback;
    private String url_twitter_auth;
    private String twitter_oauth_verifier;


    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";

    static final String PREF_KEY_GMAIL_LOGIN = "isGmailLoggedIn";

    private static Context context;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        twitter_consumer_key = getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = getResources().getString(R.string.twitter_callback);
        url_twitter_auth = getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = getResources().getString(R.string.twitter_oauth_verifier);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        MainActivity.context = getApplicationContext();

        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);
        String accountName = mSharedPreferences.getString(PREF_ACCOUNT_NAME, null);

        gmailStatus = (TextView) findViewById(googleStatus);
        twitterStatus = (TextView) findViewById(R.id.twitterStatus);


        if (mSharedPreferences.getBoolean(PREF_KEY_GMAIL_LOGIN, false)) {

            gmailStatus.setText("Gmail logged in");

        }

        if (mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false)) {
            twitterStatus.setText("Twitter logged in");

        }


        btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //loginToTwitter();
                new loginTwitter(MainActivity.getAppContext()).execute();

            }
        });

        logOut = (Button) findViewById(R.id.logOutFromTwitter);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutFromTwitter();
                Toast.makeText(MainActivity.this, "You have logged out of Twitter", Toast.LENGTH_LONG).show();
            }
        });

        //// TODO: 05.07.2016 Should prioritise to find out how to log out from gmail as well.

        actButton = (Button) findViewById(R.id.newActButton);
        actButton.setText("Send messages");
        actButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                startActivity(intent);
            }
        });

        logoutGmail = (Button) findViewById(R.id.logoutGmail);
        logoutGmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "This function has not been implemented yet.", Toast.LENGTH_LONG).show();
            }
        });


        mCallApiButton = (SignInButton) findViewById(R.id.button);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });


        mOutputText = (TextView) findViewById(R.id.textView);
        mOutputText.setText("Click the \'" + BUTTON_TEXT + "\' button to test the API.");
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Gmail API ...");


        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());


        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(twitter_callback)) {
                // oAuth verifier
                String verifier = uri.getQueryParameter(twitter_oauth_verifier);

                try {
                    // Get the access token
                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

                    // Shared Preferences
                    SharedPreferences.Editor e = mSharedPreferences.edit();

                    // After getting access token, access token secret
                    // store them in application preferences
                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());

                    // Store login status - true
                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    e.commit(); // save changes


                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    String username = user.getName();

                    Toast.makeText(MainActivity.this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                    twitterStatus.setText("Twitter logged in");
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mSharedPreferences.getBoolean(PREF_KEY_GMAIL_LOGIN, true)) {
            gmailStatus.setText("Gmail logged in");
            mOutputText.setText("Logged in to Gmail");
            Log.i("hallo", "gmail logged in");
        }
        else{
            gmailStatus.setText("");
            Log.i("hallo", "gmail not logged in");
        }

        if (mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false)) {
            twitterStatus.setText("Twitter logged in");
            Log.i("hallo", "twitter logged in");
        }
        else{
            twitterStatus.setText("");
            Log.i("hallo", "twitter not logged in");
        }

    }

    public static Context getAppContext() {
        return MainActivity.context;
    }


    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }


    //// TODO: 05.07.2016 move this method to twitterconnector. Find out where it is used, change logic there to point to twitterconnector.
    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
        twitterStatus.setText("");

    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();

        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {

            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);

            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " + "Google Play Services on your device and relaunch this app.");
                } else {
                    settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(PREF_KEY_GMAIL_LOGIN, true);
                    editor.commit();
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.putBoolean(PREF_KEY_GMAIL_LOGIN, true);
                        editor.commit();

                        SharedPreferences.Editor edi = mSharedPreferences.edit();
                        edi.putString(PREF_ACCOUNT_NAME, accountName);
                        edi.commit();

                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor edi = settings.edit();
                    edi.putBoolean(PREF_KEY_GMAIL_LOGIN, true);
                    edi.commit();
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }


    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(MainActivity.this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();

    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<ArrayList<String>, String, List<Message>> {
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(transport, jsonFactory, credential).setApplicationName("Gmail API Android Quickstart").build();
        }


        @Override
        protected List<Message> doInBackground(ArrayList<String>... params) {
            return null;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Message> output) {
            mProgress.hide();
            mOutputText.setText("Logged in to Gmail");
            gmailStatus.setText("Gmail logged in");
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(), MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    private class loginTwitter extends AsyncTask<String, String, Void> {
        Context mContext;
        String username;

        public loginTwitter(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            if (!isTwitterLoggedInAlready()) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(twitter_consumer_key);
                builder.setOAuthConsumerSecret(twitter_consumer_secret);
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);
                twitter = factory.getInstance();

                try {
                    requestToken = twitter.getOAuthRequestToken(twitter_callback);

                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            } else {
                // user already logged into twitter

                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(twitter_consumer_key);
                builder.setOAuthConsumerSecret(twitter_consumer_secret);
                String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);
                String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
                AccessToken accessToken = new AccessToken(token, tokenSecret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);


                long userID = accessToken.getUserId();
                User user = null;
                try {
                    user = twitter.showUser(userID);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                username = user.getScreenName();


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            super.onPostExecute(aVoid);
            if (!isTwitterLoggedInAlready()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


                mContext.startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "Already Logged into twitter: " + username, Toast.LENGTH_LONG).show();
            }


        }
    }

}
