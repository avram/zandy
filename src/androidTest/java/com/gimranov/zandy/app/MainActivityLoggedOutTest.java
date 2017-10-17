package com.gimranov.zandy.app;

import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityLoggedOutTest {
    @Rule
    public IntentsTestRule<MainActivity> activityTestRule =
            new IntentsTestRule<>(MainActivity.class);

    @Test
    public void loginButtonShowsOnLaunch() {
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }

    @Test
    public void loginButtonLaunchesOauth() throws Exception {
        onView(withId(R.id.loginButton)).perform(click());
        Thread.sleep(1000);
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(new BaseMatcher<Uri>() {
            @Override
            public boolean matches(Object item) {
                return ((Uri) item).getHost().contains("zotero.org");
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("should have host zotero.org");
            }
        })));
    }

    @Test
    public void viewCollectionsLaunchesActivity() throws Exception {
        onView(withId(R.id.collectionButton)).perform(click());
        intended(hasComponent(hasClassName(CollectionActivity.class.getName())));
    }
}