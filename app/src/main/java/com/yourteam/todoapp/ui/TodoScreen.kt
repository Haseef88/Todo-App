package com.yourteam.todoapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.yourteam.todoapp.data.DEFAULT_REMINDER_MINUTES
import com.yourteam.todoapp.data.NetworkConfig
import com.yourteam.todoapp.data.TodoItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    todos: List<TodoItem>,
    errorMessage: String?,
    onErrorShown: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddTodo: (title: String, deadlineMillis: Long?, reminderMinutes: Int, imageUri: Uri?) -> Unit,
    onToggleTodo: (String) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onEditTodo: (id: String, title: String, deadlineMillis: Long?, reminderMinutes: Int) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onPickImage: (id: String, uri: Uri) -> Unit,
    onRemoveImage: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var titleInput by rememberSaveable { mutableStateOf("") }
    var deadlineInput by rememberSaveable { mutableStateOf<Long?>(null) }
    var reminderInput by rememberSaveable { mutableStateOf(DEFAULT_REMINDER_MINUTES) }
    var pendingImage by rememberSaveable { mutableStateOf<Uri?>(null) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    val completedCount = todos.count { it.isCompleted }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "My Todo List",
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (todos.isNotEmpty()) {
                    Text(
                        text = "$completedCount of ${todos.size} done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search todos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhotoSourceMenu(onPhoto = { pendingImage = it }) { openMenu ->
                        TextButton(onClick = openMenu) {
                            Text(if (pendingImage == null) "Add photo" else "Change photo")
                        }
                    }
                    val chosenImage = pendingImage
                    if (chosenImage != null) {
                        AsyncImage(
                            model = chosenImage,
                            contentDescription = "Photo for the new task",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        IconButton(onClick = { pendingImage = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove photo")
                        }
                    } else {
                        Text(
                            text = "optional",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeadlineField(
                        valueMillis = deadlineInput,
                        onValueChange = { deadlineInput = it },
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onAddTodo(titleInput, deadlineInput, reminderInput, pendingImage)
                            titleInput = ""
                            deadlineInput = null
                            reminderInput = DEFAULT_REMINDER_MINUTES
                            pendingImage = null
                        },
                    ) {
                        Text("Add")
                    }
                }

                if (deadlineInput != null) {
                    ReminderTimeField(
                        minutes = reminderInput,
                        onChange = { reminderInput = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No todos yet — add one above."
                        } else {
                            "No todos match \"$searchQuery\"."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = todos, key = { it.id }) { todo ->
                        TodoCard(
                            todo = todo,
                            onToggle = { onToggleTodo(todo.id) },
                            onEdit = { editingTodo = todo },
                            onPickImage = { uri -> onPickImage(todo.id, uri) },
                            onRemoveImage = { onRemoveImage(todo.id) },
                            onDelete = {
                                onDeleteTodo(todo.id)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestoreTodo(todo)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    val todoBeingEdited = editingTodo
    if (todoBeingEdited != null) {
        EditTodoDialog(
            todo = todoBeingEdited,
            onDismiss = { editingTodo = null },
            onConfirm = { newTitle, newDeadline, newReminder ->
                onEditTodo(todoBeingEdited.id, newTitle, newDeadline, newReminder)
                editingTodo = null
            },
        )
    }
}

/** Gives back two actions: choose a photo from the gallery, or take a new one with the camera. */
private class PhotoActions(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

@Composable
private fun TodoCard(
    todo: TodoItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPickImage: (Uri) -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (todo.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    // The ?v= number changes whenever the photo is replaced or removed,
    // so the app never shows an old cached picture.
    val imageUrl = "${NetworkConfig.BASE_URL}todos/${todo.id}/image?v=${todo.imageVersion}"

    var showFullImage by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
            )

            if (todo.hasImage) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Photo for ${todo.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullImage = true },
                )
            } else {
                PhotoSourceMenu(onPhoto = onPickImage) { openMenu ->
                    IconButton(onClick = openMenu) {
                        Icon(Icons.Default.Add, contentDescription = "Add photo to ${todo.title}")
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (todo.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                )
                todo.deadlineMillis?.let { millis ->
                    Text(
                        text = "Due: ${formatDate(millis)} · Reminder ${formatTime(todo.reminderMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${todo.title}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${todo.title}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showFullImage) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Photo for ${todo.title}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The dialog stays open until the new photo actually comes back from the camera/gallery.
                        PhotoSourceMenu(
                            onPhoto = { uri ->
                                showFullImage = false
                                onPickImage(uri)
                            },
                        ) { openMenu ->
                            TextButton(onClick = openMenu) {
                                Text("Replace photo")
                            }
                        }
                        TextButton(
                            onClick = {
                                showFullImage = false
                                onRemoveImage()
                            },
                        ) {
                            Text("Remove photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPhotoActions(onPhoto: (Uri) -> Unit): PhotoActions {
    val context = LocalContext.current
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(currentOnPhoto) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) currentOnPhoto(uri)
    }

    return remember {
        PhotoActions(
            pickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            takePhoto = {
                val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraUri = uri
                cameraLauncher.launch(uri)
            },
        )
    }
}

/** Shows whatever button you give it; tapping it opens a small menu: "Take a photo" / "Choose from gallery". */
@Composable
private fun PhotoSourceMenu(
    onPhoto: (Uri) -> Unit,
    anchor: @Composable (openMenu: () -> Unit) -> Unit,
) {
    val actions = rememberPhotoActions(onPhoto)
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        anchor { menuOpen = true }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Take a photo") },
                onClick = {
                    menuOpen = false
                    actions.takePhoto()
                },
            )
            DropdownMenuItem(
                text = { Text("Choose from gallery") },
                onClick = {
                    menuOpen = false
                    actions.pickFromGallery()
                },
            )
        }
    }
}

@Composable
private fun EditTodoDialog(
    todo: TodoItem,
    onDismiss: () -> Unit,
    onConfirm: (title: String, deadlineMillis: Long?, reminderMinutes: Int) -> Unit,
) {
    var titleInput by remember { mutableStateOf(todo.title) }
    var deadlineInput by remember { mutableStateOf(todo.deadlineMillis) }
    var reminderInput by remember { mutableStateOf(todo.reminderMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DeadlineField(
                    valueMillis = deadlineInput,
                    onValueChange = { deadlineInput = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (deadlineInput != null) {
                    ReminderTimeField(
                        minutes = reminderInput,
                        onChange = { reminderInput = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(titleInput, deadlineInput, reminderInput) },
                enabled = titleInput.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TodoCard(
    todo: TodoItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPickImage: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (todo.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    var showFullImage by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
            )

            if (todo.hasImage) {
                AsyncImage(
                    model = "${NetworkConfig.BASE_URL}todos/${todo.id}/image",
                    contentDescription = "Photo for ${todo.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullImage = true },
                )
            } else {
                PhotoSourceMenu(onPhoto = onPickImage) { openMenu ->
                    IconButton(onClick = openMenu) {
                        Icon(Icons.Default.Add, contentDescription = "Add photo to ${todo.title}")
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (todo.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                )
                todo.deadlineMillis?.let { millis ->
                    Text(
                        text = "Due: ${formatDate(millis)} · Reminder ${formatTime(todo.reminderMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${todo.title}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${todo.title}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showFullImage) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            AsyncImage(
                model = "${NetworkConfig.BASE_URL}todos/${todo.id}/image",
                contentDescription = "Photo for ${todo.title}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineField(
    valueMillis: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valueMillis?.let { formatDate(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Deadline (optional)") },
        leadingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick deadline date")
            }
        },
        trailingIcon = {
            if (valueMillis != null) {
                IconButton(onClick = { onValueChange(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear deadline")
                }
            }
        },
        singleLine = true,
        modifier = modifier,
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = valueMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValueChange(it) }
                        showPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeField(
    minutes: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = formatTime(minutes),
        onValueChange = {},
        readOnly = true,
        label = { Text("Reminder time") },
        leadingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Notifications, contentDescription = "Pick reminder time")
            }
        },
        singleLine = true,
        modifier = modifier,
    )

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(pickerState.hour * 60 + pickerState.minute)
                        showPicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}

private fun formatTime(minutes: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(calendar.time)
}