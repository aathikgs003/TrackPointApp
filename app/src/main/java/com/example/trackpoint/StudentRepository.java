package com.example.trackpoint;

import android.content.Context;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StudentRepository {

    private final TrackPointRepository cloudRepository;

    public StudentRepository(Context context) {
        this.cloudRepository = TrackPointRepository.getInstance(context.getApplicationContext());
    }

    public TrackPointRepository getCloudRepository() {
        return cloudRepository;
    }

    // --- SUPER ADMIN OPERATIONS ---

    public long addSuperAdmin(SuperAdminModel superAdmin) {
        if (superAdmin == null) return -1;
        return 1;
    }

    public SuperAdminModel authenticateSuperAdmin(String emailOrId, String password) {
        if (emailOrId == null || password == null) return null;
        String lower = emailOrId.toLowerCase().trim();
        if ((lower.equals("superadmin@gmail.com") || lower.equals("admin@gmail.com") || lower.equals("spadm001")) && (password.equals("123") || password.equals("admin"))) {
            return new SuperAdminModel("Super Admin", "SPADM001", emailOrId, "+91 98765 43299", password);
        }
        return null;
    }

    // --- STAFF ADMIN OPERATIONS ---

    public long addStaffAdmin(StaffAdminModel staff) {
        if (staff == null) return -1;
        return 1;
    }

    public StaffAdminModel getStaffAdminByEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        return new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", email, "+91 98765 43200");
    }

    public ArrayList<StaffAdminModel> getAllStaffAdmins() {
        ArrayList<StaffAdminModel> list = new ArrayList<>();
        list.add(new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", "suruthika@gmail.com", "+91 98765 43200"));
        return list;
    }

    public StaffAdminModel authenticateStaffAdmin(String emailOrId, String password) {
        if (emailOrId == null || password == null) return null;
        String lower = emailOrId.toLowerCase().trim();
        if ((lower.equals("suruthika@gmail.com") || lower.equals("stf001")) && (password.equals("123") || password.equals("admin"))) {
            return new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", emailOrId, "+91 98765 43200");
        }
        return null;
    }

    public boolean updateStaffAdmin(StaffAdminModel staff) {
        return staff != null;
    }

    public boolean deleteStaffAdminByStaffId(String staffIdOrEmail) {
        return staffIdOrEmail != null && !staffIdOrEmail.isEmpty();
    }

    // --- STUDENT OPERATIONS ---

    public boolean registerNumberExists(String regNo) {
        return false;
    }

    public long addStudent(StudentModel student) {
        return student != null ? 1 : -1;
    }

    public boolean updateStudent(StudentModel student) {
        return student != null;
    }

    public boolean deleteStudentByRegNo(String regNo) {
        return regNo != null && !regNo.isEmpty();
    }

    public StudentModel getStudentByRegNo(String regNoOrEmail) {
        if (regNoOrEmail == null || regNoOrEmail.isEmpty()) return null;
        return new StudentModel("Arun Kumar", regNoOrEmail, "CSE", "III Year", "arun@gmail.com", "98765 43210");
    }

    public ArrayList<StudentModel> getAllStudents() {
        ArrayList<StudentModel> list = new ArrayList<>();
        list.add(new StudentModel("Arun Kumar", "CSE22CS001", "CSE", "III Year", "arun@gmail.com", "98765 43210"));
        return list;
    }

    public ArrayList<StudentModel> searchAndFilterStudents(String query, String department, String year) {
        return getAllStudents();
    }

    public ArrayList<StudentModel> searchAndFilterStudents(String query, String department, String year, String section) {
        return getAllStudents();
    }

    public ArrayList<StudentModel> searchAndFilterStudentsForAdmin(String query, String department, String year, String adminNameOrId) {
        return getAllStudents();
    }

    public StudentModel authenticateStudent(String emailOrRegNo, String password) {
        if (emailOrRegNo == null || password == null) return null;
        String lower = emailOrRegNo.toLowerCase().trim();
        if ((lower.equals("arun@gmail.com") || lower.equals("cse22cs001")) && (password.equals("123") || password.equals("admin"))) {
            return new StudentModel("Arun Kumar", "CSE22CS001", "CSE", "III Year", emailOrRegNo, "98765 43210");
        }
        return null;
    }

    // --- STUDENT ADMIN OPERATIONS ---

    public boolean adminIdExists(String adminId) {
        return false;
    }

    public long addStudentAdmin(StudentAdminModel studentAdmin) {
        return studentAdmin != null ? 1 : -1;
    }

    public StudentAdminModel getStudentAdminById(String adminIdOrEmail) {
        if (adminIdOrEmail == null || adminIdOrEmail.isEmpty()) return null;
        return new StudentAdminModel("Aathi", "SADM001", "CSE", "III Year", "aathi@gmail.com", "98765 43201");
    }

    public ArrayList<StudentAdminModel> getAllStudentAdmins() {
        ArrayList<StudentAdminModel> list = new ArrayList<>();
        list.add(new StudentAdminModel("Aathi", "SADM001", "CSE", "III Year", "aathi@gmail.com", "98765 43201"));
        return list;
    }

    public ArrayList<StudentAdminModel> getStudentAdminsForStaff(String staffName) {
        return getAllStudentAdmins();
    }

    public int getAssignedStudentsCountForAdmin(String adminName) {
        return 1;
    }

    public boolean updateStudentAdmin(StudentAdminModel studentAdmin) {
        return studentAdmin != null;
    }

    public boolean deleteStudentAdminByAdminId(String adminIdOrEmail) {
        return adminIdOrEmail != null && !adminIdOrEmail.isEmpty();
    }

    public StudentAdminModel authenticateStudentAdmin(String emailOrId, String password) {
        if (emailOrId == null || password == null) return null;
        String lower = emailOrId.toLowerCase().trim();
        if ((lower.equals("aathi@gmail.com") || lower.equals("sadm001")) && (password.equals("123") || password.equals("admin"))) {
            return new StudentAdminModel("Aathi", "SADM001", "CSE", "III Year", emailOrId, "98765 43201");
        }
        return null;
    }

    // --- DEPARTMENTS & METRICS ---

    public ArrayList<String> getAllDepartmentCodes() {
        ArrayList<String> depts = new ArrayList<>();
        depts.add("CSE");
        depts.add("AI & DS");
        depts.add("ECE");
        depts.add("IT");
        depts.add("MECH");
        return depts;
    }

    public int getDepartmentCount() {
        return 5;
    }

    public int getStaffAdminCountForDept(String deptCode) {
        return 1;
    }

    public int getStudentAdminCountForDept(String deptCode) {
        return 1;
    }

    public int getStudentCountForDept(String deptCode) {
        return 1;
    }

    public int getStudentAdminsCountForStaff(String staffName) {
        return 1;
    }

    public ArrayList<StudentAdminModel> getStudentAdminsAssignedToStaff(String staffNameOrId) {
        return getAllStudentAdmins();
    }

    public int getOverallAttendancePercentage() {
        return 88;
    }

    public int[] getOverallAttendanceStats() {
        return new int[]{100, 88, 12};
    }

    public int getTotalDailyActivitiesCount() {
        return 42;
    }

    public int getDailyActivitiesCountForDate(String dateKey) {
        return 15;
    }

    public boolean saveOrUpdateAttendance(String studentRegNo, String dateKey, String status, String markedBy) {
        return true;
    }

    public String getAttendanceStatus(String studentRegNo, String dateKey) {
        return "Present";
    }

    public int[] getAttendanceStatsForStudent(String studentRegNo, String datePrefix) {
        return new int[]{30, 26, 4};
    }

    public int[] getAttendanceStatsForAdmin(String adminName, String datePrefix) {
        return new int[]{120, 105, 15};
    }

    public boolean saveOrUpdateDailyActivity(String studentRegNo, String dateKey, String fileName, String fileUrl, String notes, String submitTime) {
        return true;
    }

    public boolean saveOrUpdateDailyActivity(String studentRegNo, String dateKey, String fileName, String notes, String submitTime) {
        return true;
    }

    public boolean deleteDailyActivity(String studentRegNo, String dateKey) {
        return true;
    }

    public boolean updateStaffRemark(String studentRegNo, String dateKey, String remark) {
        return true;
    }

    public DailyActivityModel getDailyActivity(String studentRegNo, String dateKey) {
        return null;
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
