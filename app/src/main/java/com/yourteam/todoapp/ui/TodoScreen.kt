package com.yourteam.todoapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.yourteam.todoapp.data.TodoItem

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    var newTodoTitle by remember { mutableStateOf("") }
    var newTodoDeadline by remember { mutableStateOf("") }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val todos by viewModel.filteredTodos.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Todo List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search Todos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // --- Add New Todo Input ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTodoTitle,
                onValueChange = { newTodoTitle = it },
                label = { Text("Add new task") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTodoDeadline,
                    onValueChange = { newTodoDeadline = it },
                    label = { Text("Deadline (e.g., Tomorrow 5 PM)") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Deadline") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newTodoTitle.isNotBlank()) {
                            viewModel.addTodo(newTodoTitle, newTodoDeadline)
                            newTodoTitle = ""
                            newTodoDeadline = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Todo List ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(todos, key = { it.id }) { todo ->
                TodoItemRow(
                    todo = todo,
                    onToggle = { viewModel.toggleTodo(todo.id) },
                    onDelete = { viewModel.deleteTodo(todo.id) }
                )
            }
        }
    }
}

@Composable
fun TodoItemRow(
    todo: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!todo.deadline.isNullOrEmpty()) {
                    Text(
                        text = "Due: ${todo.deadline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Todo")
            }
        }
    }
}