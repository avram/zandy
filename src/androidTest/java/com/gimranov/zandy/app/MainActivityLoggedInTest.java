package com.gimranov.zandy.app;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.test.*;
import com.gimranov.zandy.app.test.BuildConfig;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityLoggedInTest {
    @Rule
    public IntentsTestRule<MainActivity> activityTestRule =
            new IntentsTestRule<>(MainActivity.class);

    @Before
    public void setUpCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext());
        preferences.edit()
                .putString("user_id", BuildConfig.TEST_USER_ID)
                .putString("user_key", BuildConfig.TEST_USER_KEY_READONLY)
                .commit();
    }

    @After
    public void clearCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext());
        preferences.edit().clear().commit();
    }

    @Before
    @After
    public void clearDatabase() {
        Database database = new Database(getTargetContext());
        database.resetAllData();
    }

    @Test
    public void loginButtonDoesNotShow() {
        onView(withId(R.id.loginButton)).check(matches(not(isDisplayed())));
    }
}