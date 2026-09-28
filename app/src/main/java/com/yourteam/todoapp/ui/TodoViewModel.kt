package com.yourteam.todoapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.todoapp.data.ReminderScheduler
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.yourteam.todoapp.data.DEFAULT_REMINDER_MINUTES

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
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
        // Pending first, then tasks with a due date (soonest first), then undated tasks.
        filtered.sortedWith(
            compareBy<TodoItem> { it.isCompleted }
                .thenBy { it.deadlineMillis == null }
                .thenBy { it.deadlineMillis ?: Long.MAX_VALUE }
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

    fun addTodo(
        title: String,
        deadlineMillis: Long? = null,
        reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val newTodo = TodoItem(
            title = cleanTitle,
            deadlineMillis = deadlineMillis,
            reminderMinutes = reminderMinutes,
        )
        updateTodos(_todos.value + newTodo)
        ReminderScheduler.schedule(appContext, newTodo)
    }

    fun updateTodo(id: String, title: String, deadlineMillis: Long?, reminderMinutes: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val existing = _todos.value.firstOrNull { it.id == id } ?: return
        val updated = existing.copy(
            title = cleanTitle,
            deadlineMillis = deadlineMillis,
            reminderMinutes = reminderMinutes,
        )
        updateTodos(_todos.value.map { if (it.id == id) updated else it })
        ReminderScheduler.schedule(appContext, updated)
    }

    fun toggleTodo(id: String) {
        val existing = _todos.value.firstOrNull { it.id == id } ?: return
        val toggled = existing.copy(isCompleted = !existing.isCompleted)
        updateTodos(_todos.value.map { if (it.id == id) toggled else it })
        ReminderScheduler.schedule(appContext, toggled)
    }

    fun deleteTodo(id: String) {
        updateTodos(_todos.value.filterNot { it.id == id })
        ReminderScheduler.cancel(appContext, id)
    }

    fun restoreTodo(todo: TodoItem) {
        if (_todos.value.any { it.id == todo.id }) return
        updateTodos(_todos.value + todo)
        ReminderScheduler.schedule(appContext, todo)
    }

    private fun updateTodos(newTodos: List<TodoItem>) {
        _todos.value = newTodos
        viewModelScope.launch {
            repository.saveTodos(newTodos)
        }
    }
}