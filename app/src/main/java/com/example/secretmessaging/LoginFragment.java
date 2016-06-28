package com.example.secretmessaging;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {

    private static Twitter twitter;
    private static SharedPreferences mSharedPreferences;
    private static RequestToken requestToken;
    private Button btnLogin;
    private Button logOut;


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

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mSharedPreferences = getActivity().getApplicationContext().getSharedPreferences("MyPref", 0);

        twitter_consumer_key = getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = getResources().getString(R.string.twitter_callback);
        url_twitter_auth = getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = getResources().getString(R.string.twitter_oauth_verifier);


        Button twitterLog = (Button)view.findViewById(R.id.btn_login);
        twitterLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("hallo", "clicked log into twitter");

                    new TwitterLogin().execute();
            }
        });

        Button TwitterOut = (Button)view.findViewById(R.id.logOutFromTwitter);
        TwitterOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutFromTwitter();
            }
        });
        if (!isTwitterLoggedInAlready()) {
            new TwitterGetCallback().execute();
        }

        return view;
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

    private class TwitterLogin extends AsyncTask<String, String, Void> {

        String username;
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
                    requestToken = twitter
                            .getOAuthRequestToken(twitter_callback);
                    getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            } else {
                // user already logged into twitter

                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(twitter_consumer_key);
                builder.setOAuthConsumerSecret(twitter_consumer_secret);

                SharedPreferences shared = getActivity().getPreferences(Context.MODE_PRIVATE);

                String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);
                String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
                AccessToken accessToken = new AccessToken(token, tokenSecret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                Log.i("hallo token", token);
                Log.i("hallo tokenSecret", tokenSecret);

                long userID = accessToken.getUserId();
                User user = null;
                try {
                    user = twitter.showUser(userID);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                username = user.getScreenName();


                //Toast.makeText(getActivity(), "Already Logged into twitter: " + username, Toast.LENGTH_LONG).show();

                //// TODO: 28.06.2016 Need to continue working on the async stuff. Currently I seem to have to log in two times or something. Make a text thing on the screen to make it easier to see if I am logged in or not.


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getActivity(), "Already Logged into twitter: " + username, Toast.LENGTH_LONG).show();

        }
    }

    private class TwitterGetCallback extends AsyncTask<String, String, Void>{

        @Override
        protected Void doInBackground(String... params) {
            Uri uri = getActivity().getIntent().getData();
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

                    //// TODO: 25.06.2016 Need to move all network stuff to async thread.
                    long testjo = 745178402408169472L;
                    //twitter.directMessages().sendDirectMessage(testjo,"Heisann!");
                    Log.i("hallo", "Sent message");
                    String username = user.getName();

                    //Toast.makeText(LoginFragment.this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                    Log.i("hallo" , "Welcome " + username);
                    // Displaying in xml ui
                    //lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
            return null;
        }
    }





}
