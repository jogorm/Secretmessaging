package com.example.secretmessaging;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by joakimgormrandulff on 13.09.2016.
 */

public class MessageActivityTest extends ActivityInstrumentationTestCase2<MessageActivity> {
    public MessageActivityTest() {
        super(MessageActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

    }

    @SmallTest
    public void testEditTextMessage(){
        EditText message = (EditText)getActivity().findViewById(R.id.messageText);
        assertNotNull(message);
    }

    @SmallTest
    public void testEditTextGmail(){
        EditText email = (EditText)getActivity().findViewById(R.id.emailText);
        assertNotNull(email);
    }

    @SmallTest
    public void testEditTextTwitter(){
        EditText twitter = (EditText)getActivity().findViewById(R.id.twitterText);
        assertNotNull(twitter);
    }

    @SmallTest
    public void testSendButton(){
        Button send = (Button)getActivity().findViewById(R.id.sendButton);
        assertNotNull(send);
    }
    @SmallTest
    public void testCheckButton(){
        Button check = (Button)getActivity().findViewById(R.id.checkForEmail);
        assertNotNull(check);
    }

    @SmallTest
    public void testMessageText(){
        TextView message = (TextView)getActivity().findViewById(R.id.messageReply);
        assertNotNull(message);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
