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
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import org.mitre.secretsharing.Part;
import org.mitre.secretsharing.Secrets;
import org.mitre.secretsharing.codec.PartFormats;
import org.mitre.secretsharing.util.InputValidationException;

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

import static com.example.secretmessaging.MainActivity.REQUEST_AUTHORIZATION;


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
    ProgressDialog mProgressCheck;
    ProgressDialog mProgressSend;
    EditText messageEdit;
    EditText emailEdit;
    EditText twitterEdit;

    String identifier = "jgr2016 ";
    private TextView messageReply;


    private TextView twitterSenderText;
    private TextView gmailSenderText;
    private TextView dateSentText;

    String twitterSender;
    String gmailSender;
    String dateSent;

    ArrayList<String> sendValuesGmail = new ArrayList<String>();
    ArrayList<String> sendValuesTwitter = new ArrayList<String>();

    private com.google.api.services.gmail.Gmail mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mProgressCheck = new ProgressDialog(MessageActivity.this);
        mProgressCheck.setMessage("Checking for message");

        mProgressSend = new ProgressDialog(MessageActivity.this);
        mProgressSend.setMessage("Sending message");

        twitterSenderText = (TextView)findViewById(R.id.twitterAddress);
        gmailSenderText = (TextView)findViewById(R.id.gmailAddress);
        dateSentText = (TextView)findViewById(R.id.dateView);


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
        if (actButton != null) {
            actButton.setText("Login");
        }
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

                        if(email.isEmpty() && twitt.isEmpty() && mess.isEmpty()){
                            Toast.makeText(MessageActivity.this, "You have to fill out the above fields before sending message", Toast.LENGTH_SHORT).show();
                        }
                        else if(twitt.isEmpty()){
                            Toast.makeText(MessageActivity.this, "You have to add a valid Twitter username for a user you are following, without the @", Toast.LENGTH_LONG).show();
                        }
                        else if(mess.isEmpty()){
                            Toast.makeText(MessageActivity.this, "You have to write a message", Toast.LENGTH_LONG).show();
                        }
                        else if(email.isEmpty()){
                            Toast.makeText(MessageActivity.this, "You have to add a valid Gmail address following this format: abc@gmail.com", Toast.LENGTH_LONG).show();
                        }
                        else {
                            //removing spaces from twitter username
                            String fixedTwitt = twitt.replace(" ", "");

                            //Secret sharing elements
                            byte[] secret = mess.getBytes();
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
                            sbGmail.append(identifier);
                            sbGmail.append(gmailMess);
                            gmailMess = sbGmail.toString();

                            sendValuesGmail.clear();
                            sendValuesGmail.add(gmailMess);
                            sendValuesGmail.add(email);

                            sendValuesTwitter.clear();
                            sendValuesTwitter.add(twitterMess);
                            sendValuesTwitter.add(fixedTwitt);


                            new SendGmail().execute(sendValuesGmail);
                            new SendTwitter().execute(sendValuesTwitter);

                        }
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
                sendMessage(mService, "me", createEmail(email, "me", "Message", message));
                Log.i("hallo", "Sent message: " + message + " to email: " + email);
            } catch(UserRecoverableAuthIOException e){
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);

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
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressSend.show();
        }

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
                return e.getErrorMessage();
            }
            try {
                twitter.directMessages().sendDirectMessage(userId,mess);
            } catch (TwitterException e) {
                e.printStackTrace();
                return e.getErrorMessage();
            }

            return "Twitter message sent";
        }


        @Override
        protected void onPostExecute(String code) {
            super.onPostExecute(code);
            Log.e("hallo", code);

            Toast.makeText(MessageActivity.this, code, Toast.LENGTH_SHORT).show();
            mProgressSend.hide();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressSend.hide();
        }
    }

    private class checkForMessage extends AsyncTask<String, String, HashMap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressCheck.show();
        }

        @Override
        protected HashMap doInBackground(String... params) {
            String gmailMessage = null;
            String query = params[0];
            try {
                gmailMessage = listMessagesMatchingQuery(mService, "me", query);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //twitter message
            String twitterMessage="";
            String fixedTwitterMessage;
            List<DirectMessage> messages = null;
            try {
                messages = twitter.getDirectMessages();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            if (messages != null) {
                for (DirectMessage message : messages){
                    if(message.getText().contains(query)) {

                        System.out.println(message.getSenderScreenName());
                        System.out.println(message.getCreatedAt());

                        String twitterSender2 = message.getSenderScreenName();
                        twitterSender = "@" + twitterSender2;
                        dateSent = message.getCreatedAt().toString();
                        dateSent = dateSent.substring(0, dateSent.indexOf("GMT"));
                        twitterMessage = message.getText();
                        break;
                    }
                }
            }
            else{
                Toast.makeText(MessageActivity.this, "You have no messages", Toast.LENGTH_SHORT).show();
            }

            fixedTwitterMessage = twitterMessage.replace(identifier, "");
            Log.i("Final twitter message: " , fixedTwitterMessage);

            HashMap message = new HashMap();
            message.put("gmail", gmailMessage);
            message.put("twitter", fixedTwitterMessage);
            return message;
        }

        public String listMessagesMatchingQuery(Gmail service, String userId, String query) throws IOException {
            ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

            String gmailMessage ="";
            String fixedGmailMessage;
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
            String from;
            for (Message message : messages) {
                String theMessage = getMessage(mService, "me", message.getId());


                if (theMessage.contains(identifier)) {
                    //getting the username of the sender
                    Message m = mService.users().messages().get("me", message.getId()).setFormat("full").setFields("payload, snippet").execute();
                    List<MessagePart> parts  = m.getPayload().getParts();
                    List<MessagePartHeader> headers = m.getPayload().getHeaders();

                    for(MessagePartHeader header:headers){
                        String name = header.getName();
                        if(name.equals("From")||name.equals("from")){
                            from = header.getValue();
                            System.out.println("from: " + from);
                            gmailSender = from;
                            break;
                        }
                    }
                    gmailMessage = theMessage;
                    break;
                }

            }



            fixedGmailMessage = gmailMessage.replace(identifier, "");

            if(fixedGmailMessage.contains("@")){
                fixedGmailMessage = fixedGmailMessage.substring(fixedGmailMessage.indexOf(" ") + 1);
                System.out.println("Gmail Message contains @");
            }


            if(fixedGmailMessage.length() > 60) {
                System.out.println("Fixed gmail message BEFORE second substring thing: " + fixedGmailMessage);
                try {
                    fixedGmailMessage = fixedGmailMessage.substring(0, fixedGmailMessage.indexOf(' '));
                }catch(StringIndexOutOfBoundsException e){
                    Log.e("Error", "> " + e.getMessage());
                }
                System.out.println("Fixed gmail message after second substring thing: " + fixedGmailMessage);
            }

            Log.i(" Final Gmail Message", fixedGmailMessage);

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

            if(!gmail.isEmpty() && !twitter.isEmpty()) {
                List<Part> parts = new ArrayList<Part>();
                parts.add(PartFormats.parse(gmail));
                parts.add(PartFormats.parse(twitter));

                Part[] p = parts.toArray(new Part[0]);
                try {
                    result = p[0].join(Arrays.copyOfRange(p, 1, p.length));
                    String stringResult = new String(result);
                    Log.i("RESULTAT", stringResult);
                    messageReply.setText(stringResult);
                    twitterSenderText.setText(twitterSender);
                    gmailSenderText.setText(gmailSender);
                    dateSentText.setText(dateSent);
                } catch (InputValidationException e) {
                    Toast.makeText(MessageActivity.this, "Something went wrong here. Try sending a message again", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(MessageActivity.this, "Something went wrong when merging the messages. Have you received both a gmail and a twitter message?", Toast.LENGTH_SHORT).show();
            }
            mProgressCheck.hide();

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressCheck.hide();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressSend.dismiss();
        mProgressCheck.dismiss();
    }
}
