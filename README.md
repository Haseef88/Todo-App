# Todo App

A simple Android to-do list app built with Jetpack Compose, created by Team stylecue.

## Features
- Add, edit, and delete tasks
- Mark tasks as done
- Optional deadlines per task
- Search/filter tasks by title
- Tasks persist across app restarts (saved on-device)
- Tasks are automatically sorted: pending tasks first (with deadlines prioritized), completed tasks at the bottom

## Architecture
The app follows a simple MVVM (Model-View-ViewModel) pattern:

- **`data/TodoItem.kt`** — the data model for a single task (id, title, completion state, optional deadline).
- **`data/TodoRepository.kt`** — saves/loads the task list using Jetpack DataStore. Tasks are serialized to JSON and stored in a small key-value store on the device, so they survive app restarts and the process being killed.
- **`ui/TodoViewModel.kt`** — holds app state (`StateFlow`s for the task list and search query), exposes actions (`addTodo`, `updateTodo`, `toggleTodo`, `deleteTodo`, `updateSearchQuery`), and persists every change through the repository.
- **`ui/TodoScreen.kt`** — the Compose UI: search bar, add-task form, task list, and an edit dialog. Purely presentational — it receives state and calls back up to the ViewModel via lambdas.
- **`ui/theme/Theme.kt`** — the app's color scheme (light/dark).
- **`MainActivity.kt`** — wires the ViewModel to the UI.

## Tech used
- Kotlin
- Jetpack Compose (Material 3)
- Kotlin Coroutines & Flow
- Jetpack DataStore (Preferences) for local persistence

## Team
Team 1 — stylecue