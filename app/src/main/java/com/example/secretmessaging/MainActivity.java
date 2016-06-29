package com.example.secretmessaging;


        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.GoogleApiAvailability;
        import com.google.api.client.extensions.android.http.AndroidHttp;
        import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
        import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
        import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.json.JsonFactory;
        import com.google.api.client.json.jackson2.JacksonFactory;
        import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
        import com.google.api.client.util.ExponentialBackOff;

        import com.google.api.services.gmail.Gmail;
        import com.google.api.services.gmail.GmailScopes;

        import com.google.api.services.gmail.model.*;

        import android.Manifest;
        import android.accounts.AccountManager;
        import android.app.Activity;
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
        import android.preference.PreferenceManager;
        import android.service.textservice.SpellCheckerService;
        import android.support.annotation.NonNull;
        import android.text.Html;
        import android.text.method.ScrollingMovementMethod;
        import android.util.Log;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.LinearLayout;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.io.ByteArrayOutputStream;
        import java.io.IOException;
        import java.lang.reflect.Array;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.List;
        import java.util.Properties;


        import javax.mail.MessagingException;
        import javax.mail.Session;
        import javax.mail.internet.InternetAddress;
        import javax.mail.internet.MimeMessage;



        import pub.devrel.easypermissions.AfterPermissionGranted;
        import pub.devrel.easypermissions.EasyPermissions;
        import twitter4j.DirectMessage;
        import twitter4j.Twitter;
        import twitter4j.TwitterException;
        import twitter4j.TwitterFactory;
        import twitter4j.User;
        import twitter4j.auth.AccessToken;
        import twitter4j.auth.RequestToken;
        import twitter4j.auth.RequestToken;
        import twitter4j.conf.Configuration;
        import twitter4j.conf.ConfigurationBuilder;



public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {

    //Twitter Stuff

    private static Twitter twitter;
    private static SharedPreferences mSharedPreferences;
    private static RequestToken requestToken;
    private Button btnLogin;
    private Button logOut;
    private Button doTwitterStuff;


    //Google Stuff
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    private Button sendButton;
    private Button checkButton;
    ProgressDialog mProgress;
    EditText messageEdit;
    EditText emailEdit;
    ArrayList<String> sendValues = new ArrayList<String>();


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Connect to Gmail";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[ ] SCOPES = { GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE,
        GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM };
    private com.google.api.services.gmail.Gmail mService = null;


    String twitter_consumer_key;
    String twitter_consumer_secret;
    String twitter_callback;
    String url_twitter_auth;
    String twitter_oauth_verifier;
    String url_twitter_oauth_token;

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";


    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //// TODO: 27.06.2016 consider to make fragments in the view. Button on the top, like tabs in a web browser.
        //// TODO: 27.06.2016 Tab 1 is Message tab2 is login tab 3 is settings or whatever.

        twitter_consumer_key = getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = getResources().getString(R.string.twitter_callback);
        url_twitter_auth = getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = getResources().getString(R.string.twitter_oauth_verifier);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);



        btnLogin = (Button)findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                try {
                    loginToTwitter();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        });

        logOut = (Button)findViewById(R.id.logOutFromTwitter);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                logoutFromTwitter();
                Log.i("hallo", "testing branches");
            }
        });

        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(twitter_callback)) {
                // oAuth verifier
                String verifier = uri
                        .getQueryParameter(twitter_oauth_verifier);

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

                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    User user2 = twitter.showUser("testjotestra");
                    long abc = user2.getId();
                    Log.i("hallo", "testjotestra's id is : "+String.valueOf(abc));

                    List<DirectMessage> messages = twitter.getDirectMessages();
                    for (DirectMessage message : messages)
                    {
                        if(message.getText().contains("Hei")) {
                            Log.i("hallo messages", message.toString());
                        }
                    }
                    twitter.directMessages().getDirectMessages();

                    long testjo = 745178402408169472L;
                    //twitter.directMessages().sendDirectMessage(testjo,"Heisann!");
                    Log.i("hallo", "Sent message");
                    String username = user.getName();

                    Toast.makeText(MainActivity.this, "Welcome " + username, Toast.LENGTH_SHORT).show();

                    // Displaying in xml ui
                    //lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }

        mCallApiButton = (Button)findViewById(R.id.button);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });

        mOutputText =  (TextView)findViewById(R.id.textView);
        mOutputText.setText("Click the \'" + BUTTON_TEXT + "\' button to test the API.");
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Gmail API ...");

        messageEdit = (EditText)findViewById(R.id.messageText);
        emailEdit = (EditText)findViewById(R.id.emailText);

        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Log.i("hallo", messageEdit.getText().toString());
                        Log.i("hallo", emailEdit.getText().toString());
                        sendValues.clear();
                        sendValues.add(messageEdit.getText().toString());
                        sendValues.add(emailEdit.getText().toString());

                        new SendMessage().execute(sendValues);
                    }
                });

        checkButton =(Button)findViewById(R.id.checkForEmail);
        checkButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                new checkForMessage().execute("Burr");
            }
        });


        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }




    private void loginToTwitter() throws TwitterException {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(twitter_consumer_key);
            builder.setOAuthConsumerSecret(twitter_consumer_secret);
            Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            try {
                requestToken = twitter.getOAuthRequestToken(twitter_callback);
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {
            // user already logged into twitter

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(twitter_consumer_key);
            builder.setOAuthConsumerSecret(twitter_consumer_secret);

            SharedPreferences shared = getPreferences(MODE_PRIVATE);

            String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);
            String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
            AccessToken accessToken = new AccessToken(token, tokenSecret);
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

            Log.i("hallo token", token);
            Log.i("hallo tokenSecret", tokenSecret);

            long userID = accessToken.getUserId();
            User user = twitter.showUser(userID);
            String username = user.getScreenName();


            Toast.makeText(getApplicationContext(), "Already Logged into twitter: " + username, Toast.LENGTH_LONG).show();

        }
    }

    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
    }



    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
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

            if (accountName != null) {mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */



   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Make sure that the loginButton hears the result from any
        // Activity that it triggered.
        loginButton.onActivityResult(requestCode, resultCode, data);
    }*/


    //Can't find out where "onActivityResult" is called from twitter method.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //loginButton.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }



    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
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
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<ArrayList<String>, String, List<Message>> {
        //private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        //protected List<String> doInBackground(Void... params) {
        protected List<Message> doInBackground(ArrayList<String>... params) {
            try {
                return listMessagesMatchingQuery(mService, "me", "Burr");
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of Gmail labels attached to the specified account.
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<String> labels = new ArrayList<String>();
            ListLabelsResponse listResponse =
                    mService.users().labels().list(user).execute();
            for (Label label : listResponse.getLabels()) {
                labels.add(label.getName());
            }

            return labels;
        }

        public Message getMessage(Gmail service, String userId, String messageId)
                throws IOException {
            Message message = service.users().messages().get(userId, messageId).execute();

            //System.out.println("Message snippet: " + message.getSnippet());
            Log.i("message", "Message snippet: " + message.getSnippet());

            return message;
        }

        public List<Message> listMessagesMatchingQuery(Gmail service, String userId,
                                                              String query) throws IOException {
            ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

            List<Message> messages = new ArrayList<Message>();
            while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId).setQ(query)
                            .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            for (Message message : messages) {
                System.out.println(message.toPrettyString());
            }

            return messages;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Message> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                //output.add(0, "Data retrieved using the Gmail API:");
                //mOutputText.setText(TextUtils.join("\n", output));
            }
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

    private class SendMessage extends AsyncTask<ArrayList<String>,String, Void>{

        public void sendMessage(Gmail service, String userId, MimeMessage email)
                throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            message = service.users().messages().send(userId, message).execute();

            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
        }

        public Message createMessageWithEmail(MimeMessage email)
                throws MessagingException, IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            email.writeTo(bytes);
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);
            return message;
        }

        public MimeMessage createEmail(String to, String from, String subject,
                                       String bodyText) throws MessagingException {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            InternetAddress tAddress = new InternetAddress(to);
            InternetAddress fAddress = new InternetAddress(from);

            email.setFrom(new InternetAddress(from));
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(to));
            email.setSubject(subject);
            email.setText(bodyText);
            return email;
        }


        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            String message = params[0].get(0);
            String email = params[0].get(1);

            try {
                sendMessage(mService, "me", createEmail(email, "me", "test", message));
                Log.i("hallo", "Sent message: " + message + " to email: " +email);
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Context context = getApplicationContext();
            CharSequence text = "Message sent!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

    }

    private class checkForMessage extends AsyncTask<String, String, Void>{

        @Override
        protected Void doInBackground(String... params) {
            try {
                listMessagesMatchingQuery(mService, "me", "burr");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<Message> listMessagesMatchingQuery(Gmail service, String userId,
                                                              String query) throws IOException {
            ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

            List<Message> messages = new ArrayList<Message>();
            while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId).setQ(query)
                            .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            for (Message message : messages) {
                System.out.println(message.toPrettyString());
                Log.i("hallo", message.toPrettyString());
                String theMessage = getMessage(mService, "me", message.getId());
                String keyword = "Ticket";
                if(theMessage.contains("Ticket")){
                    Log.i("hallo", "Found the email");
                    Log.i("Hallo", "Email snippet: " + theMessage);
                }
            }

            return messages;
        }

        /*public Message getMessage(Gmail service, String userId, String messageId)
                throws IOException {
            Message message = service.users().messages().get(userId, messageId).execute();

            System.out.println("Message snippet: " + message.getSnippet());

            return message;
        }*/
        public String getMessage(Gmail service, String userId, String messageId)
                throws IOException {
            Message message = service.users().messages().get(userId, messageId).execute();

            System.out.println("Message snippet: " + message.getSnippet());

            return message.getSnippet();
        }
    }



}


