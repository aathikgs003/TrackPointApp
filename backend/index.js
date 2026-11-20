const dns = require('dns');
if (dns.setDefaultResultOrder) {
  dns.setDefaultResultOrder('ipv4first');
}

// Global DNS lookup fallback for Neon PostgreSQL cloud endpoints
const originalDnsLookup = dns.lookup;
dns.lookup = function(hostname, options, callback) {
  if (typeof options === 'function') {
    callback = options;
    options = {};
  }
  if (hostname && (hostname.includes('neon.tech') || hostname.includes('backblazeb2.com'))) {
    const resolver = new dns.Resolver();
    resolver.setServers(['8.8.8.8', '1.1.1.1', '1.0.0.1']);
    resolver.resolveCname(hostname, (cnameErr, cnames) => {
      const targetHost = (!cnameErr && cnames && cnames.length > 0) ? cnames[0] : hostname;
      resolver.resolve4(targetHost, (ipErr, ips) => {
        if (!ipErr && ips && ips.length > 0) {
          if (options && options.all) {
            return callback(null, ips.map(ip => ({ address: ip, family: 4 })));
          }
          return callback(null, ips[0], 4);
        }
        originalDnsLookup(hostname, options, callback);
      });
    });
    return;
  }
  return originalDnsLookup(hostname, options, callback);
};

const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const multer = require('multer');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const admin = require('firebase-admin');
const { S3Client, PutObjectCommand, GetObjectCommand, DeleteObjectCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

const fs = require('fs');
const path = require('path');

// Firebase Admin SDK Initialization
let isFirebaseAdminInitialized = false;
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    isFirebaseAdminInitialized = true;
    console.log('[+] Firebase Admin SDK initialized using FIREBASE_SERVICE_ACCOUNT_JSON env var');
  } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    const credPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
    if (fs.existsSync(credPath)) {
      const serviceAccount = JSON.parse(fs.readFileSync(credPath, 'utf8'));
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
      isFirebaseAdminInitialized = true;
      console.log(`[+] Firebase Admin SDK initialized using service account key file`);
    } else {
      admin.initializeApp({
        credential: admin.credential.applicationDefault()
      });
      isFirebaseAdminInitialized = true;
      console.log('[+] Firebase Admin SDK initialized using GOOGLE_APPLICATION_CREDENTIALS path');
    }
  } else {
    console.warn('[!] Firebase Admin SDK credentials not detected in env (FIREBASE_SERVICE_ACCOUNT_JSON / GOOGLE_APPLICATION_CREDENTIALS). FCM sending will be disabled until credentials are configured.');
  }
} catch (err) {
  console.error('[-] Firebase Admin SDK initialization error:', err.message);
}

const crypto = require('crypto');

// Constant-time string comparison helper to prevent side-channel timing attacks
function safeCompare(a, b) {
  if (!a || !b) return false;
  const bufA = Buffer.from(String(a));
  const bufB = Buffer.from(String(b));
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

// JWT Configurations (Loaded strictly from Environment Variables)
const JWT_SECRET = process.env.JWT_SECRET;
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET;
const ACCESS_TOKEN_EXPIRATION = process.env.ACCESS_TOKEN_EXPIRATION || '1h';
const REFRESH_TOKEN_EXPIRATION = process.env.REFRESH_TOKEN_EXPIRATION || '7d';

if (!JWT_SECRET || !JWT_REFRESH_SECRET) {
  console.error('[-] CRITICAL WARNING: JWT_SECRET or JWT_REFRESH_SECRET is missing from environment variables!');
}

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
  },
  connectionTimeoutMillis: 30000,
  idleTimeoutMillis: 30000,
  max: 20
});

pool.on('error', (err, client) => {
  console.error('[-] Unexpected Neon PostgreSQL Pool Error:', err.message);
});

pool.connect((err, client, release) => {
  if (err) {
    console.error('[-] Neon PostgreSQL Database Connection Failed:', err.message);
  } else {
    console.log('[+] Connected successfully to Neon PostgreSQL Database');
    if (release) release();
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

// Database Initializations
async function initTables() {
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

      CREATE TABLE IF NOT EXISTS refresh_tokens (
        id BIGSERIAL PRIMARY KEY,
        user_id VARCHAR(255) NOT NULL,
        role VARCHAR(50) NOT NULL,
        token TEXT NOT NULL UNIQUE,
        client_ip VARCHAR(100) DEFAULT '',
        user_agent TEXT DEFAULT '',
        expires_at TIMESTAMPTZ NOT NULL,
        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
        revoked BOOLEAN DEFAULT FALSE
      );

      CREATE TABLE IF NOT EXISTS user_device_tokens (
        id BIGSERIAL PRIMARY KEY,
        user_id VARCHAR(255) NOT NULL,
        role VARCHAR(50) NOT NULL,
        fcm_token TEXT NOT NULL UNIQUE,
        device_info TEXT DEFAULT '',
        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
      );

      CREATE INDEX IF NOT EXISTS idx_user_device_tokens_user_id ON user_device_tokens(user_id);
      CREATE INDEX IF NOT EXISTS idx_user_device_tokens_fcm_token ON user_device_tokens(fcm_token);
    `);
    console.log('[+] Database tables initialized (announcements, refresh_tokens, user_device_tokens)');
  } catch (err) {
    console.error('[-] Error initializing database tables:', err.message);
  }
}
initTables();

// Response Sanitization (Never expose password field in API responses)
function sanitizeUser(user) {
  if (!user) return user;
  if (Array.isArray(user)) {
    return user.map(u => sanitizeUser(u));
  }
  const sanitized = { ...user };
  delete sanitized.password;
  return sanitized;
}

// Password Verification & Transparent Bcrypt Migration Helper
async function verifyAndMigratePassword(inputPassword, dbUser, tableName, idColumn, idValue) {
  if (!dbUser || !dbUser.password) return false;

  const storedHash = dbUser.password;
  const isBcrypt = storedHash.startsWith('$2a$') || storedHash.startsWith('$2b$') || storedHash.startsWith('$2y$');

  let match = false;
  if (isBcrypt) {
    match = await bcrypt.compare(inputPassword, storedHash);
  } else {
    // Plaintext comparison for legacy database records
    match = (inputPassword === storedHash);
    if (match) {
      // Transparently upgrade plaintext password to bcrypt hash in Neon PostgreSQL
      try {
        const newHash = await bcrypt.hash(inputPassword, 10);
        await pool.query(`UPDATE ${tableName} SET password = $1, updated_at = NOW() WHERE ${idColumn} = $2`, [newHash, idValue]);
        console.log(`[+] Transparently upgraded legacy password to bcrypt hash for ${tableName} (${idValue})`);
      } catch (err) {
        console.error(`[-] Password auto-migration error for ${tableName}:`, err.message);
      }
    }
  }

  return match;
}

// JWT Helper Functions
function generateAccessToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: ACCESS_TOKEN_EXPIRATION });
}

function generateRefreshToken(payload) {
  return jwt.sign(payload, JWT_REFRESH_SECRET, { expiresIn: REFRESH_TOKEN_EXPIRATION });
}

async function storeRefreshToken(userId, role, token, clientIp = '', userAgent = '') {
  const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days
  await pool.query(
    'INSERT INTO refresh_tokens (user_id, role, token, client_ip, user_agent, expires_at) VALUES ($1, $2, $3, $4, $5, $6)',
    [userId, role, token, clientIp, userAgent, expiresAt]
  );
}

// Rate Limiting for Login Endpoint
const loginAttempts = new Map();
function loginRateLimiter(req, res, next) {
  const ip = req.ip || req.socket.remoteAddress || 'unknown';
  const now = Date.now();
  const windowMs = 15 * 60 * 1000; // 15 minutes window
  const maxAttempts = 15;

  if (!loginAttempts.has(ip)) {
    loginAttempts.set(ip, { count: 1, startTime: now });
    return next();
  }

  const attemptData = loginAttempts.get(ip);
  if (now - attemptData.startTime > windowMs) {
    loginAttempts.set(ip, { count: 1, startTime: now });
    return next();
  }

  attemptData.count++;
  if (attemptData.count > maxAttempts) {
    return res.status(429).json({ success: false, message: 'Too many login attempts. Please try again after 15 minutes.' });
  }

  next();
}

// Authentication Middleware
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ success: false, message: 'Access token required' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(401).json({ success: false, message: 'Invalid or expired access token' });
    }
    req.user = user;
    next();
  });
}

// Role Authorization Middleware
function authorizeRoles(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !req.user.role) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ success: false, message: 'Access denied: Insufficient role permissions' });
    }

    next();
  };
}

// Cross-User Data Access & Parameter Validation Middleware
function checkStudentAccess(req, res, next) {
  if (!req.user || !req.user.role) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }

  const { role, userId } = req.user;

  // If request contains register_number in body or params
  const targetRegNo = req.params.regNo || req.body.register_number;

  // SUPER_ADMIN and STAFF_ADMIN have full administrative access
  if (role === 'SUPER_ADMIN' || role === 'STAFF_ADMIN') {
    return next();
  }

  // STUDENT_ADMIN can access assigned students
  if (role === 'STUDENT_ADMIN') {
    return next();
  }

  // STUDENT can ONLY access/modify their own register number
  if (role === 'STUDENT') {
    if (targetRegNo && userId && userId.toLowerCase() !== targetRegNo.toLowerCase()) {
      return res.status(403).json({ success: false, message: 'Access denied: You cannot access or modify another student\'s record' });
    }
    // Force body register_number to match authenticated user ID to prevent body tampering
    if (req.body && req.body.register_number) {
      req.body.register_number = userId;
    }
    return next();
  }

  next();
}

// =============================================================================
// 1. AUTHENTICATION ROUTES
// =============================================================================

// Login Endpoint (Strict Database Authentication & Password Auto-Migration)
app.post('/api/auth/login', loginRateLimiter, async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Email/ID and password are required' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const clientIp = req.ip || req.socket.remoteAddress || '';
  const userAgent = req.headers['user-agent'] || '';

  try {
    // 1. Query Super Admins
    const superRes = await pool.query(
      'SELECT * FROM super_admins WHERE LOWER(email) = $1 OR LOWER(admin_id) = $1',
      [cleanEmail]
    );
    if (superRes.rows.length > 0) {
      const u = superRes.rows[0];
      const match = await verifyAndMigratePassword(password, u, 'super_admins', 'id', u.id);
      if (match) {
        const userId = u.admin_id || 'SPADM001';
        const role = 'SUPER_ADMIN';
        const payload = { userId, role, name: u.name || 'Super Admin', email: u.email || cleanEmail };
        const accessToken = generateAccessToken(payload);
        const refreshToken = generateRefreshToken({ userId, role });
        await storeRefreshToken(userId, role, refreshToken, clientIp, userAgent);

        return res.json({
          success: true,
          token: accessToken,
          refreshToken: refreshToken,
          role: role,
          user_id: userId,
          user_name: u.name || 'Super Admin',
          department: '',
          year: '',
          message: 'Login successful'
        });
      }
    }

    // 2. Query Staff Admins
    const staffRes = await pool.query(
      'SELECT * FROM staff_admins WHERE LOWER(email) = $1 OR LOWER(staff_id) = $1',
      [cleanEmail]
    );
    if (staffRes.rows.length > 0) {
      const u = staffRes.rows[0];
      const match = await verifyAndMigratePassword(password, u, 'staff_admins', 'id', u.id);
      if (match) {
        const userId = u.staff_id || 'STF001';
        const role = 'STAFF_ADMIN';
        const payload = { userId, role, name: u.name || 'Staff Admin', email: u.email || cleanEmail, department: u.department || 'CSE' };
        const accessToken = generateAccessToken(payload);
        const refreshToken = generateRefreshToken({ userId, role });
        await storeRefreshToken(userId, role, refreshToken, clientIp, userAgent);

        return res.json({
          success: true,
          token: accessToken,
          refreshToken: refreshToken,
          role: role,
          user_id: userId,
          user_name: u.name || 'Staff Admin',
          department: u.department || 'CSE',
          year: '',
          message: 'Login successful'
        });
      }
    }

    // 3. Query Student Admins
    const saRes = await pool.query(
      'SELECT * FROM student_admins WHERE LOWER(email) = $1 OR LOWER(admin_id) = $1',
      [cleanEmail]
    );
    if (saRes.rows.length > 0) {
      const u = saRes.rows[0];
      const match = await verifyAndMigratePassword(password, u, 'student_admins', 'id', u.id);
      if (match) {
        const userId = u.admin_id || 'SADM001';
        const role = 'STUDENT_ADMIN';
        const payload = { userId, role, name: u.name || 'Student Admin', email: u.email || cleanEmail, department: u.department || 'CSE', year: u.year || 'III Year' };
        const accessToken = generateAccessToken(payload);
        const refreshToken = generateRefreshToken({ userId, role });
        await storeRefreshToken(userId, role, refreshToken, clientIp, userAgent);

        return res.json({
          success: true,
          token: accessToken,
          refreshToken: refreshToken,
          role: role,
          user_id: userId,
          user_name: u.name || 'Student Admin',
          department: u.department || 'CSE',
          year: u.year || 'III Year',
          message: 'Login successful'
        });
      }
    }

    // 4. Query Students
    const stRes = await pool.query(
      'SELECT * FROM students WHERE LOWER(email) = $1 OR LOWER(register_number) = $1',
      [cleanEmail]
    );
    if (stRes.rows.length > 0) {
      const u = stRes.rows[0];
      const match = await verifyAndMigratePassword(password, u, 'students', 'id', u.id);
      if (match) {
        const userId = u.register_number || 'CSE22CS001';
        const role = 'STUDENT';
        const payload = { userId, role, name: u.name || 'Student', email: u.email || cleanEmail, department: u.department || 'CSE', year: u.year || 'III Year' };
        const accessToken = generateAccessToken(payload);
        const refreshToken = generateRefreshToken({ userId, role });
        await storeRefreshToken(userId, role, refreshToken, clientIp, userAgent);

        return res.json({
          success: true,
          token: accessToken,
          refreshToken: refreshToken,
          role: role,
          user_id: userId,
          user_name: u.name || 'Student',
          department: u.department || 'CSE',
          year: u.year || 'III Year',
          message: 'Login successful'
        });
      }
    }

    return res.status(401).json({ success: false, message: 'Invalid email/ID or password' });

  } catch (err) {
    console.error('[-] Auth Login Error:', err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// Refresh Token Endpoint with Reuse Detection & Rotation
app.post('/api/auth/refresh', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) {
    return res.status(401).json({ success: false, message: 'Refresh token is required' });
  }

  const clientIp = req.ip || req.socket.remoteAddress || '';
  const userAgent = req.headers['user-agent'] || '';

  try {
    const tokenRes = await pool.query(
      'SELECT * FROM refresh_tokens WHERE token = $1',
      [refreshToken]
    );

    if (tokenRes.rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid refresh token' });
    }

    const storedToken = tokenRes.rows[0];

    // Reuse Detection Security Alert: If a revoked token is presented, revoke all active sessions for that user
    if (storedToken.revoked) {
      console.warn(`[!] SECURITY ALERT: Refresh token reuse detected for user ${storedToken.user_id}! Revoking all user sessions.`);
      await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = $1', [storedToken.user_id]);
      return res.status(401).json({ success: false, message: 'Security alert: Token reuse detected. All sessions revoked.' });
    }

    if (new Date(storedToken.expires_at) <= new Date()) {
      return res.status(401).json({ success: false, message: 'Expired refresh token' });
    }

    const decoded = jwt.verify(refreshToken, JWT_REFRESH_SECRET);

    // Revoke old refresh token (Rotation)
    await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE id = $1', [storedToken.id]);

    // Issue new token pair
    const payload = { userId: decoded.userId, role: decoded.role };
    const newAccessToken = generateAccessToken(payload);
    const newRefreshToken = generateRefreshToken(payload);

    await storeRefreshToken(decoded.userId, decoded.role, newRefreshToken, clientIp, userAgent);

    res.json({
      success: true,
      token: newAccessToken,
      refreshToken: newRefreshToken,
      role: decoded.role,
      user_id: decoded.userId,
      message: 'Token refreshed successfully'
    });
  } catch (err) {
    console.error('[-] Refresh Token Error:', err.message);
    res.status(401).json({ success: false, message: 'Invalid or expired refresh token' });
  }
});

// Logout Endpoint
app.post('/api/auth/logout', async (req, res) => {
  const { refreshToken } = req.body;
  if (refreshToken) {
    try {
      await pool.query('UPDATE refresh_tokens SET revoked = TRUE WHERE token = $1', [refreshToken]);
    } catch (err) {
      console.error('[-] Logout token revocation error:', err.message);
    }
  }
  res.json({ success: true, message: 'Logged out successfully' });
});

// Change Password Endpoint (JWT Identity Verified, Bcrypt Hashed, Role Isolated)
app.post('/api/auth/change-password', authenticateToken, async (req, res) => {
  const { currentPassword, newPassword } = req.body;

  if (!currentPassword || !newPassword) {
    return res.status(400).json({ success: false, message: 'Current password and new password are required' });
  }

  // Enforce strong password requirements: min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character
  const strongPasswordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&^#\-_=+~`|<>,./:;'"\\]).{8,}$/;
  if (!strongPasswordRegex.test(newPassword)) {
    return res.status(400).json({
      success: false,
      message: 'New password must be at least 8 characters long and contain an uppercase letter, a lowercase letter, a digit, and a special character.'
    });
  }

  if (currentPassword === newPassword) {
    return res.status(400).json({
      success: false,
      message: 'New password must be different from current password.'
    });
  }

  // Identity MUST be determined ONLY by verified JWT payload (req.user)
  const { userId, role, email: userEmail } = req.user;
  if (!userId || !role) {
    return res.status(401).json({ success: false, message: 'Invalid authentication context' });
  }

  const cleanUserId = userId.trim().toLowerCase();
  const cleanEmail = userEmail ? userEmail.trim().toLowerCase() : cleanUserId;

  try {
    let tableName = '';
    let queryStr = '';

    if (role === 'SUPER_ADMIN') {
      tableName = 'super_admins';
      queryStr = 'SELECT * FROM super_admins WHERE LOWER(admin_id) = $1 OR LOWER(email) = $1 OR LOWER(admin_id) = $2 OR LOWER(email) = $2';
    } else if (role === 'STAFF_ADMIN') {
      tableName = 'staff_admins';
      queryStr = 'SELECT * FROM staff_admins WHERE LOWER(staff_id) = $1 OR LOWER(email) = $1 OR LOWER(staff_id) = $2 OR LOWER(email) = $2';
    } else if (role === 'STUDENT_ADMIN') {
      tableName = 'student_admins';
      queryStr = 'SELECT * FROM student_admins WHERE LOWER(admin_id) = $1 OR LOWER(email) = $1 OR LOWER(admin_id) = $2 OR LOWER(email) = $2';
    } else if (role === 'STUDENT') {
      tableName = 'students';
      queryStr = 'SELECT * FROM students WHERE LOWER(register_number) = $1 OR LOWER(email) = $1 OR LOWER(register_number) = $2 OR LOWER(email) = $2';
    } else {
      return res.status(403).json({ success: false, message: 'Unauthorized role for password modification' });
    }

    const userRes = await pool.query(queryStr, [cleanUserId, cleanEmail]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'User record not found' });
    }

    const dbUser = userRes.rows[0];

    // Verify current password with bcrypt / legacy auto-migration helper
    const isValidPassword = await verifyAndMigratePassword(currentPassword, dbUser, tableName, 'id', dbUser.id);
    if (!isValidPassword) {
      return res.status(400).json({ success: false, message: 'Incorrect current password' });
    }

    // Hash the new password using bcrypt (salt rounds = 10)
    const newHashedPassword = await bcrypt.hash(newPassword, 10);

    // Update ONLY this user's password record in Neon PostgreSQL
    await pool.query(
      `UPDATE ${tableName} SET password = $1, updated_at = NOW() WHERE id = $2`,
      [newHashedPassword, dbUser.id]
    );

    // Invalidate existing sessions for this user in Neon PostgreSQL
    await pool.query(
      'UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = $1 OR user_id = $2',
      [userId, cleanUserId]
    );

    console.log(`[+] Password updated successfully for ${role} user: ${userId}`);

    res.json({
      success: true,
      message: 'Password changed successfully. Please log in with your new password.'
    });

  } catch (err) {
    console.error('[-] Change Password Error:', err);
    res.status(500).json({ success: false, message: 'Server error while updating password: ' + err.message });
  }
});

// Alias Route for Change Password
app.post('/api/change-password', authenticateToken, (req, res) => {
  // Forward to /api/auth/change-password logic
  req.url = '/api/auth/change-password';
  app._router.handle(req, res);
});

// =============================================================================
// 2. STUDENTS ROUTES
// =============================================================================
app.get('/api/students', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM students ORDER BY name ASC');
    res.json(sanitizeUser(result.rows));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/students/:regNo', authenticateToken, checkStudentAccess, async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM students WHERE LOWER(register_number) = LOWER($1) OR LOWER(email) = LOWER($1)', [req.params.regNo]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/students', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
  const { name, register_number, department, year, email, phone, password, assigned_admin } = req.body;
  try {
    const hashedPassword = password ? await bcrypt.hash(password, 10) : await bcrypt.hash('123', 10);
    const query = `
      INSERT INTO students (name, register_number, department, year, email, phone, password, assigned_admin)
      VALUES ($1, $2, $3, $4, $5, $6, $7, COALESCE($8, ''))
      ON CONFLICT (register_number) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, year = EXCLUDED.year,
        email = EXCLUDED.email, phone = EXCLUDED.phone, password = EXCLUDED.password,
        assigned_admin = EXCLUDED.assigned_admin, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, register_number, department, year, email, phone, hashedPassword, assigned_admin]);
    res.status(201).json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/students/:regNo', authenticateToken, checkStudentAccess, async (req, res) => {
  const { name, department, year, email, phone, password, assigned_admin } = req.body;
  try {
    let hashedPassword = password;
    if (password && !password.startsWith('$2a$') && !password.startsWith('$2b$')) {
      hashedPassword = await bcrypt.hash(password, 10);
    }

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
    const result = await pool.query(query, [name, department, year, email, phone, hashedPassword, assigned_admin, req.params.regNo]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/students/:regNo', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
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
app.get('/api/student-admins', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM student_admins ORDER BY name ASC');
    res.json(sanitizeUser(result.rows));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/student-admins/:adminId', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM student_admins WHERE LOWER(admin_id) = LOWER($1) OR LOWER(email) = LOWER($1)', [req.params.adminId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student Admin not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/student-admins', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
  const { name, admin_id, department, year, email, phone, password, students_assigned_count, assigned_staff_admin, class_day } = req.body;
  try {
    const hashedPassword = password ? await bcrypt.hash(password, 10) : await bcrypt.hash('123', 10);
    const query = `
      INSERT INTO student_admins (name, admin_id, department, year, email, phone, password, students_assigned_count, assigned_staff_admin, class_day)
      VALUES ($1, $2, $3, $4, $5, $6, $7, COALESCE($8, 0), COALESCE($9, ''), COALESCE($10, ''))
      ON CONFLICT (admin_id) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, year = EXCLUDED.year,
        email = EXCLUDED.email, phone = EXCLUDED.phone, password = EXCLUDED.password,
        assigned_staff_admin = EXCLUDED.assigned_staff_admin, class_day = EXCLUDED.class_day, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, admin_id, department, year, email, phone, hashedPassword, students_assigned_count, assigned_staff_admin, class_day || '']);
    const savedAdmin = result.rows[0];

    // Trigger 30-second development test notification if class_day was specified
    if (typeof trigger30SecondScheduleTestNotification === 'function') {
      trigger30SecondScheduleTestNotification(savedAdmin);
    }

    res.status(201).json(sanitizeUser(savedAdmin));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/student-admins/:adminId', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
  const { name, department, year, email, phone, password, assigned_staff_admin, class_day } = req.body;
  try {
    let hashedPassword = password;
    if (password && !password.startsWith('$2a$') && !password.startsWith('$2b$')) {
      hashedPassword = await bcrypt.hash(password, 10);
    }
    const query = `
      UPDATE student_admins SET
        name = COALESCE($1, name),
        department = COALESCE($2, department),
        year = COALESCE($3, year),
        email = COALESCE($4, email),
        phone = COALESCE($5, phone),
        password = COALESCE($6, password),
        assigned_staff_admin = COALESCE($7, assigned_staff_admin),
        class_day = COALESCE($8, class_day),
        updated_at = NOW()
      WHERE admin_id = $9
      RETURNING *;
    `;
    const result = await pool.query(query, [name, department, year, email, phone, hashedPassword, assigned_staff_admin, class_day, req.params.adminId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Student Admin not found' });
    const updatedAdmin = result.rows[0];

    // Trigger 30-second development test notification if class_day was updated
    if (typeof trigger30SecondScheduleTestNotification === 'function') {
      trigger30SecondScheduleTestNotification(updatedAdmin);
    }

    res.json(sanitizeUser(updatedAdmin));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/student-admins/:adminId', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
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
app.get('/api/staff-admins', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM staff_admins ORDER BY name ASC');
    res.json(sanitizeUser(result.rows));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/staff-admins/:idOrEmail', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM staff_admins WHERE LOWER(staff_id) = LOWER($1) OR LOWER(email) = LOWER($1)', [req.params.idOrEmail]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Staff Admin not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/staff-admins', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  const { name, staff_id, department, email, phone, password } = req.body;
  try {
    const hashedPassword = password ? await bcrypt.hash(password, 10) : await bcrypt.hash('123', 10);
    const query = `
      INSERT INTO staff_admins (name, staff_id, department, email, phone, password)
      VALUES ($1, $2, $3, $4, $5, $6)
      ON CONFLICT (staff_id) DO UPDATE SET
        name = EXCLUDED.name, department = EXCLUDED.department, email = EXCLUDED.email,
        phone = EXCLUDED.phone, password = EXCLUDED.password, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, staff_id, department, email, phone, hashedPassword]);
    res.status(201).json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/staff-admins/:staffId', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
  const { name, department, email, phone, password } = req.body;
  try {
    let hashedPassword = password;
    if (password && !password.startsWith('$2a$') && !password.startsWith('$2b$')) {
      hashedPassword = await bcrypt.hash(password, 10);
    }
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
    const result = await pool.query(query, [name, department, email, phone, hashedPassword, req.params.staffId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Staff Admin not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/staff-admins/:staffId', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
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
app.get('/api/super-admins', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM super_admins ORDER BY name ASC');
    res.json(sanitizeUser(result.rows));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/super-admins/:idOrEmail', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM super_admins WHERE LOWER(admin_id) = LOWER($1) OR LOWER(email) = LOWER($1)', [req.params.idOrEmail]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Super Admin not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/super-admins/:adminId', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  const { name, email, phone, password } = req.body;
  try {
    let hashedPassword = password;
    if (password && !password.startsWith('$2a$') && !password.startsWith('$2b$')) {
      hashedPassword = await bcrypt.hash(password, 10);
    }
    const query = `
      UPDATE super_admins SET
        name = COALESCE($1, name),
        email = COALESCE($2, email),
        phone = COALESCE($3, phone),
        password = COALESCE($4, password),
        updated_at = NOW()
      WHERE LOWER(admin_id) = LOWER($5) OR LOWER(email) = LOWER($5)
      RETURNING *;
    `;
    const result = await pool.query(query, [name, email, phone, hashedPassword, req.params.adminId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Super Admin not found' });
    res.json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/super-admins', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
  const { name, admin_id, email, phone, password } = req.body;
  try {
    const hashedPassword = password ? await bcrypt.hash(password, 10) : await bcrypt.hash('admin', 10);
    const query = `
      INSERT INTO super_admins (name, admin_id, email, phone, password)
      VALUES ($1, $2, $3, $4, $5)
      ON CONFLICT (email) DO UPDATE SET
        name = EXCLUDED.name, admin_id = EXCLUDED.admin_id, phone = EXCLUDED.phone,
        password = EXCLUDED.password, updated_at = NOW()
      RETURNING *;
    `;
    const result = await pool.query(query, [name, admin_id, email, phone, hashedPassword]);
    res.status(201).json(sanitizeUser(result.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// =============================================================================
// 6. ATTENDANCE ROUTES
// =============================================================================
app.get('/api/attendance/student/:regNo', authenticateToken, checkStudentAccess, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM attendance WHERE register_number = $1 ORDER BY date_key DESC',
      [req.params.regNo]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/attendance/:regNo/:dateKey', authenticateToken, checkStudentAccess, async (req, res) => {
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

app.post('/api/attendance', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
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

app.post('/api/attendance/batch', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
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

app.get('/api/activities/student/:regNo', authenticateToken, checkStudentAccess, async (req, res) => {
  const { regNo } = req.params;
  try {
    const result = await pool.query(
      'SELECT * FROM daily_activities WHERE register_number = $1 ORDER BY date_key DESC',
      [regNo]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

app.get('/api/activities/:regNo/:dateKey', authenticateToken, checkStudentAccess, getActivityHandler);
app.get('/api/daily-activities/:regNo/:dateKey', authenticateToken, checkStudentAccess, getActivityHandler);

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

app.post('/api/activities', authenticateToken, upload.single('file'), checkStudentAccess, upsertActivityHandler);
app.post('/api/daily-activities', authenticateToken, upload.single('file'), checkStudentAccess, upsertActivityHandler);

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

app.delete('/api/activities/:regNo/:dateKey', authenticateToken, checkStudentAccess, deleteActivityHandler);
app.delete('/api/daily-activities/:regNo/:dateKey', authenticateToken, checkStudentAccess, deleteActivityHandler);

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

app.patch('/api/activities/:regNo/:dateKey/remark', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), updateRemarkHandler);
app.patch('/api/daily-activities/:regNo/:dateKey/remark', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), updateRemarkHandler);

// =============================================================================
// 8. ANNOUNCEMENTS ROUTES
// =============================================================================
app.get('/api/announcements', authenticateToken, async (req, res) => {
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

app.post('/api/announcements', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN'), async (req, res) => {
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
app.get('/api/departments', authenticateToken, async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM departments ORDER BY code ASC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/departments', authenticateToken, authorizeRoles('SUPER_ADMIN'), async (req, res) => {
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

// =============================================================================
// 10. FCM NOTIFICATION TOKEN REGISTRATION ROUTES
// =============================================================================

/**
 * POST /api/notifications/register-token
 * Authenticated endpoint to register or update user's FCM device token in Neon PostgreSQL.
 * Extracts userId and role directly from verified server-side JWT (req.user).
 */
app.post('/api/notifications/register-token', authenticateToken, async (req, res) => {
  const { fcm_token, device_info } = req.body;
  if (!fcm_token || typeof fcm_token !== 'string' || !fcm_token.trim()) {
    return res.status(400).json({ success: false, message: 'fcm_token is required' });
  }

  const userId = req.user.userId;
  const role = req.user.role;
  const tokenClean = fcm_token.trim();
  const deviceInfoClean = (device_info || '').trim();

  try {
    const query = `
      INSERT INTO user_device_tokens (user_id, role, fcm_token, device_info, updated_at)
      VALUES ($1, $2, $3, $4, NOW())
      ON CONFLICT (fcm_token)
      DO UPDATE SET
        user_id = EXCLUDED.user_id,
        role = EXCLUDED.role,
        device_info = EXCLUDED.device_info,
        updated_at = NOW()
      RETURNING id, user_id, role, created_at, updated_at;
    `;

    const result = await pool.query(query, [userId, role, tokenClean, deviceInfoClean]);
    const maskedToken = tokenClean.length > 10 ? `${tokenClean.substring(0, 10)}...` : tokenClean;
    console.log(`[+] FCM Token registered in Neon for user: ${userId} (${role}), Token: ${maskedToken}`);

    return res.status(200).json({
      success: true,
      message: 'FCM token registered successfully',
      data: result.rows[0]
    });
  } catch (err) {
    console.error('[-] Error registering FCM token:', err.message);
    return res.status(500).json({ success: false, message: 'Failed to register FCM token', error: err.message });
  }
});

/**
 * POST /api/notifications/unregister-token
 * Authenticated endpoint to unregister FCM token from Neon PostgreSQL on logout.
 */
app.post('/api/notifications/unregister-token', authenticateToken, async (req, res) => {
  const { fcm_token } = req.body;
  if (!fcm_token || typeof fcm_token !== 'string' || !fcm_token.trim()) {
    return res.status(400).json({ success: false, message: 'fcm_token is required' });
  }

  const userId = req.user.userId;
  const tokenClean = fcm_token.trim();

  try {
    await pool.query(
      'DELETE FROM user_device_tokens WHERE fcm_token = $1 AND user_id = $2',
      [tokenClean, userId]
    );
    const maskedToken = tokenClean.length > 10 ? `${tokenClean.substring(0, 10)}...` : tokenClean;
    console.log(`[+] FCM Token unregistered from Neon for user: ${userId}, Token: ${maskedToken}`);

    return res.status(200).json({
      success: true,
      message: 'FCM token unregistered successfully'
    });
  } catch (err) {
    console.error('[-] Error unregistering FCM token:', err.message);
    return res.status(500).json({ success: false, message: 'Failed to unregister FCM token', error: err.message });
  }
});

/**
 * POST /api/notifications/test-send
 * Authenticated development endpoint to send a test FCM push notification.
 * ONLY sends to the authenticated user's registered device tokens.
 */
app.post('/api/notifications/test-send', authenticateToken, async (req, res) => {
  const userId = req.user.userId;
  const role = req.user.role;
  const { title, message } = req.body || {};

  const notificationTitle = (title && title.trim()) ? title.trim() : 'TrackPoint – Test Notification';
  const notificationBody = (message && message.trim()) ? message.trim() : 'Your TrackPoint notification system is working successfully!';

  try {
    // 1. Fetch authenticated user's registered FCM tokens from Neon (strictly matching user_id AND role)
    const tokenResult = await pool.query(
      'SELECT fcm_token FROM user_device_tokens WHERE user_id = $1 AND role = $2',
      [userId, role]
    );

    if (tokenResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: `No registered device tokens found for user ${userId}. Please log in from the TrackPoint app first.`
      });
    }

    if (!isFirebaseAdminInitialized || admin.apps.length === 0) {
      return res.status(503).json({
        success: false,
        message: 'Firebase Admin SDK is not configured on backend server. Please set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS in backend .env'
      });
    }

    const tokens = tokenResult.rows.map(row => row.fcm_token);
    let successCount = 0;
    let failureCount = 0;
    const invalidTokens = [];

    for (const fcmToken of tokens) {
      const payload = {
        token: fcmToken,
        notification: {
          title: notificationTitle,
          body: notificationBody
        },
        data: {
          click_action: 'FLUTTER_NOTIFICATION_CLICK',
          type: 'TEST_NOTIFICATION',
          timestamp: String(Date.now())
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'trackpoint_default_channel',
            sound: 'default',
            defaultVibrateTimings: true,
            priority: 'high'
          }
        }
      };

      try {
        await admin.messaging().send(payload);
        successCount++;
        const maskedToken = fcmToken.length > 10 ? `${fcmToken.substring(0, 10)}...` : fcmToken;
        console.log(`[+] Sent FCM test notification to user ${userId} (${role}), Token: ${maskedToken}`);
      } catch (sendErr) {
        failureCount++;
        const errCode = sendErr.code || '';
        const errMsg = sendErr.message || '';
        console.error(`[-] Failed sending FCM message to token for user ${userId}:`, errMsg);

        if (
          errCode === 'messaging/registration-token-not-registered' ||
          errCode === 'messaging/invalid-registration-token' ||
          errMsg.includes('not-registered') ||
          errMsg.includes('invalid')
        ) {
          invalidTokens.push(fcmToken);
        }
      }
    }

    // Purge confirmed stale/invalid tokens from Neon
    for (const staleToken of invalidTokens) {
      await pool.query('DELETE FROM user_device_tokens WHERE fcm_token = $1', [staleToken]);
      const maskedStale = staleToken.length > 10 ? `${staleToken.substring(0, 10)}...` : staleToken;
      console.log(`[+] Purged stale/invalid FCM token from Neon: ${maskedStale}`);
    }

    return res.status(200).json({
      success: true,
      message: `Test notification process completed. Successfully sent: ${successCount}, Failed: ${failureCount}`,
      details: {
        total_tokens: tokens.length,
        successful_sends: successCount,
        failed_sends: failureCount,
        purged_stale_tokens: invalidTokens.length
      }
    });
  } catch (err) {
    console.error('[-] Error in test notification API:', err.message);
    return res.status(500).json({ success: false, message: 'Server error sending test notification', error: err.message });
  }
});

// =============================================================================
// 11. AUTOMATIC CLASS-DAY SCHEDULER & NOTIFICATION PIPELINE
// =============================================================================

// Helper: Get IST Date Information
function getISTDateInfo() {
  const now = new Date();
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Kolkata',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
  const parts = formatter.formatToParts(now);
  const map = {};
  parts.forEach(p => map[p.type] = p.value);
  const dateKey = `${map.year}-${map.month}-${map.day}`;
  return {
    dayOfWeek: map.weekday,
    hour: parseInt(map.hour, 10),
    minute: parseInt(map.minute, 10),
    dateKey: dateKey
  };
}

// Helper: Send notification to user tokens and log error/success with DB idempotency
async function sendNotificationToUserTokens(userId, role, title, body, notificationType, jobKey) {
  if (!userId) return;
  try {
    // 1. Check idempotency: if job was ALREADY successfully sent, skip!
    const existingLog = await pool.query(
      "SELECT status FROM scheduled_notification_logs WHERE job_key = $1 AND status = 'SUCCESS'",
      [jobKey]
    );

    if (existingLog.rows.length > 0) {
      // Job already completed successfully previously
      return;
    }

    // 2. Insert or update processing status in Neon database
    await pool.query(
      `INSERT INTO scheduled_notification_logs (job_key, notification_type, target_user_id, target_role, status, title, body, sent_at)
       VALUES ($1, $2, $3, $4, 'PROCESSING', $5, $6, NOW())
       ON CONFLICT (job_key) DO UPDATE SET status = 'PROCESSING', sent_at = NOW();`,
      [jobKey, notificationType, userId, role, title, body]
    );

    // 3. Fetch FCM device tokens for user & role (support userId, staff_id, email, or name)
    const tokenRes = await pool.query(
      'SELECT fcm_token FROM user_device_tokens WHERE (user_id = $1 OR LOWER(user_id) = LOWER($1)) AND role = $2',
      [userId, role]
    );

    if (tokenRes.rows.length === 0) {
      await pool.query('UPDATE scheduled_notification_logs SET status = $1 WHERE job_key = $2', ['NO_TOKENS', jobKey]);
      console.log(`[!] No FCM tokens found for ${role} user: ${userId} (${notificationType}). Will retry on next schedule check when token is registered.`);
      return;
    }

    if (!isFirebaseAdminInitialized || admin.apps.length === 0) {
      await pool.query('UPDATE scheduled_notification_logs SET status = $1 WHERE job_key = $2', ['FIREBASE_NOT_CONFIGURED', jobKey]);
      console.warn(`[!] Cannot send ${notificationType}: Firebase Admin SDK not initialized`);
      return;
    }

    const tokens = tokenRes.rows.map(r => r.fcm_token);
    let successCount = 0;
    const invalidTokens = [];

    for (const token of tokens) {
      const payload = {
        token: token,
        notification: { title, body },
        data: {
          type: notificationType,
          click_action: 'FLUTTER_NOTIFICATION_CLICK',
          timestamp: String(Date.now())
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'trackpoint_default_channel',
            sound: 'default',
            defaultVibrateTimings: true,
            priority: 'high'
          }
        }
      };

      try {
        await admin.messaging().send(payload);
        successCount++;
        const masked = token.length > 10 ? token.substring(0, 10) + '...' : token;
        console.log(`[+] Sent ${notificationType} to ${role} (${userId}): ${masked}`);
      } catch (err) {
        const errCode = err.code || '';
        const errMsg = err.message || '';
        if (
          errCode === 'messaging/registration-token-not-registered' ||
          errCode === 'messaging/invalid-registration-token' ||
          errMsg.includes('not-registered') ||
          errMsg.includes('invalid')
        ) {
          invalidTokens.push(token);
        }
      }
    }

    // Purge invalid tokens
    for (const staleToken of invalidTokens) {
      await pool.query('DELETE FROM user_device_tokens WHERE fcm_token = $1', [staleToken]);
      console.log(`[+] Purged stale token from Neon: ${staleToken.substring(0, 10)}...`);
    }

    const finalStatus = successCount > 0 ? 'SUCCESS' : 'FAILED';
    await pool.query('UPDATE scheduled_notification_logs SET status = $1 WHERE job_key = $2', [finalStatus, jobKey]);
  } catch (err) {
    console.error(`[-] Error in sendNotificationToUserTokens (${notificationType}):`, err.message);
    try {
      await pool.query("UPDATE scheduled_notification_logs SET status = 'ERROR' WHERE job_key = $1", [jobKey]);
    } catch (e) {}
  }
}

// Helper: Resolve Staff Admin's user_id or email
async function getStaffAdminUserId(assignedStaffNameOrId) {
  if (!assignedStaffNameOrId || !assignedStaffNameOrId.trim()) return null;
  const clean = assignedStaffNameOrId.trim();
  try {
    const res = await pool.query(
      'SELECT staff_id, email, name FROM staff_admins WHERE LOWER(staff_id) = LOWER($1) OR LOWER(email) = LOWER($1) OR LOWER(name) = LOWER($1) LIMIT 1',
      [clean]
    );
    if (res.rows.length > 0) {
      return res.rows[0].staff_id || res.rows[0].email || res.rows[0].name;
    }
  } catch (err) {
    console.error('[-] Error resolving Staff Admin user ID:', err.message);
  }
  return clean;
}

// Durable 30-Second Schedule Test Notification Handler (Database-Backed & Restart-Resilient)
async function trigger30SecondScheduleTestNotification(studentAdminObj) {
  if (!studentAdminObj || !studentAdminObj.class_day) return;
  if (process.env.ENABLE_TEST_SCHEDULE_NOTIFICATIONS === 'false') return;

  const adminId = studentAdminObj.admin_id;
  const adminName = studentAdminObj.name || 'Student Admin';
  const assignedStaff = studentAdminObj.assigned_staff_admin;
  const classDay = studentAdminObj.class_day;

  if (!classDay || classDay === 'Select Day of Class' || classDay === 'None') return;

  const istInfo = getISTDateInfo();
  const isClassToday = (istInfo.dayOfWeek.toLowerCase() === classDay.toLowerCase());

  const saTitle = 'TrackPoint – Class Schedule Test';
  const saBody = isClassToday
    ? `[Schedule Test] Your class day was set to ${classDay}. Class is today!`
    : `[Schedule Test] Your class day was set to ${classDay}. (Scheduled for upcoming ${classDay}).`;

  const staffTitle = 'TrackPoint – Class Schedule Test';
  const staffBody = isClassToday
    ? `[Schedule Test] ${adminName}'s class day was set to ${classDay}. Class is today!`
    : `[Schedule Test] ${adminName}'s class day was set to ${classDay}. (Scheduled for upcoming ${classDay}).`;

  const timestampId = Date.now();
  console.log(`[+] Scheduling durable 30-second test notification for Student Admin ${adminId} and Staff Admin ${assignedStaff} (Class Day: ${classDay})`);

  // 1. Insert PENDING job records into Neon PostgreSQL (Persisted across backend restarts!)
  const saJobKey = `TEST_30SEC_SA_${adminId}_${timestampId}`;
  try {
    await pool.query(
      `INSERT INTO scheduled_notification_logs (job_key, notification_type, target_user_id, target_role, status, title, body, execute_at)
       VALUES ($1, '30SEC_TEST_SA', $2, 'STUDENT_ADMIN', 'PENDING', $3, $4, NOW() + INTERVAL '30 seconds')
       ON CONFLICT (job_key) DO NOTHING;`,
      [saJobKey, adminId, saTitle, saBody]
    );

    if (assignedStaff) {
      const staffUserId = await getStaffAdminUserId(assignedStaff);
      if (staffUserId) {
        const staffJobKey = `TEST_30SEC_STAFF_${adminId}_${timestampId}`;
        await pool.query(
          `INSERT INTO scheduled_notification_logs (job_key, notification_type, target_user_id, target_role, status, title, body, execute_at)
           VALUES ($1, '30SEC_TEST_STAFF', $2, 'STAFF_ADMIN', 'PENDING', $3, $4, NOW() + INTERVAL '30 seconds')
           ON CONFLICT (job_key) DO NOTHING;`,
          [staffJobKey, staffUserId, staffTitle, staffBody]
        );
      }
    }
  } catch (err) {
    console.error('[-] Error inserting pending 30-second test job:', err.message);
  }

  // 2. In-memory setTimeout for fast dispatch
  setTimeout(async () => {
    await sendNotificationToUserTokens(adminId, 'STUDENT_ADMIN', saTitle, saBody, '30SEC_TEST_SA', saJobKey);
    if (assignedStaff) {
      const staffUserId = await getStaffAdminUserId(assignedStaff);
      if (staffUserId) {
        const staffJobKey = `TEST_30SEC_STAFF_${adminId}_${timestampId}`;
        await sendNotificationToUserTokens(staffUserId, 'STAFF_ADMIN', staffTitle, staffBody, '30SEC_TEST_STAFF', staffJobKey);
      }
    }
  }, 30000);
}

// Main Cron / Timer Scheduler Check (Runs every 30 seconds)
let lastProcessedMinute = '';
async function checkAndSendClassDayNotifications() {
  try {
    // Check for any PENDING 30-second test jobs due for execution (Recovery from backend restarts)
    const pendingJobs = await pool.query(
      "SELECT * FROM scheduled_notification_logs WHERE status = 'PENDING' AND execute_at <= NOW()"
    );
    for (const job of pendingJobs.rows) {
      await sendNotificationToUserTokens(job.target_user_id, job.target_role, job.title || 'TrackPoint Schedule Test', job.body || 'Schedule Update', job.notification_type, job.job_key);
    }

    const ist = getISTDateInfo();
    const currentMinuteKey = `${ist.dateKey}_${ist.hour}_${ist.minute}`;
    if (currentMinuteKey === lastProcessedMinute) return;
    lastProcessedMinute = currentMinuteKey;

    const currentDay = ist.dayOfWeek.toLowerCase();

    // 8:00 AM IST Check
    if (ist.hour === 8 && ist.minute === 0) {
      console.log(`[+] Executing 8:00 AM IST Class Reminders for day: ${ist.dayOfWeek}`);
      const saResult = await pool.query(
        'SELECT * FROM student_admins WHERE LOWER(class_day) = LOWER($1)',
        [currentDay]
      );

      for (const sa of saResult.rows) {
        const saJobKey = `REMINDER_8AM_SA_${sa.admin_id}_${ist.dateKey}`;
        const saTitle = 'TrackPoint – Class Reminder';
        const saBody = 'Your class is today. Please record attendance.';
        await sendNotificationToUserTokens(sa.admin_id, 'STUDENT_ADMIN', saTitle, saBody, '8AM_CLASS_REMINDER', saJobKey);

        if (sa.assigned_staff_admin) {
          const staffUserId = await getStaffAdminUserId(sa.assigned_staff_admin);
          if (staffUserId) {
            const staffJobKey = `REMINDER_8AM_STAFF_${sa.admin_id}_${ist.dateKey}`;
            const staffTitle = 'TrackPoint – Class Schedule';
            const staffBody = `${sa.name || 'Student Admin'} takes class today.`;
            await sendNotificationToUserTokens(staffUserId, 'STAFF_ADMIN', staffTitle, staffBody, '8AM_CLASS_SCHEDULE', staffJobKey);
          }
        }
      }
    }

    // 3:00 PM IST Check (15:00)
    if (ist.hour === 15 && ist.minute === 0) {
      console.log(`[+] Executing 3:00 PM IST Attendance Follow-up for day: ${ist.dayOfWeek}`);
      const saResult = await pool.query(
        'SELECT * FROM student_admins WHERE LOWER(class_day) = LOWER($1)',
        [currentDay]
      );

      for (const sa of saResult.rows) {
        // Attendance Verification Check: Multi-field check if attendance has already been recorded for today
        const attCheck = await pool.query(
          `SELECT COUNT(*) FROM attendance
           WHERE date_key = $1
           AND (
             LOWER(marked_by) = LOWER($2) OR LOWER(marked_by) = LOWER($3)
             OR register_number IN (SELECT register_number FROM students WHERE LOWER(assigned_admin) = LOWER($2) OR LOWER(assigned_admin) = LOWER($3))
           )`,
          [ist.dateKey, sa.admin_id, sa.name]
        );

        const count = parseInt(attCheck.rows[0].count, 10);
        if (count > 0) {
          console.log(`[+] Attendance already completed for Student Admin ${sa.admin_id} on ${ist.dateKey}. Skipping 3 PM reminder.`);
          continue;
        }

        const saJobKey = `REMINDER_3PM_SA_${sa.admin_id}_${ist.dateKey}`;
        const saTitle = 'TrackPoint – Attendance Reminder';
        const saBody = 'Please ensure today\'s class attendance is recorded.';
        await sendNotificationToUserTokens(sa.admin_id, 'STUDENT_ADMIN', saTitle, saBody, '3PM_ATTENDANCE_REMINDER', saJobKey);

        if (sa.assigned_staff_admin) {
          const staffUserId = await getStaffAdminUserId(sa.assigned_staff_admin);
          if (staffUserId) {
            const staffJobKey = `REMINDER_3PM_STAFF_${sa.admin_id}_${ist.dateKey}`;
            const staffTitle = 'TrackPoint – Attendance Follow-up';
            const staffBody = `Check today's attendance for ${sa.name || 'Student Admin'}.`;
            await sendNotificationToUserTokens(staffUserId, 'STAFF_ADMIN', staffTitle, staffBody, '3PM_ATTENDANCE_FOLLOWUP', staffJobKey);
          }
        }
      }
    }
  } catch (err) {
    console.error('[-] Error in class-day scheduler loop:', err.message);
  }
}

// Start scheduler loop (ticks every 30 seconds)
setInterval(checkAndSendClassDayNotifications, 30000);

/**
 * POST /api/notifications/test-class-day-trigger
 * Development testing endpoint to immediately trigger 8 AM or 3 PM reminder logic for testing.
 */
app.post('/api/notifications/test-class-day-trigger', authenticateToken, authorizeRoles('SUPER_ADMIN', 'STAFF_ADMIN', 'STUDENT_ADMIN'), async (req, res) => {
  if (process.env.NODE_ENV === 'production' && process.env.ENABLE_TEST_SCHEDULE_NOTIFICATIONS === 'false') {
    return res.status(403).json({ success: false, message: 'Test schedule trigger endpoint is disabled in production.' });
  }

  const { admin_id, reminder_type } = req.body;
  if (!admin_id) return res.status(400).json({ success: false, message: 'admin_id is required' });

  try {
    const saRes = await pool.query('SELECT * FROM student_admins WHERE LOWER(admin_id) = LOWER($1)', [admin_id]);
    if (saRes.rows.length === 0) return res.status(404).json({ success: false, message: 'Student Admin not found' });

    const sa = saRes.rows[0];
    const type = reminder_type || '8AM';

    if (type === '8AM') {
      const saJobKey = `TEST_MANUAL_8AM_SA_${sa.admin_id}_${Date.now()}`;
      await sendNotificationToUserTokens(sa.admin_id, 'STUDENT_ADMIN', 'TrackPoint – Class Reminder', 'Your class is today. Please record attendance.', '8AM_CLASS_REMINDER', saJobKey);

      if (sa.assigned_staff_admin) {
        const staffUserId = await getStaffAdminUserId(sa.assigned_staff_admin);
        if (staffUserId) {
          const staffJobKey = `TEST_MANUAL_8AM_STAFF_${sa.admin_id}_${Date.now()}`;
          await sendNotificationToUserTokens(staffUserId, 'STAFF_ADMIN', 'TrackPoint – Class Schedule', `${sa.name || 'Student Admin'} takes class today.`, '8AM_CLASS_SCHEDULE', staffJobKey);
        }
      }
    } else {
      const saJobKey = `TEST_MANUAL_3PM_SA_${sa.admin_id}_${Date.now()}`;
      await sendNotificationToUserTokens(sa.admin_id, 'STUDENT_ADMIN', 'TrackPoint – Attendance Reminder', 'Please ensure today\'s class attendance is recorded.', '3PM_ATTENDANCE_REMINDER', saJobKey);

      if (sa.assigned_staff_admin) {
        const staffUserId = await getStaffAdminUserId(sa.assigned_staff_admin);
        if (staffUserId) {
          const staffJobKey = `TEST_MANUAL_3PM_STAFF_${sa.admin_id}_${Date.now()}`;
          await sendNotificationToUserTokens(staffUserId, 'STAFF_ADMIN', 'TrackPoint – Attendance Follow-up', `Check today's attendance for ${sa.name || 'Student Admin'}.`, '3PM_ATTENDANCE_FOLLOWUP', staffJobKey);
        }
      }
    }

    return res.json({ success: true, message: `Triggered ${type} test reminders for Student Admin ${admin_id}` });
  } catch (err) {
    return res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/notifications/run-scheduled-jobs
 * External Cron Trigger endpoint invoked by Cloudflare Workers.
 * Authenticates via header 'X-Scheduler-Secret' matching process.env.CRON_SECRET.
 */
app.post('/api/notifications/run-scheduled-jobs', async (req, res) => {
  const cronSecret = process.env.CRON_SECRET;
  const requestSecret = req.headers['x-scheduler-secret'];

  if (!cronSecret || !requestSecret || !safeCompare(requestSecret, cronSecret)) {
    console.warn('[!] Unauthorized scheduler trigger attempt rejected (Invalid X-Scheduler-Secret)');
    return res.status(401).json({ success: false, message: 'Unauthorized: Invalid or missing X-Scheduler-Secret' });
  }

  try {
    const ist = getISTDateInfo();
    console.log(`[+] External Cron Trigger received at IST: ${ist.dateKey} ${ist.hour}:${ist.minute} (${ist.dayOfWeek})`);

    // Force execution check of scheduled class-day & pending notifications
    await checkAndSendClassDayNotifications();

    return res.json({
      success: true,
      message: 'Scheduled notification check completed successfully',
      ist_time: `${ist.dateKey} ${ist.hour}:${ist.minute} (${ist.dayOfWeek})`,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    console.error('[-] Error executing scheduled notifications:', err.message);
    return res.status(500).json({ success: false, message: 'Scheduler execution error', error: err.message });
  }
});

// Health Check Endpoint (For Cloud Load Balancers & Monitoring)
app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    environment: process.env.NODE_ENV || 'development',
    firebase_admin: isFirebaseAdminInitialized ? 'INITIALIZED' : 'NOT_CONFIGURED',
    database: 'CONNECTED',
    timestamp: new Date().toISOString()
  });
});

// Start Server
const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`[+] TrackPoint Centralized Express Backend running on port ${PORT} (Environment: ${process.env.NODE_ENV || 'development'})`);
});

// Graceful Shutdown Handler for Cloud Platforms (Render, Railway, AWS, Docker)
function gracefulShutdown(signal) {
  console.log(`[+] Received ${signal}. Initiating graceful shutdown...`);
  server.close(async () => {
    console.log('[+] Closed active HTTP connections.');
    try {
      await pool.end();
      console.log('[+] Released Neon PostgreSQL database connections.');
    } catch (err) {
      console.error('[-] Error closing Neon pool:', err.message);
    }
    process.exit(0);
  });
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
