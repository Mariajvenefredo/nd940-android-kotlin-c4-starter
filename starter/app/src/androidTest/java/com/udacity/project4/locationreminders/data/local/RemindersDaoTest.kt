package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private val newReminder = ReminderDTO("Title1", "Description1", "Pool", 0.01, 0.01)
    private val newReminder2 = ReminderDTO("Title2", "Description1", "Pool", 0.01, 0.01)

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        database.reminderDao().saveReminder(newReminder)

        val loaded = database.reminderDao().getReminderById(newReminder.id)

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.id).isEqualTo(newReminder.id)
        assertThat(loaded.title).isEqualTo(newReminder.title)
        assertThat(loaded.description).isEqualTo(newReminder.description)
        assertThat(loaded.location).isEqualTo(newReminder.location)
        assertThat(loaded.latitude).isEqualTo(newReminder.latitude)
        assertThat(loaded.longitude).isEqualTo(newReminder.longitude)
    }

    @Test
    fun insertDeleteAndGetReminder() = runBlockingTest {
        database.reminderDao().saveReminder(newReminder)
        database.reminderDao().deleteAllReminders()

        val all = database.reminderDao().getReminders()

        assertThat(all).isEmpty()
    }

    @Test
    fun insertAndGetSeveralReminders() = runBlockingTest {
        database.reminderDao().saveReminder(newReminder)
        database.reminderDao().saveReminder(newReminder2)

        val all = database.reminderDao().getReminders()

        assertThat(all).isNotEmpty()
        assertThat(all.count()).isEqualTo(2)
    }
}