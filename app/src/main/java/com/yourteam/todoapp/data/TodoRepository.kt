package com.yourteam.todoapp.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Talks to the Node/Express API (backed by H2). The server address lives in NetworkConfig. */
class TodoRepository {

    private val api: TodoApi = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TodoApi::class.java)

    suspend fun getTodos(): List<TodoItem> = api.getTodos()

    suspend fun addTodo(todo: TodoItem): TodoItem = api.addTodo(todo)

    suspend fun updateTodo(todo: TodoItem): TodoItem = api.updateTodo(todo.id, todo)

    suspend fun deleteTodo(id: String) {
        api.deleteTodo(id)
    }

    suspend fun uploadImage(id: String, bytes: ByteArray): TodoItem {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        return api.uploadImage(id, body)
    }

    suspend fun deleteImage(id: String): TodoItem = api.deleteImage(id)
}