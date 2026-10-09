package com.yourteam.todoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.ui.TodoScreen
import com.yourteam.todoapp.ui.TodoViewModel
import com.yourteam.todoapp.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            TodoAppTheme {
                val todoViewModel: TodoViewModel = viewModel()
                val todos by todoViewModel.filteredTodos.collectAsState()
                val searchQuery by todoViewModel.searchQuery.collectAsState()
                val errorMessage by todoViewModel.errorMessage.collectAsState()

                TodoScreen(
                    todos = todos,
                    errorMessage = errorMessage,
                    onErrorShown = todoViewModel::clearError,
                    searchQuery = searchQuery,
                    onSearchQueryChange = todoViewModel::updateSearchQuery,
                    onAddTodo = todoViewModel::addTodo,
                    onToggleTodo = todoViewModel::toggleTodo,
                    onDeleteTodo = todoViewModel::deleteTodo,
                    onEditTodo = todoViewModel::updateTodo,
                    onRestoreTodo = todoViewModel::restoreTodo,
                    onPickImage = todoViewModel::onImagePicked,
                    onRemoveImage = todoViewModel::removeImage,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoScreenPreview() {
    val previewTodos = listOf(
        TodoItem(title = "Buy groceries"),
        TodoItem(title = "Walk the dog", isCompleted = true),
        TodoItem(title = "Prepare meeting notes", deadlineMillis = System.currentTimeMillis()),
    )
    TodoAppTheme {
        TodoScreen(
            todos = previewTodos,
            errorMessage = null,
            onErrorShown = {},
            searchQuery = "",
            onSearchQueryChange = {},
            onAddTodo = { _, _, _, _ -> },
            onToggleTodo = {},
            onDeleteTodo = {},
            onEditTodo = { _, _, _, _ -> },
            onRestoreTodo = {},
            onPickImage = { _, _ -> },
            onRemoveImage = {},
        )
    }
}