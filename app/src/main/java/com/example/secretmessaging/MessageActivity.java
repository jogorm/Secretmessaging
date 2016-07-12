package com.example.secretmessaging;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import org.mitre.secretsharing.Part;
import org.mitre.secretsharing.Secrets;
import org.mitre.secretsharing.codec.PartFormats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.RequestToken;


public class MessageActivity extends AppCompatActivity {

    String twitter_consumer_key;
    String twitter_consumer_secret;
    String twitter_callback;
    String url_twitter_auth;
    String twitter_oauth_verifier;
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";



    private static Twitter twitter;
    private static SharedPreferences mSharedPreferences;
    private static RequestToken requestToken;

    private Button actButton;
    private Button sendButton;
    private Button checkButton;
    ProgressDialog mProgress;
    EditText messageEdit;
    EditText emailEdit;
    EditText twitterEdit;
    Handler handler;
    String identifier = "jgr2016 ";
    private TextView messageReply;

    ArrayList<String> sendValuesGmail = new ArrayList<String>();
    ArrayList<String> sendValuesTwitter = new ArrayList<String>();

    private com.google.api.services.gmail.Gmail mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Sending message");

        final GmailConnector gCon = new GmailConnector(this);
        mService = gCon.getService();

        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);
        if(mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN,false)){

        TwitterConnector tCon = new TwitterConnector(this);
        twitter = tCon.getTwitter();
        }
        else{
            Toast.makeText(MessageActivity.this, "You are not logged into twitter. The application will not work.", Toast.LENGTH_SHORT).show();
        }

        twitter_consumer_key = getResources().getString(R.string.twitter_consumer_key);
        twitter_consumer_secret = getResources().getString(R.string.twitter_consumer_secret);
        twitter_callback = getResources().getString(R.string.twitter_callback);
        url_twitter_auth = getResources().getString(R.string.url_twitter_auth);
        twitter_oauth_verifier = getResources().getString(R.string.twitter_oauth_verifier);
        messageReply = (TextView)findViewById(R.id.messageReply);



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
        twitterEdit = (EditText)findViewById(R.id.twitterText);

        checkButton =(Button)findViewById(R.id.checkForEmail);
        checkButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                messageReply.setText("");
                new checkForMessage().execute(identifier);


            }
        });

        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String email = emailEdit.getText().toString();
                        String mess = messageEdit.getText().toString();
                        String twitt = twitterEdit.getText().toString();

                        //removing spaces from twitter username
                        String fixedTwitt = twitt.replace(" ", "");

                        //Secret sharing elements
                        byte [] secret = mess.getBytes();
                        Random rndm = new Random();
                        Part[] parts = Secrets.split(secret, 2, 2, rndm);
                        String gmailMess = String.valueOf(parts[0]);
                        String twitterMess = String.valueOf(parts[1]);


                        //Building the string by adding the identifier to start of string.
                        StringBuilder sbTwitter = new StringBuilder();
                        sbTwitter.append(identifier);
                        sbTwitter.append(twitterMess);
                        twitterMess = sbTwitter.toString();

                        StringBuilder sbGmail = new StringBuilder();
                        sbGmail.append(identifier );
                        sbGmail.append(gmailMess);
                        gmailMess = sbGmail.toString();

                        sendValuesGmail.clear();
                        sendValuesGmail.add(gmailMess);
                        sendValuesGmail.add(emailEdit.getText().toString());

                        sendValuesTwitter.clear();
                        sendValuesTwitter.add(twitterMess);
                        sendValuesTwitter.add(fixedTwitt);


                        new SendGmail().execute(sendValuesGmail);
                        new SendTwitter().execute(sendValuesTwitter);


                    }
                });


    }
    private class SendGmail extends AsyncTask<ArrayList<String>, String, Void> {

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
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            String message = params[0].get(0);
            String email = params[0].get(1);

            try {
                sendMessage(mService, "me", createEmail(email, "me", "test", message));
                Log.i("hallo", "Sent message: " + message + " to email: " + email);
            } catch (MessagingException | IOException e) {
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

    private class SendTwitter extends AsyncTask<ArrayList<String>, String, String> {

        @Override
        protected String doInBackground(ArrayList<String>... params) {
            String mess = params[0].get(0);
            String twitt = params[0].get(1);

            User user;
            long userId = 0;
            try {
                user = twitter.users().showUser(twitt);
                userId = user.getId();
            } catch (TwitterException e) {
                e.printStackTrace();
                //return e.getErrorCode();
                return e.getErrorMessage();
            }
            try {
                twitter.directMessages().sendDirectMessage(userId,mess);
            } catch (TwitterException e) {
                e.printStackTrace();
                //return e.getErrorCode();
                return e.getErrorMessage();
            }

            return "Twitter message sent";
        }


        @Override
        protected void onPostExecute(String code) {
            super.onPostExecute(code);
            Log.e("hallo", code.toString());

            Toast.makeText(MessageActivity.this, code, Toast.LENGTH_SHORT).show();
        }
    }

    private class checkForMessage extends AsyncTask<String, String, HashMap> {

        @Override
        protected HashMap doInBackground(String... params) {
            String gmailMessage = "empty";
            String query = params[0];
            try {

                gmailMessage = listMessagesMatchingQuery(mService, "me", query);
//                listMessagesMatchingQuery(mService, "me", query);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String twitterMessage="";
            String fixedTwitterMessage = "empty";
            List<DirectMessage> messages = null;
            try {
                messages = twitter.getDirectMessages();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            for (DirectMessage message : messages){
                if(message.getText().contains(query)) {
                    Log.i("hallo messages twitter", message.getText().toString());
                    twitterMessage = message.getText().toString();
                    break;
                }
            }

            fixedTwitterMessage = twitterMessage.replace(identifier, "");

            HashMap message = new HashMap();
            message.put("gmail", gmailMessage);
            message.put("twitter", fixedTwitterMessage);
            /*ArrayList message = new ArrayList();
            message.add(gmailMessage);
            message.add(fixedTwitterMessage);*/
            return message;
        }

        public String listMessagesMatchingQuery(Gmail service, String userId, String query) throws IOException {
            ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

            String gmailMessage ="";
            String fixedGmailMessage = "empty";
            List<Message> messages = new ArrayList<Message>();
            while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId).setQ(query).setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            for (Message message : messages) {
                String theMessage = getMessage(mService, "me", message.getId());
                if (theMessage.contains(identifier)) {
                    Log.i("Hallo", "Email snippet found ");
                    gmailMessage = theMessage;
                    break;
                }
            }

            fixedGmailMessage = gmailMessage.replace(identifier, "");

            return fixedGmailMessage;
        }

        public String getMessage(Gmail service, String userId, String messageId)
                throws IOException {
            Message message = service.users().messages().get(userId, messageId).execute();

            return message.getSnippet();
        }

        @Override
        protected void onPostExecute(HashMap s) {
            super.onPostExecute(s);

            byte[] result;
            String twitter = String.valueOf(s.get("twitter"));
            String gmail = String.valueOf(s.get("gmail"));

            Log.i("Fixed gmail message: " , gmail);
            Log.i("Fixed twitter message: ", twitter);


            List<Part> parts = new ArrayList<Part>();
            parts.add(PartFormats.parse(gmail));
            parts.add(PartFormats.parse(twitter));

            Part[] p = parts.toArray(new Part[0]);
            result = p[0].join(Arrays.copyOfRange(p, 1, p.length));

            String stringResult = new String(result);
            Log.i("RESULTAT", stringResult);

            System.out.println(result);
            Log.i("hallo", "inne i try catch");

            messageReply.setText(stringResult);

            // TODO: 12.07.2016 Make a progress bar or something while this is happening

        }
    }



}
