package com.yourteam.todoapp.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yourteam.todoapp.MainActivity
import com.yourteam.todoapp.R
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Runs in the background at the scheduled time and rings the alarm. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        ReminderScheduler.showNotification(applicationContext, id, title)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "todo_id"
        const val KEY_TITLE = "todo_title"
    }
}

object ReminderScheduler {

    private const val CHANNEL_ID = "todo_alarms"
    private const val OLD_CHANNEL_ID = "todo_reminders"

    private fun workName(todoId: String) = "reminder_$todoId"

    /** Schedules (or re-schedules) the alarm for a task. Does nothing for done or undated tasks. */
    fun schedule(context: Context, todo: TodoItem) {
        cancel(context, todo.id)

        val dueMillis = todo.deadlineMillis ?: return
        if (todo.isCompleted) return

        val delay = reminderTimeMillis(dueMillis, todo.reminderMinutes) - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_ID to todo.id,
                    ReminderWorker.KEY_TITLE to todo.title,
                )
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(todo.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, todoId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(todoId))
    }

    /** The date picker gives midnight UTC of the chosen day; turn that into the chosen local time on that day. */
    private fun reminderTimeMillis(dueMillis: Long, reminderMinutes: Int): Long {
        val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dueMillis }
        return Calendar.getInstance().apply {
            clear()
            set(
                picked.get(Calendar.YEAR),
                picked.get(Calendar.MONTH),
                picked.get(Calendar.DAY_OF_MONTH),
                reminderMinutes / 60,
                reminderMinutes % 60,
                0,
            )
        }.timeInMillis
    }

    /** Creates the alarm channel: high importance, alarm ringtone, alarm volume, vibration. */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        // The old channel had a normal notification sound and can't be changed, so remove it.
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(CHANNEL_ID, "Task alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings when a task's reminder time arrives"
            setSound(alarmSound, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500)
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, todoId: String, title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task due today")
            .setContentText(title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT } // keep ringing until dismissed

        NotificationManagerCompat.from(context).notify(todoId.hashCode(), notification)
    }
}