package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val newReminder = ReminderDTO("Title1", "Description1", "Pool", 0.01, 0.01)
    private val newReminder2 = ReminderDTO("Title2", "Description1", "Pool", 0.01, 0.01)

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        localDataSource = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_getsReminder() = runBlocking {
        localDataSource.saveReminder(newReminder)

        val result = localDataSource.getReminder(newReminder.id)

        assertThat(result).isEqualTo(Result.Success(newReminder))

        val data = (result as Result.Success<ReminderDTO>).data

        assertThat(data.id).isEqualTo(newReminder.id)
        assertThat(data.title).isEqualTo(newReminder.title)
        assertThat(data.description).isEqualTo(newReminder.description)
        assertThat(data.location).isEqualTo(newReminder.location)
        assertThat(data.latitude).isEqualTo(newReminder.latitude)
        assertThat(data.longitude).isEqualTo(newReminder.longitude)
    }

    @Test
    fun saveReminders_getsReminders() = runBlocking {
        val reminders = listOf(newReminder, newReminder2)

        localDataSource.saveReminder(newReminder)
        localDataSource.saveReminder(newReminder2)

        val result = localDataSource.getReminders()

        assertThat(result).isEqualTo(Result.Success(reminders))

        val data = (result as Result.Success<List<ReminderDTO>>).data

        assertThat(data[0]).isEqualTo(newReminder)
        assertThat(data[1]).isEqualTo(newReminder2)
    }

    @Test
    fun getReminder_ReturnsError() = runBlocking {
        localDataSource.saveReminder(newReminder)

        val result = localDataSource.getReminder("nonexistent_id")

        assertThat(result).isEqualTo(Result.Error("Reminder not found!"))
    }

    @Test
    fun saveReminders_deleteReminders() = runBlocking {
        localDataSource.saveReminder(newReminder)
        localDataSource.saveReminder(newReminder2)

        localDataSource.deleteAllReminders()

        val result = localDataSource.getReminders()

        val data = (result as Result.Success<List<ReminderDTO>>).data

        assertThat(data.count()).isEqualTo(0)
    }
}