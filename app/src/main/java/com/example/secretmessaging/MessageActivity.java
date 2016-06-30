package com.example.secretmessaging;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;



public class MessageActivity extends AppCompatActivity {

    String twitter_consumer_key;
    String twitter_consumer_secret;
    String twitter_callback;
    String url_twitter_auth;
    String twitter_oauth_verifier;
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";

    private static Twitter twitter;
    private static SharedPreferences mSharedPreferences;
    private static RequestToken requestToken;

    private Button actButton;
    private Button sendButtonTwitter;
    private Button checkButton;
    private Button sendButtonGmail;
    ProgressDialog mProgress;
    EditText messageEdit;
    EditText emailEdit;
    ArrayList<String> sendValues = new ArrayList<String>();


    private com.google.api.services.gmail.Gmail mService = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        twitter_consumer_key = getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = getResources().getString(R.string.twitter_callback);
        url_twitter_auth = getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = getResources().getString(R.string.twitter_oauth_verifier);

        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);



        actButton = (Button)findViewById(R.id.newActButton);
        actButton.setText("Go to login");
        actButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessageActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });



        messageEdit = (EditText)findViewById(R.id.messageText);
        emailEdit = (EditText)findViewById(R.id.emailText);

        sendButtonTwitter = (Button)findViewById(R.id.sendButton);
        sendButtonTwitter.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String email = emailEdit.getText().toString();
                        String mess = messageEdit.getText().toString();

                        Log.i("hallo", mess);
                        Log.i("hallo", email);
//                        sendValues.clear();
//                        sendValues.add(messageEdit.getText().toString());
//                        sendValues.add(emailEdit.getText().toString());

                        //new SendMessage().execute(sendValues);

                        ConfigurationBuilder builder = new ConfigurationBuilder();
                        builder.setOAuthConsumerKey(twitter_consumer_key);
                        builder.setOAuthConsumerSecret(twitter_consumer_secret);

                        SharedPreferences shared = getPreferences(MODE_PRIVATE);

                        String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);
                        String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
                        AccessToken accessToken = new AccessToken(token, tokenSecret);
                        Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                        User user;
                        long userId = 0;
                        try {
                            user = twitter.users().showUser(email);
                            Log.i("Hallo", email + "'s user id is: " + user.getId());
                            userId = user.getId();
                        } catch (TwitterException e) {
                            e.printStackTrace();
                            Toast.makeText(MessageActivity.this, "Is that a real user? Try again.", Toast.LENGTH_SHORT).show();
                        }
                        try {
                            twitter.directMessages().sendDirectMessage(userId,"Heisann!");
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                    }
                });

        sendButtonGmail = (Button)findViewById(R.id.sendGmail);
        sendButtonGmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gson gson = new Gson();
                String json = mSharedPreferences.getString("GoogleService", " ");
                mService = gson.fromJson(json, mService.getClass());
//                new SendMessage().execute();
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
                //new checkForMessage().execute("Burr");

            }
        });
    }
    private class SendMessage extends AsyncTask<ArrayList<String>, String, Void> {

        public void sendMessage(Gmail service, String userId, MimeMessage email)
                throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            message = service.users().messages().send(userId, message).execute();

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
                Log.i("hallo", "Sent message: " + message + " to email: " + email);
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
}
