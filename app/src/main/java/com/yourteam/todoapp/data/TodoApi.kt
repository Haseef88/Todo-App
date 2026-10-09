package com.yourteam.todoapp.data

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TodoApi {
    @GET("todos")
    suspend fun getTodos(): List<TodoItem>

    @POST("todos")
    suspend fun addTodo(@Body todo: TodoItem): TodoItem

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: String, @Body todo: TodoItem): TodoItem

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String)

    @PUT("todos/{id}/image")
    suspend fun uploadImage(@Path("id") id: String, @Body image: RequestBody): TodoItem

    @DELETE("todos/{id}/image")
    suspend fun deleteImage(@Path("id") id: String): TodoItem
}