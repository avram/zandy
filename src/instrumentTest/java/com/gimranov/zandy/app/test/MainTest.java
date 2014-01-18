package com.gimranov.zandy.app.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;

import com.gimranov.zandy.app.MainActivity;

public class MainTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private MainActivity mActivity;
	private Button loginButton;
	
	 public MainTest() {
	      super("com.gimranov.zandy.app", MainActivity.class);
	 }
	 
	 public void testPreconditions() {
		 assertNotNull(loginButton);
	 }
	 
	 @Override
	 protected void setUp() throws Exception {
		 super.setUp();
		 mActivity = this.getActivity();
		 loginButton = (Button) mActivity.findViewById(com.gimranov.zandy.app.R.id.loginButton);
	}
}
