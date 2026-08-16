package com.yourteam.todoapp.ui

import androidx.lifecycle.ViewModel
import com.yourteam.todoapp.data.TodoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class TodoViewModel : ViewModel() {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dynamically filters todos whenever the list or the search query changes
    val filteredTodos: StateFlow<List<TodoItem>> = combine(_todos, _searchQuery) { todos, query ->
        if (query.isBlank()) {
            todos
        } else {
            todos.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = androidx.lifecycle.viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addTodo(title: String) {
        if (title.isBlank()) return
        val newItem = TodoItem(title = title)
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