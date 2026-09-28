package com.yourteam.todoapp.data

import java.util.UUID

const val DEFAULT_REMINDER_MINUTES = 9 * 60

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val deadlineMillis: Long? = null,
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
)