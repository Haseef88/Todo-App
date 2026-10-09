const express = require('express');
const { Pool } = require('pg');

const app = express();
app.use(express.json({ limit: '10mb' }));

const pool = new Pool({
  host: 'localhost',
  port: 5435,
  user: 'sa',
  password: '',
  database: 'tododb',
});

// Lets async routes report errors instead of crashing the whole server.
const wrap = (fn) => (req, res, next) => fn(req, res, next).catch(next);

async function ensureTable() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS todos (
      id VARCHAR(64) PRIMARY KEY,
      title VARCHAR(500) NOT NULL,
      is_completed BOOLEAN NOT NULL DEFAULT FALSE,
      deadline_millis BIGINT,
      reminder_minutes INT,
      image BYTEA
    )
  `);
  await pool.query(`ALTER TABLE todos ADD COLUMN IF NOT EXISTS image_type VARCHAR(50)`);
  // Goes up by 1 every time the photo is replaced or removed, so the app can tell the picture changed.
  await pool.query(`ALTER TABLE todos ADD COLUMN IF NOT EXISTS image_version INT DEFAULT 0`);
}

// Everything about a todo EXCEPT the image bytes (too heavy to send around).
const TODO_COLUMNS = `id, title, is_completed, deadline_millis, reminder_minutes, image_version,
                      (image IS NOT NULL) AS has_image`;

function toJson(row) {
  return {
    id: row.id,
    title: row.title,
    isCompleted: row.is_completed,
    deadlineMillis: row.deadline_millis !== null ? Number(row.deadline_millis) : null,
    reminderMinutes: row.reminder_minutes,
    hasImage: Boolean(row.has_image),
    imageVersion: row.image_version !== null && row.image_version !== undefined ? Number(row.image_version) : 0,
  };
}

async function fetchTodo(id) {
  const result = await pool.query(`SELECT ${TODO_COLUMNS} FROM todos WHERE id = $1`, [id]);
  return result.rows[0];
}

// list all todos
app.get('/todos', wrap(async (req, res) => {
  const result = await pool.query(`SELECT ${TODO_COLUMNS} FROM todos ORDER BY id`);
  res.json(result.rows.map(toJson));
}));

// add a todo
app.post('/todos', wrap(async (req, res) => {
  const { id, title, deadlineMillis, reminderMinutes } = req.body;
  if (!id || !title) {
    return res.status(400).json({ error: 'id and title are required' });
  }
  await pool.query(
    `INSERT INTO todos (id, title, is_completed, deadline_millis, reminder_minutes)
     VALUES ($1, $2, FALSE, $3, $4)`,
    [id, title, deadlineMillis ?? null, reminderMinutes ?? null],
  );
  res.status(201).json(toJson(await fetchTodo(id)));
}));

// update a todo (title, done, deadline, reminder time)
app.put('/todos/:id', wrap(async (req, res) => {
  const { title, isCompleted, deadlineMillis, reminderMinutes } = req.body;
  const result = await pool.query(
    `UPDATE todos
     SET title = COALESCE($1, title),
         is_completed = COALESCE($2, is_completed),
         deadline_millis = $3,
         reminder_minutes = COALESCE($4, reminder_minutes)
     WHERE id = $5`,
    [title ?? null, isCompleted ?? null, deadlineMillis ?? null, reminderMinutes ?? null, req.params.id],
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  res.json(toJson(await fetchTodo(req.params.id)));
}));

// delete a todo
app.delete('/todos/:id', wrap(async (req, res) => {
  const result = await pool.query('DELETE FROM todos WHERE id = $1', [req.params.id]);
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  res.status(204).end();
}));

// upload or replace a todo's image (raw binary body), and send back the updated todo
app.put('/todos/:id/image', express.raw({ type: '*/*', limit: '10mb' }), wrap(async (req, res) => {
  if (!Buffer.isBuffer(req.body) || req.body.length === 0) {
    return res.status(400).json({ error: 'expected raw image bytes in the request body' });
  }
  const contentType = req.headers['content-type'] || 'image/jpeg';
  const result = await pool.query(
    `UPDATE todos
     SET image = $1, image_type = $2, image_version = COALESCE(image_version, 0) + 1
     WHERE id = $3`,
    [req.body, contentType, req.params.id],
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  res.json(toJson(await fetchTodo(req.params.id)));
}));

// remove a todo's image, and send back the updated todo
app.delete('/todos/:id/image', wrap(async (req, res) => {
  const result = await pool.query(
    `UPDATE todos
     SET image = NULL, image_type = NULL, image_version = COALESCE(image_version, 0) + 1
     WHERE id = $1`,
    [req.params.id],
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  res.json(toJson(await fetchTodo(req.params.id)));
}));

// fetch a todo's image as raw bytes
app.get('/todos/:id/image', wrap(async (req, res) => {
  const result = await pool.query('SELECT image, image_type FROM todos WHERE id = $1', [req.params.id]);
  if (result.rowCount === 0 || result.rows[0].image === null) {
    return res.status(404).end();
  }
  res.set('Content-Type', result.rows[0].image_type || 'image/jpeg');
  res.send(result.rows[0].image);
}));

// any error inside a route ends up here instead of crashing the server
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: 'server error' });
});

ensureTable()
  .then(() => {
    app.listen(3000, () => console.log('API on http://localhost:3000'));
  })
  .catch((err) => {
    console.error('Could not connect to H2. Is the H2 server running?', err.message);
    process.exit(1);
  });