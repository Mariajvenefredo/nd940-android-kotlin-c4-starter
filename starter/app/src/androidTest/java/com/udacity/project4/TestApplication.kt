package com.udacity.project4

import android.app.Application
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class TestApplication : Application() {
    private val reminderList = mutableListOf(
        ReminderDTO("Title1", "Description1", "Home", 0.01, 0.01),
        ReminderDTO("Title2", "Description2", "Work", 0.02, 0.02)
    )

    override fun onCreate() {
        super.onCreate()
        val dataSource = FakeDataSource(reminderList)

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    this@TestApplication,
                    dataSource
                )
            }
        }

        GlobalContext.startKoin {
            modules(listOf(myModule))
        }
    }
}