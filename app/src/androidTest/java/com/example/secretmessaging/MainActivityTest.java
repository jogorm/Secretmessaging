package com.example.secretmessaging;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Button;

import com.beardedhen.androidbootstrap.BootstrapButton;

/**
 * Created by joakimgormrandulff on 13.09.2016.
 */

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @SmallTest
    public void testGmailButton(){
        com.google.android.gms.common.SignInButton gmail = (com.google.android.gms.common.SignInButton)getActivity().findViewById(R.id.button);
        assertNotNull(gmail);
    }

    public void testTwitterButton(){
        BootstrapButton twitter = (BootstrapButton)getActivity().findViewById(R.id.bootstrapButton);
        assertNotNull(twitter);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
