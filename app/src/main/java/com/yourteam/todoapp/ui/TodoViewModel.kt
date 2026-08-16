package com.yourteam.todoapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.todoapp.data.TodoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TodoViewModel : ViewModel() {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTodos: StateFlow<List<TodoItem>> = combine(_todos, _searchQuery) { todos, query ->
        if (query.isBlank()) {
            todos
        } else {
            todos.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addTodo(title: String, deadline: String? = null) {
        if (title.isBlank()) return
        val cleanDeadline = if (deadline.isNullOrBlank()) null else deadline.trim()
        val newItem = TodoItem(title = title, deadline = cleanDeadline)
        _todos.value = _todos.value + newItem
    }

    fun toggleTodo(id: String) {
        _todos.value = _todos.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun deleteTodo(id: String) {
        _todos.value = _todos.value.filter { it.id != id }
    }
}