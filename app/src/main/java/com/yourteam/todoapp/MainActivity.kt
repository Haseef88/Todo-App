package com.yourteam.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.ui.TodoScreen
import com.yourteam.todoapp.ui.TodoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val todoViewModel: TodoViewModel = viewModel()
                val todos by todoViewModel.todos.collectAsState()

                TodoScreen(
                    todos = todos,
                    onAddTodo = todoViewModel::addTodo,
                    onToggleTodo = todoViewModel::toggleTodo,
                    onDeleteTodo = todoViewModel::deleteTodo,
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
        TodoItem(title = "Prepare meeting notes"),
    )
    MaterialTheme {
        TodoScreen(
            todos = previewTodos,
            onAddTodo = {},
            onToggleTodo = {},
            onDeleteTodo = {},
        )
    }
}
