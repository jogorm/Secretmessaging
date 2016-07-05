package com.example.secretmessaging;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by joakimgormrandulff on 05.07.2016.
 */


public class TwitterConnector extends Activity {

    String twitter_consumer_key;
    String twitter_consumer_secret;
    String twitter_callback;
    String url_twitter_auth;
    String twitter_oauth_verifier;
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";

    private static SharedPreferences mSharedPreferences;


    Context context;
    public TwitterConnector(Context c){
        context = c;
    }

    public Twitter getTwitter(){

        twitter_consumer_key = context.getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = context.getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = context.getResources().getString(R.string.twitter_callback);
        url_twitter_auth = context.getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = context.getResources().getString(R.string.twitter_oauth_verifier);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(twitter_consumer_key);
        builder.setOAuthConsumerSecret(twitter_consumer_secret);

        //SharedPreferences shared = getPreferences(MODE_PRIVATE);
        mSharedPreferences = context.getSharedPreferences("MyPref", 0);

        String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);
        String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
        AccessToken accessToken = new AccessToken(token, tokenSecret);
        Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

        return twitter;
    }



}
