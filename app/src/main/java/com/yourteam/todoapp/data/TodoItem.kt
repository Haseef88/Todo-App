package com.yourteam.todoapp.data

import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val deadline: String? = null,
)
