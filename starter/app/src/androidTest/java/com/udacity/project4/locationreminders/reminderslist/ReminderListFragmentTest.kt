package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.FakeDataSource
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito

@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private lateinit var appContext: Application

    private val reminderList = mutableListOf(
        ReminderDTO("Title1", "Description1", "Home", 0.01, 0.01),
        ReminderDTO("Title2", "Description2", "Work", 0.02, 0.02)
    )

    //Needed to add this code here because when i ran tests all together it did not start the test application
    @Before
    fun startKoin() {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()

        val dataSource = FakeDataSource(reminderList)

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    dataSource
                )
            }
        }

        GlobalContext.startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun closeKoin() {
        stopKoin()
    }

    @Test
    fun fragmentIsShowing_ListAppears() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.remindersRecyclerView))
            .check(ViewAssertions.matches(isDisplayed()))
        onView(
            allOf(
                withId(R.id.remindersRecyclerView),
                hasDescendant(withText("Title1")),
                hasDescendant(withText("Title2")),
                isDisplayed()
            )
        )
    }

    @Test
    fun fragmentIsShowing_ClickToNewReminder() {
        val navController = Mockito.mock(NavController::class.java)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB))
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(click())

        Mockito.verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
}