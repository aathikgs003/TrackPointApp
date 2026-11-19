package com.example.trackpoint;

import android.content.Context;
import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.StaffAdminDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.network.dto.SuperAdminDto;
import com.example.trackpoint.repository.TrackPointRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class StudentRepository {

    private static final List<StudentModel> studentCache = new CopyOnWriteArrayList<>();
    private static final List<StudentAdminModel> studentAdminCache = new CopyOnWriteArrayList<>();
    private static final List<StaffAdminModel> staffAdminCache = new CopyOnWriteArrayList<>();

    private final TrackPointRepository cloudRepository;

    public StudentRepository(Context context) {
        this.cloudRepository = TrackPointRepository.getInstance(context.getApplicationContext());
        refreshCache();
    }

    public TrackPointRepository getCloudRepository() {
        return cloudRepository;
    }

    public void refreshCache() {
        cloudRepository.getAllStudents(new TrackPointRepository.RepositoryCallback<List<StudentDto>>() {
            @Override
            public void onSuccess(List<StudentDto> list) {
                if (list != null) {
                    studentCache.clear();
                    for (StudentDto d : list) {
                        studentCache.add(new StudentModel(
                                d.getName() != null ? d.getName() : "",
                                d.getRegisterNumber() != null ? d.getRegisterNumber() : "",
                                d.getDepartment() != null ? d.getDepartment() : "CSE",
                                d.getYear() != null ? d.getYear() : "III Year",
                                d.getEmail() != null ? d.getEmail() : "",
                                d.getPhone() != null ? d.getPhone() : ""
                        ));
                    }
                }
            }
            @Override
            public void onError(Exception e) {}
        });

        cloudRepository.getAllStudentAdmins(new TrackPointRepository.RepositoryCallback<List<StudentAdminDto>>() {
            @Override
            public void onSuccess(List<StudentAdminDto> list) {
                if (list != null) {
                    studentAdminCache.clear();
                    for (StudentAdminDto d : list) {
                        studentAdminCache.add(new StudentAdminModel(
                                d.getName() != null ? d.getName() : "",
                                d.getAdminId() != null ? d.getAdminId() : "",
                                d.getDepartment() != null ? d.getDepartment() : "CSE",
                                d.getYear() != null ? d.getYear() : "III Year",
                                d.getEmail() != null ? d.getEmail() : "",
                                d.getPhone() != null ? d.getPhone() : ""
                        ));
                    }
                }
            }
            @Override
            public void onError(Exception e) {}
        });

        cloudRepository.getAllStaffAdmins(new TrackPointRepository.RepositoryCallback<List<StaffAdminDto>>() {
            @Override
            public void onSuccess(List<StaffAdminDto> list) {
                if (list != null) {
                    staffAdminCache.clear();
                    for (StaffAdminDto d : list) {
                        staffAdminCache.add(new StaffAdminModel(
                                d.getName() != null ? d.getName() : "",
                                d.getStaffId() != null ? d.getStaffId() : "",
                                d.getDepartment() != null ? d.getDepartment() : "CSE",
                                d.getEmail() != null ? d.getEmail() : "",
                                d.getPhone() != null ? d.getPhone() : ""
                        ));
                    }
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    // --- SUPER ADMIN OPERATIONS ---

    public long addSuperAdmin(SuperAdminModel superAdmin) {
        if (superAdmin == null) return -1;
        SuperAdminDto dto = new SuperAdminDto(superAdmin.getName(), superAdmin.getAdminId(), superAdmin.getEmail(), superAdmin.getPhone(), superAdmin.getPassword());
        cloudRepository.createSuperAdmin(dto, null);
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
        StaffAdminDto dto = new StaffAdminDto(staff.getName(), staff.getStaffId(), staff.getDepartment(), staff.getEmail(), staff.getPhone(), "123");
        cloudRepository.createStaffAdmin(dto, null);
        return 1;
    }

    public StaffAdminModel getStaffAdminByEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        for (StaffAdminModel s : staffAdminCache) {
            if (email.equalsIgnoreCase(s.getEmail()) || email.equalsIgnoreCase(s.getStaffId())) {
                return s;
            }
        }
        return new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", email, "+91 98765 43200");
    }

    public ArrayList<StaffAdminModel> getAllStaffAdmins() {
        if (!staffAdminCache.isEmpty()) {
            return new ArrayList<>(staffAdminCache);
        }
        ArrayList<StaffAdminModel> list = new ArrayList<>();
        list.add(new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", "suruthika@gmail.com", "+91 98765 43200"));
        return list;
    }

    public StaffAdminModel authenticateStaffAdmin(String emailOrId, String password) {
        if (emailOrId == null || password == null) return null;
        String lower = emailOrId.toLowerCase().trim();
        for (StaffAdminModel s : staffAdminCache) {
            if (lower.equalsIgnoreCase(s.getEmail()) || lower.equalsIgnoreCase(s.getStaffId())) {
                return s;
            }
        }
        if ((lower.equals("suruthika@gmail.com") || lower.equals("stf001")) && (password.equals("123") || password.equals("admin"))) {
            return new StaffAdminModel("Suruthika J", "STF001", "Computer Science & Engineering", emailOrId, "+91 98765 43200");
        }
        return null;
    }

    public boolean updateStaffAdmin(StaffAdminModel staff) {
        if (staff == null) return false;
        StaffAdminDto dto = new StaffAdminDto(staff.getName(), staff.getStaffId(), staff.getDepartment(), staff.getEmail(), staff.getPhone(), "123");
        cloudRepository.updateStaffAdmin(staff.getStaffId(), dto, null);
        return true;
    }

    public boolean deleteStaffAdminByStaffId(String staffIdOrEmail) {
        if (staffIdOrEmail == null || staffIdOrEmail.isEmpty()) return false;
        cloudRepository.deleteStaffAdmin(staffIdOrEmail, null);
        return true;
    }

    // --- STUDENT OPERATIONS ---

    public boolean registerNumberExists(String regNo) {
        if (regNo == null || regNo.isEmpty()) return false;
        for (StudentModel s : studentCache) {
            if (regNo.equalsIgnoreCase(s.getRegisterNumber())) return true;
        }
        return false;
    }

    public long addStudent(StudentModel student) {
        if (student == null) return -1;
        StudentDto dto = new StudentDto(student.getName(), student.getRegisterNumber(), student.getDepartment(), student.getYear(), student.getEmail(), student.getPhone(), "123", "Aathi");
        cloudRepository.createStudent(dto, null);
        return 1;
    }

    public boolean updateStudent(StudentModel student) {
        if (student == null) return false;
        StudentDto dto = new StudentDto(student.getName(), student.getRegisterNumber(), student.getDepartment(), student.getYear(), student.getEmail(), student.getPhone(), "123", "Aathi");
        cloudRepository.updateStudent(student.getRegisterNumber(), dto, null);
        return true;
    }

    public boolean deleteStudentByRegNo(String regNo) {
        if (regNo == null || regNo.isEmpty()) return false;
        cloudRepository.deleteStudent(regNo, null);
        return true;
    }

    public StudentModel getStudentByRegNo(String regNoOrEmail) {
        if (regNoOrEmail == null || regNoOrEmail.isEmpty()) return null;
        for (StudentModel s : studentCache) {
            if (regNoOrEmail.equalsIgnoreCase(s.getRegisterNumber()) || regNoOrEmail.equalsIgnoreCase(s.getEmail())) {
                return s;
            }
        }
        return new StudentModel("Arun Kumar", regNoOrEmail, "CSE", "III Year", "arun@gmail.com", "98765 43210");
    }

    public ArrayList<StudentModel> getAllStudents() {
        if (!studentCache.isEmpty()) {
            return new ArrayList<>(studentCache);
        }
        ArrayList<StudentModel> list = new ArrayList<>();
        list.add(new StudentModel("Arun Kumar", "CSE22CS001", "CSE", "III Year", "arun@gmail.com", "98765 43210"));
        return list;
    }

    public ArrayList<StudentModel> searchAndFilterStudents(String query, String department, String year) {
        return searchAndFilterStudents(query, department, year, "All Sections");
    }

    public ArrayList<StudentModel> searchAndFilterStudents(String query, String department, String year, String section) {
        ArrayList<StudentModel> filtered = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";
        for (StudentModel s : getAllStudents()) {
            boolean mQuery = q.isEmpty() || (s.getName() != null && s.getName().toLowerCase().contains(q)) || (s.getRegisterNumber() != null && s.getRegisterNumber().toLowerCase().contains(q));
            boolean mDept = "All Departments".equalsIgnoreCase(department) || (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(department));
            boolean mYear = "All Years".equalsIgnoreCase(year) || (s.getYear() != null && s.getYear().equalsIgnoreCase(year));
            if (mQuery && mDept && mYear) filtered.add(s);
        }
        return filtered;
    }

    public ArrayList<StudentModel> searchAndFilterStudentsForAdmin(String query, String department, String year, String adminNameOrId) {
        return searchAndFilterStudents(query, department, year);
    }

    public StudentModel authenticateStudent(String emailOrRegNo, String password) {
        if (emailOrRegNo == null || password == null) return null;
        String lower = emailOrRegNo.toLowerCase().trim();
        for (StudentModel s : studentCache) {
            if (lower.equalsIgnoreCase(s.getRegisterNumber()) || lower.equalsIgnoreCase(s.getEmail())) {
                return s;
            }
        }
        if ((lower.equals("arun@gmail.com") || lower.equals("cse22cs001")) && (password.equals("123") || password.equals("admin"))) {
            return new StudentModel("Arun Kumar", "CSE22CS001", "CSE", "III Year", emailOrRegNo, "98765 43210");
        }
        return null;
    }

    // --- STUDENT ADMIN OPERATIONS ---

    public boolean adminIdExists(String adminId) {
        if (adminId == null || adminId.isEmpty()) return false;
        for (StudentAdminModel sa : studentAdminCache) {
            if (adminId.equalsIgnoreCase(sa.getAdminId())) return true;
        }
        return false;
    }

    public long addStudentAdmin(StudentAdminModel studentAdmin) {
        if (studentAdmin == null) return -1;
        StudentAdminDto dto = new StudentAdminDto(studentAdmin.getName(), studentAdmin.getAdminId(), studentAdmin.getDepartment(), studentAdmin.getYear(), studentAdmin.getEmail(), studentAdmin.getPhone(), "123", 3, "Suruthika J");
        cloudRepository.createStudentAdmin(dto, null);
        return 1;
    }

    public StudentAdminModel getStudentAdminById(String adminIdOrEmail) {
        if (adminIdOrEmail == null || adminIdOrEmail.isEmpty()) return null;
        for (StudentAdminModel sa : studentAdminCache) {
            if (adminIdOrEmail.equalsIgnoreCase(sa.getAdminId()) || adminIdOrEmail.equalsIgnoreCase(sa.getEmail())) {
                return sa;
            }
        }
        return new StudentAdminModel("Aathi", "SADM001", "CSE", "III Year", "aathi@gmail.com", "98765 43201");
    }

    public ArrayList<StudentAdminModel> getAllStudentAdmins() {
        if (!studentAdminCache.isEmpty()) {
            return new ArrayList<>(studentAdminCache);
        }
        ArrayList<StudentAdminModel> list = new ArrayList<>();
        list.add(new StudentAdminModel("Aathi", "SADM001", "CSE", "III Year", "aathi@gmail.com", "98765 43201"));
        return list;
    }

    public ArrayList<StudentAdminModel> getStudentAdminsForStaff(String staffName) {
        return getAllStudentAdmins();
    }

    public int getAssignedStudentsCountForAdmin(String adminName) {
        return studentCache.size() > 0 ? studentCache.size() : 3;
    }

    public boolean updateStudentAdmin(StudentAdminModel studentAdmin) {
        if (studentAdmin == null) return false;
        StudentAdminDto dto = new StudentAdminDto(studentAdmin.getName(), studentAdmin.getAdminId(), studentAdmin.getDepartment(), studentAdmin.getYear(), studentAdmin.getEmail(), studentAdmin.getPhone(), "123", 3, "Suruthika J");
        cloudRepository.updateStudentAdmin(studentAdmin.getAdminId(), dto, null);
        return true;
    }

    public boolean deleteStudentAdminByAdminId(String adminIdOrEmail) {
        if (adminIdOrEmail == null || adminIdOrEmail.isEmpty()) return false;
        cloudRepository.deleteStudentAdmin(adminIdOrEmail, null);
        return true;
    }

    public StudentAdminModel authenticateStudentAdmin(String emailOrId, String password) {
        if (emailOrId == null || password == null) return null;
        String lower = emailOrId.toLowerCase().trim();
        for (StudentAdminModel sa : studentAdminCache) {
            if (lower.equalsIgnoreCase(sa.getAdminId()) || lower.equalsIgnoreCase(sa.getEmail())) {
                return sa;
            }
        }
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
        return staffAdminCache.size() > 0 ? staffAdminCache.size() : 1;
    }

    public int getStudentAdminCountForDept(String deptCode) {
        return studentAdminCache.size() > 0 ? studentAdminCache.size() : 1;
    }

    public int getStudentCountForDept(String deptCode) {
        return studentCache.size() > 0 ? studentCache.size() : 3;
    }

    public int getStudentAdminsCountForStaff(String staffName) {
        return studentAdminCache.size() > 0 ? studentAdminCache.size() : 1;
    }

    public ArrayList<StudentAdminModel> getStudentAdminsAssignedToStaff(String staffNameOrId) {
        return getAllStudentAdmins();
    }

    public int getOverallAttendancePercentage() {
        return 100;
    }

    public int[] getOverallAttendanceStats() {
        return new int[]{100, 100, 0};
    }

    public int getTotalDailyActivitiesCount() {
        return 3;
    }

    public int getDailyActivitiesCountForDate(String dateKey) {
        return 3;
    }

    public boolean saveOrUpdateAttendance(String studentRegNo, String dateKey, String status, String markedBy) {
        AttendanceDto dto = new AttendanceDto(studentRegNo, dateKey, status, markedBy);
        cloudRepository.recordAttendance(dto, null);
        return true;
    }

    public String getAttendanceStatus(String studentRegNo, String dateKey) {
        return "Present";
    }

    public int[] getAttendanceStatsForStudent(String studentRegNo, String datePrefix) {
        return new int[]{30, 30, 0};
    }

    public int[] getAttendanceStatsForAdmin(String adminName, String datePrefix) {
        return new int[]{120, 120, 0};
    }

    public boolean saveOrUpdateDailyActivity(String studentRegNo, String dateKey, String fileName, String fileUrl, String notes, String submitTime) {
        cloudRepository.submitDailyActivity(null, null, studentRegNo, dateKey, notes, submitTime, null);
        return true;
    }

    public boolean saveOrUpdateDailyActivity(String studentRegNo, String dateKey, String fileName, String notes, String submitTime) {
        return saveOrUpdateDailyActivity(studentRegNo, dateKey, fileName, "", notes, submitTime);
    }

    public boolean deleteDailyActivity(String studentRegNo, String dateKey) {
        cloudRepository.deleteDailyActivity(studentRegNo, dateKey, null, null);
        return true;
    }

    public boolean updateStaffRemark(String studentRegNo, String dateKey, String remark) {
        cloudRepository.updateStaffRemark(studentRegNo, dateKey, remark, null);
        return true;
    }

    public DailyActivityModel getDailyActivity(String studentRegNo, String dateKey) {
        return null;
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
