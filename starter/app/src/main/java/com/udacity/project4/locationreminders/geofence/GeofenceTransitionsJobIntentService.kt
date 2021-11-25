package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService() :
    JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob
    private val remindersLocalRepository: ReminderDataSource by inject()

    companion object {
        private const val JOB_ID = 573

        private const val ACTION_GEOFENCE_EVENT =
            "locationReminders.ACTION_GEOFENCE_EVENT"

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofenceEvent = GeofencingEvent.fromIntent(intent)

            if (geofenceEvent.hasError())
                return

            /**
             * send a notification to the user when he enters the geofence area
             */
            if (geofenceEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val geofences = geofenceEvent.triggeringGeofences
                geofences.forEach { geofence ->
                    val requestId = geofence.requestId
                    sendNotification(requestId)
                }
            }
        }
    }

    //TODO: get the request id of the current geofence
    private fun sendNotification(requestId: String?) {

        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            if (requestId != null) {
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }
}