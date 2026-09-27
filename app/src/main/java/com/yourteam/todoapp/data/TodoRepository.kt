package com.yourteam.todoapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.todoDataStore by preferencesDataStore(name = "todos")

class TodoRepository(private val context: Context) {

    private val todosKey = stringPreferencesKey("todos_json")

    val todosFlow: Flow<List<TodoItem>> = context.todoDataStore.data.map { preferences ->
        val json = preferences[todosKey] ?: "[]"
        parseTodos(json)
    }

    suspend fun saveTodos(todos: List<TodoItem>) {
        context.todoDataStore.edit { preferences ->
            preferences[todosKey] = toJson(todos)
        }
    }

    private fun toJson(todos: List<TodoItem>): String {
        val array = JSONArray()
        todos.forEach { todo ->
            val obj = JSONObject()
            obj.put("id", todo.id)
            obj.put("title", todo.title)
            obj.put("isCompleted", todo.isCompleted)
            obj.put("deadline", todo.deadline)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseTodos(json: String): List<TodoItem> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            TodoItem(
                id = obj.getString("id"),
                title = obj.getString("title"),
                isCompleted = obj.getBoolean("isCompleted"),
                deadline = if (obj.isNull("deadline")) null else obj.getString("deadline"),
            )
        }
    }
}