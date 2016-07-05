package com.example.secretmessaging;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

/**
 * Created by joakimgormrandulff on 04.07.2016.
 */

public class GmailConnector extends Activity{
    private static final String[] SCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM};
    private com.google.api.services.gmail.Gmail mService = null;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    private static SharedPreferences mSharedPreferences;



    GoogleAccountCredential mCredential;
    Context context;



    public GmailConnector(Context c){
        context = c;
    }



    public com.google.api.services.gmail.Gmail getService(){

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        mService = new com.google.api.services.gmail.Gmail.Builder(transport, jsonFactory, getCredentials()).setApplicationName("Gmail API Android Quickstart").build();
        return mService;
    }

    public GoogleAccountCredential getCredentials(){

        mSharedPreferences = context.getSharedPreferences("MyPref", 0);

        mCredential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());

        String accountName = mSharedPreferences.getString(PREF_ACCOUNT_NAME, null);


        mCredential.setSelectedAccountName(accountName);
        return mCredential;

    }

}
