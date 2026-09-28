# Todo App

A simple Android to-do list app built with Jetpack Compose, created by Team stylecue.

## Features
- Add, edit, and delete tasks
- Undo a delete straight after it happens
- Mark tasks as done
- Optional deadline per task, chosen with a calendar date picker
- Alarm on the deadline day at a time you choose (default 9:00 AM): the phone rings with the alarm sound until you tap the notification
- Search/filter tasks by title
- Tasks persist across app restarts (saved on the device)
- Automatic sorting: pending tasks first (soonest deadline first), then tasks without a deadline, with completed tasks at the bottom
- Light and dark theme

## How reminders work
When a task has a deadline, the app schedules a background job with WorkManager for the chosen reminder time on that date. When the time arrives, a notification "Task due today" is shown, and tapping it opens the app. The reminder is cancelled or rescheduled automatically when a task is edited, completed, or deleted, and restored if a delete is undone. Reminders are approximate and may arrive a few minutes late because Android batches background work to save battery. On Android 13 and newer the app asks for notification permission the first time it opens.

## Architecture
The app follows a simple MVVM (Model-View-ViewModel) pattern:

- data/TodoItem.kt: the data model for a single task (id, title, completion state, optional deadline date, reminder time).
- data/TodoRepository.kt: saves and loads the task list using Jetpack DataStore. Tasks are stored as JSON on the device, so they survive app restarts and the process being killed.
- data/ReminderScheduler.kt: schedules, cancels, and shows reminder notifications using WorkManager.
- ui/TodoViewModel.kt: holds app state (StateFlows for the task list and search query), exposes actions (add, update, toggle, delete, restore, search), saves every change through the repository, and keeps reminders in sync.
- ui/TodoScreen.kt: the Jetpack Compose UI: search bar, add-task form with date and time pickers, task list, edit dialog, and undo bar. It only receives state and calls back to the ViewModel through lambdas.
- ui/theme/Theme.kt: the app's colour scheme (light and dark).
- MainActivity.kt: connects the ViewModel to the UI and requests notification permission.

## Tech used
- Kotlin
- Jetpack Compose (Material 3)
- Kotlin Coroutines and Flow
- Jetpack DataStore (Preferences) for local storage
- WorkManager and notifications for reminders

## Team
Team 1: stylecue