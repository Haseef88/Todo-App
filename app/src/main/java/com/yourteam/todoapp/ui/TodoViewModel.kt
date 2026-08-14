package com.yourteam.todoapp.ui

import androidx.lifecycle.ViewModel
import com.yourteam.todoapp.data.TodoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoViewModel : ViewModel() {

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    fun addTodo(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        _todos.value = _todos.value + TodoItem(title = trimmedTitle)
    }

    fun deleteTodo(id: String) {
        _todos.value = _todos.value.filterNot { it.id == id }
    }

    fun toggleTodo(id: String) {
        _todos.value = _todos.value.map { todo ->
            if (todo.id == id) todo.copy(isCompleted = !todo.isCompleted) else todo
        }
    }
}
