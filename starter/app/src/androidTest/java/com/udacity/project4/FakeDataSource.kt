package com.udacity.project4

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>) : ReminderDataSource {

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Exception")
        }
        return Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO): Long {
        if (shouldReturnError) {
            return 0
        }
        reminders.add(reminder)
        return 1
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Exception")
        }
        val reminder = reminders.firstOrNull { r -> r.id == id }

        return if (reminder != null) {
            Result.Success(reminder)
        } else {
            Result.Error("Exception")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}