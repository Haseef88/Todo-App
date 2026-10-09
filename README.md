# Todo App

An Android to-do app built with Kotlin and Jetpack Compose. The app talks to a small Node.js server, which stores everything in an H2 database. Built by Team stylecue as a course exercise.

## Features
- Add, edit and delete tasks, with an Undo bar after a delete
- Mark tasks as done (done tasks move to the bottom)
- Search tasks by title as you type
- Optional deadline, picked from a calendar
- Reminder time chosen per task (default 9:00 AM): the phone rings with the alarm sound and shows a "Task due today" notification
- Photos on tasks: take one with the camera or choose one from the gallery, add it while creating a task or later, view it full-size, replace it or remove it
- Tasks and photos are stored on the server, so they stay after the app is closed
- Light and dark theme

## How it works
```
Phone app  --HTTP (Retrofit)-->  Node.js / Express API  --Postgres protocol-->  H2 database (file)
```

### Android app (`app/src/main/java/com/yourteam/todoapp`)
| File | What it does |
|---|---|
| `MainActivity.kt` | Connects the ViewModel to the screen and asks for notification permission |
| `ui/TodoScreen.kt` | The whole UI: search, add form, task list, edit dialog, photo menu, full-size photo |
| `ui/TodoViewModel.kt` | Holds the app state and calls the server; also shrinks photos before upload |
| `ui/theme/Theme.kt` | Colours for light and dark mode |
| `data/TodoItem.kt` | The task model |
| `data/TodoApi.kt` | The server endpoints (Retrofit) |
| `data/TodoRepository.kt` | Wraps the API calls |
| `data/NetworkConfig.kt` | The server address |
| `data/ReminderScheduler.kt` | Schedules the reminders (WorkManager) and rings the alarm |

### Backend (`backend/`)
`server.js` is an Express server that uses the `pg` package to talk to H2. H2 is run in its PostgreSQL-protocol mode, so the normal Postgres client works.

| Endpoint | What it does |
|---|---|
| `GET /todos` | List all tasks (without photo bytes) |
| `POST /todos` | Add a task |
| `PUT /todos/:id` | Update a task |
| `DELETE /todos/:id` | Delete a task |
| `PUT /todos/:id/image` | Upload or replace the photo (raw bytes in the request body) |
| `DELETE /todos/:id/image` | Remove the photo |
| `GET /todos/:id/image` | Download the photo |

Table `todos`: `id`, `title`, `is_completed`, `deadline_millis`, `reminder_minutes`, `image` (BYTEA, the photo itself), `image_type`, `image_version` (goes up each time the photo changes, so the app never shows an old cached picture).

## Running it
You need Android Studio, Node.js, Java (JDK 17 or newer), the [H2 database](https://www.h2database.com) jar, and a phone on the same Wi-Fi as your PC.

1. **Start H2** (leave the window open):
```
   java -cp h2-2.5.252.jar org.h2.tools.Server -pg -pgPort 5435 -baseDir "<path to backend>\data" -ifNotExists
```
2. **Start the API** (in a second window):
```
   cd backend
   npm install
   node server.js
```
It should print `API on http://localhost:3000`. Test it at `http://localhost:3000/todos`.
3. **Set the server address.** Find your PC's IPv4 address with `ipconfig`, then put it in `data/NetworkConfig.kt`, for example `http://192.168.8.158:3000/`.
4. **Run the app** from Android Studio on your phone.

## Notes and limitations
- The app needs the server. There is no offline mode, and it shows "Couldn't reach the server" if the PC or Wi-Fi is down.
- The app uses plain `http` for development, so it only works on the local network.
- Reminders use WorkManager, so they can arrive a minute or two late.
- If your PC's IP address changes after a restart, update `NetworkConfig.kt`.
- Avoid keeping the Android project inside OneDrive. It can lock build files and make Gradle fail.

## Team
Team stylecue