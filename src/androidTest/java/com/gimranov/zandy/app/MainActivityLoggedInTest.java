package com.gimranov.zandy.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.gimranov.zandy.app.data.Database;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityLoggedInTest {
    @Rule
    public IntentsTestRule<MainActivity> activityTestRule =
            new IntentsTestRule<MainActivity>(MainActivity.class){
                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    preferences.edit()
                            .putString("user_id", BuildConfig.TEST_USER_ID)
                            .putString("user_key", BuildConfig.TEST_USER_KEY_READONLY)
                            .commit();
                }
            };

    @Before
    public void setUpCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.edit()
                .putString("user_id", BuildConfig.TEST_USER_ID)
                .putString("user_key", BuildConfig.TEST_USER_KEY_READONLY)
                .commit();
    }

    @After
    public void clearCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.edit().clear().commit();
    }

    @Before
    @After
    public void clearDatabase() {
        Database database = new Database(getApplicationContext());
        database.resetAllData();
    }

    @Test
    public void loginButtonDoesNotShow() {
        onView(withId(R.id.loginButton)).check(matches(not(isDisplayed())));
    }
}