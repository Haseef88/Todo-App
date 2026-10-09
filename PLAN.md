# Feature specs: Todo App (Team stylecue)

Short specs for each exercise, written from the finished app.

## 7.1 Basic todo application
**Goal:** add, list and delete todo items.
**Input:** a task title typed into the form. **Output:** the task appears in the list.
**Behaviour:**
- An empty title is ignored.
- Each task has a checkbox to mark it done (done tasks move to the bottom).
- A task can be edited, or deleted. After a delete, an Undo bar appears for a few seconds.
  **Done when:** a task added on the phone appears in the list, can be ticked, edited and deleted, and Undo brings a deleted task back.

## 7.2 Search a todo item
**Goal:** filter the list as the user types.
**Input:** text in the search box. **Output:** only tasks whose title contains that text.
**Behaviour:**
- The match ignores upper and lower case.
- An empty box shows every task, and an X button clears the search.
- If nothing matches, the screen says so.
  **Done when:** typing "milk" shows only tasks with "milk" in the title, and clearing the box shows the full list again.

## 7.3 Deadline and alarm
**Goal:** set a deadline on a task and ring an alarm when it is reached.
**Input:** a deadline date from a calendar picker, and a reminder time from a clock picker (default 9:00 AM).
**Output:** at that time on that date the phone rings with the alarm sound and shows a "Task due today" notification. Tapping it opens the app.
**Behaviour:**
- The reminder is scheduled with WorkManager, so it works after the app is closed.
- It is cancelled or rescheduled when the task is edited, completed or deleted, and restored if a delete is undone.
- Android 13 and newer asks for notification permission the first time the app opens.
- Tasks are sorted so the soonest deadline comes first.
  **Done when:** a task with today's date and a reminder time a few minutes ahead makes the phone ring after the app is closed. It can arrive a minute or two late, because Android batches background work.

## 7.4 Upload an image to a todo item
**Goal:** attach a photo to a task.
**Input:** a photo taken with the camera or chosen from the gallery, either while creating the task or later.
**Output:** a thumbnail on the task card. Tapping it opens the photo full-size, with Replace and Remove buttons.
**Behaviour:**
- Before upload the photo is shrunk to at most 800 px and saved as JPEG, and camera photos are rotated the right way up.
- The photo is sent to the server as raw bytes (a blob), not as JSON, and stored in the same database row as its task.
- The server keeps an image version number that goes up each time the photo changes, so the app never shows an old cached picture.
  **Done when:** a photo added to a task is still there after closing and reopening the app. Replacing it shows the new picture, and removing it brings back the "+" button.

## 7.5 Save todos through a Node.js API backed by H2
**Goal:** store the todos on a server instead of on the phone.
**Parts:** the Android app (Retrofit) calls a Node.js / Express server, which stores data in an H2 database file. H2 runs in its PostgreSQL-protocol mode, so the server uses the normal `pg` package.
**Table `todos`:** id, title, is_completed, deadline_millis, reminder_minutes, image (BYTEA), image_type, image_version.
**Endpoints:**
- GET /todos, POST /todos, PUT /todos/:id, DELETE /todos/:id
- PUT, DELETE and GET /todos/:id/image
  **Behaviour:** the list endpoint never sends photo bytes, only a hasImage flag. If the server can't be reached, the app shows "Couldn't reach the server".
  **Done when:** a task added on the phone appears at http://localhost:3000/todos on the PC, and it is still there after restarting both the app and the Node server.