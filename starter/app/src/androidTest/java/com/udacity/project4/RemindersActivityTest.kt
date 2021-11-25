package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val reminderList = mutableListOf(
        ReminderDTO("Title1", "Description1", "Home", 0.01, 0.01),
        ReminderDTO("Title2", "Description2", "Work", 0.02, 0.02)
    )

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainActivityTestRule: ActivityTestRule<RemindersActivity> = ActivityTestRule(
        RemindersActivity::class.java
    )

    @Before
    fun init() {
        //stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        //stops test application (not needed in this test)
        stopKoin()

        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
            repository.saveReminder(reminderList[0])
            repository.saveReminder(reminderList[1])
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        stopKoin()
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun addReminder_successFlow() {
        // Start up Tasks screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //has 2 reminders
        Espresso.onView(
            allOf(
                withId(R.id.remindersRecyclerView),
                hasDescendant(withText("Title1")),
                hasDescendant(withText("Title2")),
                isDisplayed()
            )
        )

        //selects add reminder
        Espresso.onView(withId(R.id.addReminderFAB))
            .check(matches(isDisplayed()))
            .perform(ViewActions.click())

        //selects save reminder BEFORE having a title
        Espresso.onView(withId(R.id.saveReminder))
            .check(matches(isDisplayed()))
            .perform(ViewActions.click())

        //Shows snackbar saying title is missing
        Espresso.onView(allOf(withId(R.id.snackbar_text), withText(R.string.err_enter_title)))
            .check(matches(isDisplayed()));

        //defines a title for reminder
        Espresso.onView(withId(R.id.reminderTitle))
            .check(matches(isDisplayed()))
            .perform(ViewActions.typeText("Title3"))

        //defines a description for reminder
        Espresso.onView(withId(R.id.reminderDescription))
            .check(matches(isDisplayed()))
            .perform(ViewActions.typeText("Description3"))

        //opens map
        Espresso.onView(withId(R.id.selectLocation))
            .check(matches(isDisplayed()))
            .perform(ViewActions.click())

        //selects POI
        Espresso.onView(withId(R.id.map))
            .check(matches(isDisplayed()))
            .perform(ViewActions.longClick())

        //confirms selected POI
        Espresso.onView(withId(R.id.confirmPOI))
            .check(matches(isDisplayed()))
            .perform(ViewActions.click())

        //selects save reminder
        Espresso.onView(withId(R.id.saveReminder))
            .check(matches(isDisplayed()))
            .perform(ViewActions.click())

        Espresso.onView(
            withText(R.string.geofence_added)
        ).inRoot(withDecorView(not(mainActivityTestRule.activity.window.decorView)))
            .check(matches(isDisplayed()))

        //has all 3 reminders
        Espresso.onView(
            allOf(
                withId(R.id.remindersRecyclerView),
                hasDescendant(withText("Title1")),
                hasDescendant(withText("Title2")),
                hasDescendant(withText("Title3")),
                isDisplayed()
            )
        )
        activityScenario.close()
    }
}
