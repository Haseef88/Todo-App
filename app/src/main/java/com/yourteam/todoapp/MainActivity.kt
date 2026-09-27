package com.yourteam.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.ui.TodoScreen
import com.yourteam.todoapp.ui.TodoViewModel
import com.yourteam.todoapp.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoAppTheme {
                val todoViewModel: TodoViewModel = viewModel()
                val todos by todoViewModel.filteredTodos.collectAsState()
                val searchQuery by todoViewModel.searchQuery.collectAsState()

                TodoScreen(
                    todos = todos,
                    searchQuery = searchQuery,
                    onSearchQueryChange = todoViewModel::updateSearchQuery,
                    onAddTodo = todoViewModel::addTodo,
                    onToggleTodo = todoViewModel::toggleTodo,
                    onDeleteTodo = todoViewModel::deleteTodo,
                    onEditTodo = todoViewModel::updateTodo,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoScreenPreview() {
    val previewTodos = listOf(
        TodoItem(title = "Buy groceries"),
        TodoItem(title = "Walk the dog", isCompleted = true),
        TodoItem(title = "Prepare meeting notes", deadline = "Tomorrow 5 PM"),
    )
    TodoAppTheme {
        TodoScreen(
            todos = previewTodos,
            searchQuery = "",
            onSearchQueryChange = {},
            onAddTodo = { _, _ -> },
            onToggleTodo = {},
            onDeleteTodo = {},
            onEditTodo = { _, _, _ -> },
        )
    }
}