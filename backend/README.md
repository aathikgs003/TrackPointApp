# TrackPoint Express.js Backend Server

Centralized Node.js Express backend server connecting the TrackPoint Android application to **Neon PostgreSQL** and **Backblaze B2 S3 Object Storage**.

## Prerequisites
- Node.js (v18+ recommended)
- Neon PostgreSQL connection string (`DATABASE_URL`)

## Setup Instructions

1. **Navigate to the backend folder**:
   ```bash
   cd backend
   ```

2. **Install Dependencies**:
   ```bash
   npm install
   ```

3. **Configure Environment Variables (`.env`)**:
   Open `.env` and replace `DATABASE_URL` with your actual Neon PostgreSQL connection string:
   ```env
   PORT=5000
   DATABASE_URL=postgresql://neondb_owner:YOUR_PASSWORD@ep-sample-pool-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   AWS_S3_ENDPOINT=https://s3.us-east-005.backblazeb2.com
   AWS_S3_REGION=us-east-005
   AWS_S3_ACCESS_KEY_ID=0051269645c6ad00000000001
   AWS_S3_SECRET_ACCESS_KEY=K005shOEwSiMuA3ZH3qsu7v1OYD+/Sw
   AWS_S3_BUCKET_NAME=trackpoint-data
   ```

4. **Start Server**:
   ```bash
   npm start
   # or for development mode with auto-reload:
   npm run dev
   ```

5. **Test Health Endpoint**:
   Open browser at `http://localhost:5000/health` (or `http://10.0.2.2:5000/health` inside Android Emulator).

## Implemented Routes Matrix

| Category | Endpoint | Method | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `/api/auth/login` | `POST` | Authenticates Super Admin, Staff Admin, Student Admin, or Student |
| **Students** | `/api/students` | `GET`, `POST` | List & Create Students |
| **Students** | `/api/students/:regNo` | `GET`, `PUT`, `DELETE` | Read, Update, Delete Student |
| **Student Admins** | `/api/student-admins` | `GET`, `POST` | List & Create Student Admins |
| **Student Admins** | `/api/student-admins/:adminId` | `GET`, `PUT`, `DELETE` | Read, Update, Delete Student Admin |
| **Staff Admins** | `/api/staff-admins` | `GET`, `POST` | List & Create Staff Admins |
| **Staff Admins** | `/api/staff-admins/:idOrEmail` | `GET`, `PUT`, `DELETE` | Read, Update, Delete Staff Admin |
| **Super Admins** | `/api/super-admins` | `GET`, `POST` | List & Create Super Admins |
| **Attendance** | `/api/attendance/:regNo/:dateKey` | `GET` | Get Attendance Status |
| **Attendance** | `/api/attendance` | `POST` | Record Single Attendance |
| **Attendance** | `/api/attendance/batch` | `POST` | Record Batch Attendance |
| **Daily Activities** | `/api/activities/:regNo/:dateKey` | `GET` | Get Daily Activity & Generate Presigned B2 S3 URL |
| **Daily Activities** | `/api/activities` | `POST` | Upload Activity / Record Metadata in Neon & S3 |
| **Daily Activities** | `/api/activities/:regNo/:dateKey` | `DELETE` | Delete Activity Record & Purge S3 Object |
| **Daily Activities** | `/api/activities/:regNo/:dateKey/remark` | `PATCH` | Update Staff Remark |
| **Announcements** | `/api/announcements` | `GET`, `POST` | Fetch & Post Announcements |
| **Departments** | `/api/departments` | `GET`, `POST` | Fetch & Create Departments |
