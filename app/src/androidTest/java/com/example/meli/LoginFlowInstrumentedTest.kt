package com.example.meli

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowInstrumentedTest {

    @After
    fun tearDown() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    // Confirms the login screen starts in the expected signed-out state for first-time users.
    fun loginScreen_startsInSignInMode() {
        launchLoggedOutMainActivity().use {
            onView(withId(R.id.signInTabText)).check(matches(isDisplayed()))
            onView(withId(R.id.actionButton)).check(matches(withText("Sign In")))
            onView(withId(R.id.nameInputLayout)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.createUsernameInputLayout)).check(matches(withEffectiveVisibility(GONE)))
        }
    }

    @Test
    // Confirms the create-account flow reveals the extra fields needed to register a new user.
    fun createAccountTab_showsAccountCreationFields() {
        launchLoggedOutMainActivity().use {
            onView(withId(R.id.createAccountTabText)).perform(click())

            onView(withId(R.id.actionButton)).check(matches(withText("Create Account")))
            onView(withId(R.id.nameInputLayout)).check(matches(isDisplayed()))
            onView(withId(R.id.createUsernameInputLayout)).check(matches(isDisplayed()))
        }
    }

    @Test
    // Confirms the current auth flow state survives recreation so rotation does not reset the screen.
    fun createAccountState_survivesActivityRecreation() {
        launchLoggedOutMainActivity().use { scenario ->
            onView(withId(R.id.createAccountTabText)).perform(click())
            scenario.recreate()

            onView(withId(R.id.actionButton)).check(matches(withText("Create Account")))
            onView(withId(R.id.nameInputLayout)).check(matches(isDisplayed()))
            onView(withId(R.id.createUsernameInputLayout)).check(matches(isDisplayed()))
        }
    }

    private fun launchLoggedOutMainActivity(): ActivityScenario<MainActivity> {
        FirebaseAuth.getInstance().signOut()
        return ActivityScenario.launch(MainActivity::class.java)
    }
}
