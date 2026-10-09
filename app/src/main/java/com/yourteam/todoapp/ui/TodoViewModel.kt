package com.yourteam.todoapp.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.todoapp.data.DEFAULT_REMINDER_MINUTES
import com.yourteam.todoapp.data.ReminderScheduler
import com.yourteam.todoapp.data.TodoItem
import com.yourteam.todoapp.data.TodoRepository
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository = TodoRepository()

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val filteredTodos: StateFlow<List<TodoItem>> = combine(_todos, _searchQuery) { todos, query ->
        val filtered = if (query.isBlank()) {
            todos
        } else {
            todos.filter { it.title.contains(query, ignoreCase = true) }
        }
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
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            runCatching { repository.getTodos() }
                .onSuccess { todos ->
                    _todos.value = todos
                    todos.forEach { ReminderScheduler.schedule(appContext, it) }
                }
                .onFailure { showError() }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTodo(
        title: String,
        deadlineMillis: Long? = null,
        reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
        imageUri: Uri? = null,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val draft = TodoItem(title = cleanTitle, deadlineMillis = deadlineMillis, reminderMinutes = reminderMinutes)
        viewModelScope.launch {
            runCatching { repository.addTodo(draft) }
                .onSuccess { created ->
                    _todos.value = _todos.value + created
                    ReminderScheduler.schedule(appContext, created)
                    // If a photo was chosen while typing the task, upload it now that the task exists.
                    if (imageUri != null) uploadImage(created.id, imageUri)
                }
                .onFailure { showError() }
        }
    }

    fun updateTodo(id: String, title: String, deadlineMillis: Long?, reminderMinutes: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val existing = _todos.value.firstOrNull { it.id == id } ?: return
        val draft = existing.copy(title = cleanTitle, deadlineMillis = deadlineMillis, reminderMinutes = reminderMinutes)
        viewModelScope.launch {
            runCatching { repository.updateTodo(draft) }
                .onSuccess { updated ->
                    _todos.value = _todos.value.map { if (it.id == id) updated else it }
                    ReminderScheduler.schedule(appContext, updated)
                }
                .onFailure { showError() }
        }
    }

    fun toggleTodo(id: String) {
        val existing = _todos.value.firstOrNull { it.id == id } ?: return
        val draft = existing.copy(isCompleted = !existing.isCompleted)
        viewModelScope.launch {
            runCatching { repository.updateTodo(draft) }
                .onSuccess { updated ->
                    _todos.value = _todos.value.map { if (it.id == id) updated else it }
                    ReminderScheduler.schedule(appContext, updated)
                }
                .onFailure { showError() }
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTodo(id) }
                .onSuccess {
                    _todos.value = _todos.value.filterNot { it.id == id }
                    ReminderScheduler.cancel(appContext, id)
                }
                .onFailure { showError() }
        }
    }

    fun restoreTodo(todo: TodoItem) {
        if (_todos.value.any { it.id == todo.id }) return
        viewModelScope.launch {
            runCatching {
                val created = repository.addTodo(todo)
                if (todo.isCompleted) repository.updateTodo(created.copy(isCompleted = true)) else created
            }.onSuccess { restored ->
                _todos.value = _todos.value + restored
                ReminderScheduler.schedule(appContext, restored)
            }.onFailure { showError() }
        }
    }

    /** Called when a photo is picked or taken for a task that already exists. */
    fun onImagePicked(id: String, uri: Uri) {
        viewModelScope.launch { uploadImage(id, uri) }
    }

    fun removeImage(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteImage(id) }
                .onSuccess { updated ->
                    _todos.value = _todos.value.map { if (it.id == id) updated else it }
                }
                .onFailure { showError() }
        }
    }

    private suspend fun uploadImage(id: String, uri: Uri) {
        runCatching {
            val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
            repository.uploadImage(id, bytes)
        }.onSuccess { updated ->
            _todos.value = _todos.value.map { if (it.id == id) updated else it }
        }.onFailure { showError() }
    }

    /**
     * Shrinks the photo to at most 800px, fixes sideways camera photos using the
     * EXIF rotation tag, and returns it as JPEG bytes ready to upload.
     */
    private fun compressImage(uri: Uri): ByteArray {
        val resolver = appContext.contentResolver
        val maxDimension = 800

        // 1. Read only the image size, so a big camera photo isn't fully loaded into memory.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        // 2. Decode a reduced-size version.
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not read the selected image")

        // 3. Work out how much the photo needs to be rotated.
        val rotation = resolver.openInputStream(uri)?.use { stream ->
            val orientation = ExifInterface(stream)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        // 4. Scale down to the final size and apply the rotation.
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        return ByteArrayOutputStream().use { output ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        }
    }

    private fun showError() {
        _errorMessage.value = "Couldn't reach the server. Check your PC and Wi-Fi."
    }
}