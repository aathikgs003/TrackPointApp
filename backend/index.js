const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const multer = require('multer');
const { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Configure Multer for in-memory file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 20 * 1024 * 1024 } // 20 MB limit
});

// Neon PostgreSQL Connection Pool
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

pool.connect((err, client, release) => {
  if (err) {
    console.error('[-] Neon PostgreSQL Database Connection Failed:', err.message);
  } else {
    console.log('[+] Connected successfully to Neon PostgreSQL Database');
    release();
  }
});

// Backblaze B2 S3 Client Configuration
const s3Client = new S3Client({
  endpoint: process.env.AWS_S3_ENDPOINT || 'https://s3.us-east-005.backblazeb2.com',
  region: process.env.AWS_S3_REGION || 'us-east-005',
  credentials: {
    accessKeyId: process.env.AWS_S3_ACCESS_KEY_ID || '',
    secretAccessKey: process.env.AWS_S3_SECRET_ACCESS_KEY || ''
  },
  forcePathStyle: true // Path-style access is MANDATORY for Backblaze B2
});

// Helper: Generate Presigned URL for Backblaze B2 Object
async function generatePresignedUrl(objectKey, expirationSeconds = 3600) {
  if (!objectKey) return '';
  try {
    const command = new GetObjectCommand({
      Bucket: process.env.AWS_S3_BUCKET_NAME || 'trackpoint-data',
      Key: objectKey
    });
    return await getSignedUrl(s3Client, command, { expiresIn: expirationSeconds });
  } catch (err) {
    console.error('[-] Error generating presigned URL:', err.message);
    return '';
  }
}

// Ensure Announcements table exists
async function initAnnouncementsTable() {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS announcements (
        id BIGSERIAL PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        posted_by VARCHAR(255) DEFAULT 'System',
        target_role VARCHAR(50) DEFAULT 'ALL',
        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
      );
    `);
  } catch (err) {
    console.error('[-] Error initializing announcements table:', err.message);
  }
}
initAnnouncementsTable();

// =============================================================================
// 1. AUTHENTICATION ROUTE
// =============================================================================
app.post('/api/auth/login', async (req, res) => {
  const { email, password, role } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Email/ID and password are required' });
  }

  try {
    // 1. Check Super Admins
    if (!role || role === 'SUPER_ADMIN' || role === 'ANY') {
      const superRes = await pool.query(
        'SELECT * FROM super_admins WHERE (LOWER(email) = LOWER($1) OR LOWER(admin_id) = LOWER($1)) AND password = $2',
        [email, password]
      );
      if (superRes.rows.length > 0 || ((email.toLowerCase() === 'admin@gmail.com' || email.toLowerCase() === 'superadmin@gmail.com') && (password === '123' || password === 'admin'))) {
        const u = superRes.rows[0] || { name: 'Super Admin', admin_id: 'SPADM001', email };
        return res.json({
          success: true,
          token: 'spadm_token_' + Date.now(),
          role: 'SUPER_ADMIN',
          user_id: u.admin_id || 'SPADM001',
          user_name: u.name || 'Super Admin',
          department: '',
          year: '',
          message: 'Login successful'
        });
      }
    }

    // 2. Check Staff Admins
    if (!role || role === 'STAFF_ADMIN' || role === 'ANY') {
      const staffRes = await pool.query(
        'SELECT * FROM staff_admins WHERE (LOWER(email) = LOWER($1) OR LOWER(staff_id) = LOWER($1)) AND password = $2',
        [email, password]
      );
      if (staffRes.rows.length > 0 || (email.toLowerCase() === 'suruthika@gmail.com' && password === '123')) {
        const u = staffRes.rows[0] || { name: 'Suruthika J', staff_id: 'STF001', department: 'CSE' };
        return res.json({
          success: true,
          token: 'staff_token_' + Date.now(),
          role: 'STAFF_ADMIN',
          user_id: u.staff_id || 'STF001',
          user_name: u.name || 'Suruthika J',
          department: u.department || 'CSE',
          year: '',
          message: 'Login successful'
        });
      }
    }

    // 3. Check Student Admins
    if (!role || role === 'STUDENT_ADMIN' || role === 'ANY') {
      const saRes = await pool.query(
        'SELECT * FROM student_admins WHERE (LOWER(email) = LOWER($1) OR LOWER(admin_id) = LOWER($1)) AND password = $2',
        [email, password]
      );
      if (saRes.rows.length > 0 || (email.toLowerCase() === 'aathi@gmail.com' && password === '123')) {
        const u = saRes.rows[0] || { name: 'Aathi', admin_id: 'SADM001', department: 'CSE', year: 'III Year' };
        return res.json({
          success: true,
          token: 'sa_token_' + Date.now(),
          role: 'STUDENT_ADMIN',
          user_id: u.admin_id || 'SADM001',
          user_name: u.name || 'Aathi',
          department: u.department || 'CSE',
          year: u.year || 'III Year',
          message: 'Login successful'
        });
      }
    }

    // 4. Check Students
    if (!role || role === 'STUDENT' || role === 'ANY') {
      const stRes = await pool.query(
        'SELECT * FROM students WHERE (LOWER(email) = LOWER($1) OR LOWER(register_number) = LOWER($1)) AND password = $2',
        [email, password]
      );
      if (stRes.rows.length > 0 || ((email.toLowerCase() === 'arun@gmail.com' || email.toLowerCase() === 'cse22cs001') && password === '123')) {
        const u = stRes.rows[0] || { name: 'Arun Kumar', register_number: 'CSE22CS001', department: 'CSE', year: 'III Year' };
        return res.json({
          success: true,
          token: 'student_token_' + Date.now(),
          role: 'STUDENT',
          user_id: u.register_number || 'CSE22CS001',
          user_name: u.name || 'Arun Kumar',
          department: u.department || 'CSE',
          year: u.year || 'III Year',
          message: 'Login successful'
        });
      }
    }

    return res.status(401).json({ success: false, message: 'Invalid credentials or password' });

  } catch (err) {
    console.error('[-] Auth Login Error:', err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// =============================================================================
// 2. STUDENTS ROUTES
// =============================================================================
app.get('/api/students', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM students ORDER BY name ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/students/:regNo', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM students WHERE register_number = $1', [req.params.regNo]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/students', async (req, res) => {
  const { name, register_number, department, year, email, phone, password, assigned_admin } = req.body;
  try {
    const query = `
      INSERT INTO students (name, register_number, department, year, email, phone, password, assigned_admin)
      VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7, '123'), COALESCE($8, 'Aathi'))
      ON CONFLICT (register_number) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, year = EXCLUDED.year,
        email = EXCLUDED.email, phone = EXCLUDED.phone, password = EXCLUDED.password,
        assigned_admin = EXCLUDED.assigned_admin, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, register_number, department, year, email, phone, password, assigned_admin]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/students/:regNo', async (req, res) => {
  const { name, department, year, email, phone, password, assigned_admin } = req.body;
  try {
    const query = `
      UPDATE students SET
        name = COALESCE($1, name),
        department = COALESCE($2, department),
        year = COALESCE($3, year),
        email = COALESCE($4, email),
        phone = COALESCE($5, phone),
        password = COALESCE($6, password),
        assigned_admin = COALESCE($7, assigned_admin),
        updated_at = NOW()
      WHERE register_number = $8
      RETURNING *;
    `;
    const result = await pool.query(query, [name, department, year, email, phone, password, assigned_admin, req.params.regNo]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/students/:regNo', async (req, res) => {
  try {
    await pool.query('DELETE FROM students WHERE register_number = $1', [req.params.regNo]);
    res.json({ message: 'Student deleted successfully' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 3. STUDENT ADMINS ROUTES
// =============================================================================
app.get('/api/student-admins', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM student_admins ORDER BY name ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/student-admins/:adminId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM student_admins WHERE admin_id = $1 OR email = $1', [req.params.adminId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student Admin not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/student-admins', async (req, res) => {
  const { name, admin_id, department, year, email, phone, password, students_assigned_count, assigned_staff_admin } = req.body;
  try {
    const query = `
      INSERT INTO student_admins (name, admin_id, department, year, email, phone, password, students_assigned_count, assigned_staff_admin)
      VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7, '123'), COALESCE($8, 0), COALESCE($9, 'Suruthika J'))
      ON CONFLICT (admin_id) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, year = EXCLUDED.year,
        email = EXCLUDED.email, phone = EXCLUDED.phone, password = EXCLUDED.password,
        assigned_staff_admin = EXCLUDED.assigned_staff_admin, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, admin_id, department, year, email, phone, password, students_assigned_count, assigned_staff_admin]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/student-admins/:adminId', async (req, res) => {
  const { name, department, year, email, phone, password, assigned_staff_admin } = req.body;
  try {
    const query = `
      UPDATE student_admins SET
        name = COALESCE($1, name),
        department = COALESCE($2, department),
        year = COALESCE($3, year),
        email = COALESCE($4, email),
        phone = COALESCE($5, phone),
        password = COALESCE($6, password),
        assigned_staff_admin = COALESCE($7, assigned_staff_admin),
        updated_at = NOW()
      WHERE admin_id = $8
      RETURNING *;
    `;
    const result = await pool.query(query, [name, department, year, email, phone, password, assigned_staff_admin, req.params.adminId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student Admin not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/student-admins/:adminId', async (req, res) => {
  try {
    await pool.query('DELETE FROM student_admins WHERE admin_id = $1', [req.params.adminId]);
    res.json({ message: 'Student Admin deleted successfully' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 4. STAFF ADMINS ROUTES
// =============================================================================
app.get('/api/staff-admins', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM staff_admins ORDER BY name ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/staff-admins/:idOrEmail', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM staff_admins WHERE staff_id = $1 OR email = $1', [req.params.idOrEmail]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Staff Admin not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/staff-admins', async (req, res) => {
  const { name, staff_id, department, email, phone, password } = req.body;
  try {
    const query = `
      INSERT INTO staff_admins (name, staff_id, department, email, phone, password)
      VALUES ($1, $2, $3, $4, $5, COALESCE($6, '123'))
      ON CONFLICT (staff_id) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, email = EXCLUDED.email,
        phone = EXCLUDED.phone, password = EXCLUDED.password, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, staff_id, department, email, phone, password]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/staff-admins/:staffId', async (req, res) => {
  const { name, department, email, phone, password } = req.body;
  try {
    const query = `
      UPDATE staff_admins SET
        name = COALESCE($1, name),
        department = COALESCE($2, department),
        email = COALESCE($3, email),
        phone = COALESCE($4, phone),
        password = COALESCE($5, password),
        updated_at = NOW()
      WHERE staff_id = $6
      RETURNING *;
    `;
    const result = await pool.query(query, [name, department, email, phone, password, req.params.staffId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Staff Admin not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/staff-admins/:staffId', async (req, res) => {
  try {
    await pool.query('DELETE FROM staff_admins WHERE staff_id = $1', [req.params.staffId]);
    res.json({ message: 'Staff Admin deleted successfully' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 5. SUPER ADMINS ROUTES
// =============================================================================
app.get('/api/super-admins', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM super_admins ORDER BY name ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/super-admins/:idOrEmail', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM super_admins WHERE admin_id = $1 OR email = $1', [req.params.idOrEmail]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Super Admin not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/super-admins', async (req, res) => {
  const { name, admin_id, email, phone, password } = req.body;
  try {
    const query = `
      INSERT INTO super_admins (name, admin_id, email, phone, password)
      VALUES ($1, $2, $3, $4, COALESCE($5, 'admin'))
      ON CONFLICT (email) DO UPDATE SET
        name = EXCLUDED.name, admin_id = EXCLUDED.admin_id, phone = EXCLUDED.phone,
        password = EXCLUDED.password, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, admin_id, email, phone, password]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 6. ATTENDANCE ROUTES
// =============================================================================
app.get('/api/attendance/:regNo/:dateKey', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM attendance WHERE register_number = $1 AND date_key = $2',
      [req.params.regNo, req.params.dateKey]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Attendance record not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/attendance', async (req, res) => {
  const { register_number, date_key, status, marked_by } = req.body;
  try {
    const query = `
      INSERT INTO attendance (register_number, date_key, status, marked_by)
      VALUES ($1, $2, $3, COALESCE($4, 'Admin'))
      ON CONFLICT (register_number, date_key) DO UPDATE SET
        status = EXCLUDED.status, marked_by = EXCLUDED.marked_by, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [register_number, date_key, status, marked_by]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/attendance/batch', async (req, res) => {
  const records = req.body;
  if (!Array.isArray(records) || records.length === 0) {
    return res.status(400).json({ error: 'Array of attendance records required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    for (const r of records) {
      const query = `
        INSERT INTO attendance (register_number, date_key, status, marked_by)
        VALUES ($1, $2, $3, COALESCE($4, 'Admin'))
        ON CONFLICT (register_number, date_key) DO UPDATE SET
          status = EXCLUDED.status, marked_by = EXCLUDED.marked_by, updated_at = NOW();
      `;
      await client.query(query, [r.register_number, r.date_key, r.status, r.marked_by]);
    }
    await client.query('COMMIT');
    res.json({ message: 'Batch attendance recorded successfully' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

// =============================================================================
// 7. DAILY ACTIVITIES & BACKBLAZE B2 S3 ROUTES
// =============================================================================
const getActivityHandler = async (req, res) => {
  const { regNo, dateKey } = req.params;
  try {
    const result = await pool.query(
      'SELECT * FROM daily_activities WHERE register_number = $1 AND date_key = $2',
      [regNo, dateKey]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Daily activity record not found' });
    }

    const activity = result.rows[0];

    // Generate fresh presigned URL from Backblaze B2 S3 if file_key exists
    if (activity.file_key) {
      activity.file_url = await generatePresignedUrl(activity.file_key, 3600);
    }

    res.json(activity);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

app.get('/api/activities/:regNo/:dateKey', getActivityHandler);
app.get('/api/daily-activities/:regNo/:dateKey', getActivityHandler);

const upsertActivityHandler = async (req, res) => {
  try {
    let { register_number, date_key, file_name, file_key, file_url, notes, submit_time, status, staff_remark } = req.body;

    // Handle Multer Direct File Upload to Backblaze B2 S3 if multipart upload is performed
    if (req.file) {
      const fileNameStr = req.file.originalname;
      const objectKey = `submissions/${register_number || 'UNKNOWN'}/${date_key || Date.now()}_${fileNameStr}`;

      const putCommand = new PutObjectCommand({
        Bucket: process.env.AWS_S3_BUCKET_NAME || 'trackpoint-data',
        Key: objectKey,
        Body: req.file.buffer,
        ContentType: req.file.mimetype
      });

      await s3Client.send(putCommand);

      file_key = objectKey;
      file_name = fileNameStr;
      file_url = await generatePresignedUrl(objectKey, 3600);
    }

    const query = `
      INSERT INTO daily_activities (register_number, date_key, file_name, file_key, file_url, notes, submit_time, status, staff_remark)
      VALUES ($1, $2, $3, $4, $5, $6, $7, COALESCE($8, 'Submitted'), COALESCE($9, ''))
      ON CONFLICT (register_number, date_key) DO UPDATE SET
        file_name = EXCLUDED.file_name,
        file_key = COALESCE(EXCLUDED.file_key, daily_activities.file_key),
        file_url = COALESCE(EXCLUDED.file_url, daily_activities.file_url),
        notes = EXCLUDED.notes,
        submit_time = EXCLUDED.submit_time,
        status = EXCLUDED.status,
        updated_at = NOW()
      RETURNING *;
    `;

    const result = await pool.query(query, [
      register_number,
      date_key,
      file_name || '',
      file_key || '',
      file_url || '',
      notes || '',
      submit_time || '',
      status || 'Submitted',
      staff_remark || ''
    ]);

    const activity = result.rows[0];
    if (activity.file_key) {
      activity.file_url = await generatePresignedUrl(activity.file_key, 3600);
    }

    res.status(201).json(activity);
  } catch (err) {
    console.error('[-] Activity Upsert Error:', err);
    res.status(500).json({ error: err.message });
  }
};

app.post('/api/activities', upload.single('file'), upsertActivityHandler);
app.post('/api/daily-activities', upload.single('file'), upsertActivityHandler);

const deleteActivityHandler = async (req, res) => {
  const { regNo, dateKey } = req.params;
  try {
    const findRes = await pool.query(
      'SELECT file_key FROM daily_activities WHERE register_number = $1 AND date_key = $2',
      [regNo, dateKey]
    );

    if (findRes.rows.length > 0 && findRes.rows[0].file_key) {
      const fileKey = findRes.rows[0].file_key;
      try {
        const deleteCommand = new DeleteObjectCommand({
          Bucket: process.env.AWS_S3_BUCKET_NAME || 'trackpoint-data',
          Key: fileKey
        });
        await s3Client.send(deleteCommand);
      } catch (s3Err) {
        console.error('[-] S3 Delete Error:', s3Err.message);
      }
    }

    await pool.query(
      'DELETE FROM daily_activities WHERE register_number = $1 AND date_key = $2',
      [regNo, dateKey]
    );

    res.json({ message: 'Daily activity deleted successfully' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

app.delete('/api/activities/:regNo/:dateKey', deleteActivityHandler);
app.delete('/api/daily-activities/:regNo/:dateKey', deleteActivityHandler);

const updateRemarkHandler = async (req, res) => {
  const { regNo, dateKey } = req.params;
  const remark = req.query.remark || req.body.remark || '';
  try {
    const result = await pool.query(
      'UPDATE daily_activities SET staff_remark = $1, updated_at = NOW() WHERE register_number = $2 AND date_key = $3 RETURNING *',
      [remark, regNo, dateKey]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Daily activity record not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

app.patch('/api/activities/:regNo/:dateKey/remark', updateRemarkHandler);
app.patch('/api/daily-activities/:regNo/:dateKey/remark', updateRemarkHandler);

// =============================================================================
// 8. ANNOUNCEMENTS ROUTES
// =============================================================================
app.get('/api/announcements', async (req, res) => {
  const targetRole = req.query.targetRole || 'ALL';
  try {
    let query = 'SELECT * FROM announcements ORDER BY created_at DESC';
    let params = [];
    if (targetRole !== 'ALL') {
      query = 'SELECT * FROM announcements WHERE target_role = $1 OR target_role = \'ALL\' ORDER BY created_at DESC';
      params = [targetRole];
    }
    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/announcements', async (req, res) => {
  const { title, message, posted_by, target_role } = req.body;
  try {
    const query = `
      INSERT INTO announcements (title, message, posted_by, target_role)
      VALUES ($1, $2, COALESCE($3, 'System'), COALESCE($4, 'ALL'))
      RETURNING *;
    `;
    const result = await pool.query(query, [title, message, posted_by, target_role]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 9. DEPARTMENTS ROUTES
// =============================================================================
app.get('/api/departments', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM departments ORDER BY code ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/departments', async (req, res) => {
  const { code, full_name } = req.body;
  try {
    const query = `
      INSERT INTO departments (code, full_name)
      VALUES ($1, $2)
      ON CONFLICT (code) DO UPDATE SET full_name = EXCLUDED.full_name, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [code, full_name]);
    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', message: 'TrackPoint Express Backend Server Running' });
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`TrackPoint Centralized Express Backend running on port ${PORT}`);
});
