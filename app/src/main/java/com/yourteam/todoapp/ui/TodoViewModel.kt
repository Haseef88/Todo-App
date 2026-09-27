package com.yourteam.todoapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TodoRepository(application)

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTodos: StateFlow<List<TodoItem>> = combine(_todos, _searchQuery) { todos, query ->
        val filtered = if (query.isBlank()) {
            todos
        } else {
            todos.filter { it.title.contains(query, ignoreCase = true) }
        }
        // Pending tasks first, tasks with a deadline before those without,
        // completed tasks sink to the bottom.
        filtered.sortedWith(
            compareBy<TodoItem> { it.isCompleted }
                .thenBy { it.deadline == null }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            repository.todosFlow.collect { savedTodos ->
                _todos.value = savedTodos
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTodo(title: String, deadline: String? = null) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanDeadline = deadline?.trim()?.takeIf { it.isNotEmpty() }
        updateTodos(_todos.value + TodoItem(title = cleanTitle, deadline = cleanDeadline))
    }

    fun updateTodo(id: String, title: String, deadline: String?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanDeadline = deadline?.trim()?.takeIf { it.isNotEmpty() }
        updateTodos(
            _todos.value.map { todo ->
                if (todo.id == id) todo.copy(title = cleanTitle, deadline = cleanDeadline) else todo
            }
        )
    }

    fun toggleTodo(id: String) {
        updateTodos(
            _todos.value.map { todo ->
                if (todo.id == id) todo.copy(isCompleted = !todo.isCompleted) else todo
            }
        )
    }

    fun deleteTodo(id: String) {
        updateTodos(_todos.value.filterNot { it.id == id })
    }

    private fun updateTodos(newTodos: List<TodoItem>) {
        _todos.value = newTodos
        viewModelScope.launch {
            repository.saveTodos(newTodos)
        }
    }
}