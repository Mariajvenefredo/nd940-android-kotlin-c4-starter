package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var context: Application

    private val reminderList = mutableListOf(
        ReminderDTO("Title1", "Description1", "Home", 0.01, 0.01),
        ReminderDTO("Title2", "Description2", "Work", 0.02, 0.02)
    )
    private val newReminder = ReminderDataItem("Title3", "Description3", "Pool", 0.03, 0.03)

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setupViewModel() {
        dataSource = spyk(FakeDataSource(reminderList))
        context = ApplicationProvider.getApplicationContext()

        saveReminderViewModel = SaveReminderViewModel(context, dataSource)
    }

    @After
    fun tearDown() {
        GlobalContext.stopKoin()
    }

    @Test
    fun saveReminder_ReminderIsSavedCorrectly() {
        val testReminder = newReminder
        val success = saveReminderViewModel.validateAndSaveReminder(testReminder)

        coVerify(exactly = 1) { dataSource.saveReminder(any()) }

        assertEquals(true, success)
        assertEquals(NavigationCommand.Back, saveReminderViewModel.navigationCommand.value)
        assertEquals("Reminder Saved !", saveReminderViewModel.showToast.value)
    }

    @Test
    fun saveReminder_ErrorNoTitle() {
        val testReminder = newReminder
        testReminder.title = null
        val success = saveReminderViewModel.validateAndSaveReminder(testReminder)

        assertEquals(false, success)
        assertEquals(R.string.err_enter_title, saveReminderViewModel.showSnackBarInt.value)
    }

    @Test
    fun saveReminder_ErrorNoLocation() {
        val testReminder = newReminder
        testReminder.location = null
        val success = saveReminderViewModel.validateAndSaveReminder(testReminder)

        assertEquals(false, success)
        assertEquals(R.string.err_select_location, saveReminderViewModel.showSnackBarInt.value)
    }

    @Test
    fun saveReminder_ClearData() {
        saveReminderViewModel.onClear()

        assertNull(saveReminderViewModel.reminderTitle.value)
        assertNull(saveReminderViewModel.reminderDescription.value)
        assertNull(saveReminderViewModel.reminderSelectedLocationStr.value)
        assertNull(saveReminderViewModel.latitude.value)
        assertNull(saveReminderViewModel.longitude.value)
        assertNull(saveReminderViewModel.selectedPOI.value)
    }
}